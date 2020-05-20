package com.accantosystems.stratoss.vnfmdriver.service;

import static com.accantosystems.stratoss.vnfmdriver.config.VNFMDriverConstants.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.accantosystems.stratoss.vnfmdriver.config.VNFMDriverProperties;
import com.accantosystems.stratoss.vnfmdriver.driver.VNFMResponseErrorHandler;
import com.accantosystems.stratoss.vnfmdriver.model.AuthenticationType;
import com.accantosystems.stratoss.vnfmdriver.model.alm.ResourceManagerDeploymentLocation;
import com.accantosystems.stratoss.vnfmdriver.security.CookieAuthenticatedRestTemplate;
import com.accantosystems.stratoss.vnfmdriver.security.CookieCredentials;
import com.accantosystems.stratoss.vnfmdriver.utils.DynamicSslCertificateHttpRequestFactory;

@Service("AuthenticatedRestTemplateService")
public class AuthenticatedRestTemplateService {

    private final static Logger logger = LoggerFactory.getLogger(AuthenticatedRestTemplateService.class);

    private final RestTemplateBuilder restTemplateBuilder;
    private final Map<ResourceManagerDeploymentLocation, RestTemplate> cachedRestTemplatesByDLs = new ConcurrentHashMap<>();
    private final Map<String, RestTemplate> cachedRestTemplatesByServerUrl = new ConcurrentHashMap<>();

    @Autowired
    public AuthenticatedRestTemplateService(RestTemplateBuilder restTemplateBuilder, VNFMResponseErrorHandler vnfmResponseErrorHandler, VNFMDriverProperties vnfmDriverProperties) {
        logger.info("Initialising RestTemplate configuration");
        this.restTemplateBuilder = restTemplateBuilder.errorHandler(vnfmResponseErrorHandler)
                .requestFactory(DynamicSslCertificateHttpRequestFactory.class)
                .setConnectTimeout(vnfmDriverProperties.getRestConnectTimeout())
                .setReadTimeout(vnfmDriverProperties.getRestReadTimeout());
    }

    public RestTemplate getRestTemplate(ResourceManagerDeploymentLocation deploymentLocation) {
        if (cachedRestTemplatesByDLs.containsKey(deploymentLocation)) {
            return cachedRestTemplatesByDLs.get(deploymentLocation);
        }

        // Double-check we haven't got a cached entry of the same "name", but different properties. If so, remove it.
        cachedRestTemplatesByDLs.keySet()
                                .stream()
                                .filter(dl -> Objects.equals(dl.getName(), deploymentLocation.getName()))
                                .findFirst()
                                .ifPresent(cachedRestTemplatesByDLs::remove);

        // Check there's a URL defined
        Map<String,String> authenticationProperties = deploymentLocation.getProperties().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (String)e.getValue()));
        checkProperty(authenticationProperties, VNFM_SERVER_URL);

        final RestTemplate restTemplate = getRestTemplate(authenticationProperties);
        cachedRestTemplatesByDLs.put(deploymentLocation, restTemplate);
        return restTemplate;
    }

    public RestTemplate getRestTemplate(String serverUrl, Map<String, String> authenticationProperties) {
        if (cachedRestTemplatesByServerUrl.containsKey(serverUrl)) {
            return cachedRestTemplatesByServerUrl.get(serverUrl);
        }

        final RestTemplate restTemplate = getRestTemplate(authenticationProperties);
        cachedRestTemplatesByServerUrl.put(serverUrl, restTemplate);
        return restTemplate;
    }

    private RestTemplate getRestTemplate(Map<String, String> authenticationProperties) {
        final String authenticationTypeString = authenticationProperties.getOrDefault(AUTHENTICATION_TYPE, AuthenticationType.NONE.toString());
        final AuthenticationType authenticationType = AuthenticationType.valueOfIgnoreCase(authenticationTypeString);
        if (authenticationType == null) {
            throw new IllegalArgumentException(String.format("Invalid authentication type specified [%s]", authenticationTypeString));
        }

        final RestTemplate restTemplate;
        switch (authenticationType) {
        case BASIC:
            checkProperty(authenticationProperties, AUTHENTICATION_USERNAME);
            checkProperty(authenticationProperties, AUTHENTICATION_PASSWORD);
            restTemplate = getBasicAuthenticatedRestTemplate(authenticationProperties);
            break;
        case OAUTH2:
            checkProperty(authenticationProperties, AUTHENTICATION_ACCESS_TOKEN_URI);
            checkProperty(authenticationProperties, AUTHENTICATION_CLIENT_ID);
            checkProperty(authenticationProperties, AUTHENTICATION_CLIENT_SECRET);
            restTemplate = getOAuth2RestTemplate(authenticationProperties);
            break;
        case COOKIE:
            checkProperty(authenticationProperties, AUTHENTICATION_URL);
            checkProperty(authenticationProperties, AUTHENTICATION_USERNAME);
            checkProperty(authenticationProperties, AUTHENTICATION_PASSWORD);
            restTemplate = getCookieAuthenticatedRestTemplate(authenticationProperties);
            break;
        default:
            restTemplate = getUnauthenticatedRestTemplate();
        }

        return restTemplate;
    }

    private void checkProperty(Map<String, String> authenticationProperties, String propertyName) {
        if (StringUtils.isEmpty(authenticationProperties.get(propertyName))) {
            throw new IllegalArgumentException(String.format("Authentication properties must specify a value for [%s]", propertyName));
        }
    }

    private RestTemplate getUnauthenticatedRestTemplate() {
        logger.info("Configuring unauthenticated RestTemplate.");
        return restTemplateBuilder.build();
    }

    private RestTemplate getBasicAuthenticatedRestTemplate(final Map<String, String> authenticationProperties) {
        logger.info("Configuring Basic Authentication RestTemplate.");
        return restTemplateBuilder.basicAuthentication(authenticationProperties.get(AUTHENTICATION_USERNAME),
                                                       authenticationProperties.get(AUTHENTICATION_PASSWORD))
                .build();
    }

    private RestTemplate getOAuth2RestTemplate(final Map<String, String> authenticationProperties) {
        final ClientCredentialsResourceDetails resourceDetails = new ClientCredentialsResourceDetails();
        resourceDetails.setAccessTokenUri(authenticationProperties.get(AUTHENTICATION_ACCESS_TOKEN_URI));
        resourceDetails.setClientId(authenticationProperties.get(AUTHENTICATION_CLIENT_ID));
        resourceDetails.setClientSecret(authenticationProperties.get(AUTHENTICATION_CLIENT_SECRET));
        resourceDetails.setGrantType(authenticationProperties.getOrDefault(AUTHENTICATION_GRANT_TYPE, "client_credentials"));
        if (StringUtils.hasText(authenticationProperties.get(AUTHENTICATION_SCOPE))) {
            resourceDetails.setScope(Arrays.asList(authenticationProperties.get(AUTHENTICATION_SCOPE).split(",")));
        }

        logger.info("Configuring OAuth2 authenticated RestTemplate.");
        return restTemplateBuilder.configure(new OAuth2RestTemplate(resourceDetails));
    }

    private RestTemplate getCookieAuthenticatedRestTemplate(final Map<String, String> authenticationProperties) {
        CookieCredentials cookieCredentials = new CookieCredentials();
        cookieCredentials.setAuthenticationUrl(authenticationProperties.get(AUTHENTICATION_URL));
        cookieCredentials.setUsernameTokenName(authenticationProperties.getOrDefault(AUTHENTICATION_USERNAME_TOKEN_NAME, "IDToken1"));
        cookieCredentials.setPasswordTokenName(authenticationProperties.getOrDefault(AUTHENTICATION_PASSWORD_TOKEN_NAME, "IDToken2"));
        cookieCredentials.setUsername(authenticationProperties.get(AUTHENTICATION_USERNAME));
        cookieCredentials.setPassword(authenticationProperties.get(AUTHENTICATION_PASSWORD));

        logger.info("Configuring Cookie authenticated RestTemplate.");
        return restTemplateBuilder.configure(new CookieAuthenticatedRestTemplate(cookieCredentials));
    }

}
