package com.accantosystems.stratoss.vnfmdriver.web.etsi;

import static com.accantosystems.stratoss.vnfmdriver.test.TestConstants.loadFileIntoByteArray;
import static com.accantosystems.stratoss.vnfmdriver.test.TestConstants.loadFileIntoString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.etsi.sol003.common.ProblemDetails;
import org.etsi.sol003.packagemanagement.VnfPkgInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.accantosystems.stratoss.vnfmdriver.driver.VNFPackageNotFoundException;
import com.accantosystems.stratoss.vnfmdriver.service.ContentRangeNotSatisfiableException;
import com.accantosystems.stratoss.vnfmdriver.service.PackageManagementService;
import com.accantosystems.stratoss.vnfmdriver.service.PackageStateConflictException;
import com.accantosystems.stratoss.vnfmdriver.service.UnexpectedPackageContentsException;
import com.accantosystems.stratoss.vnfmdriver.test.TestConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "test" })
public class PackageManagementControllerTest {

    public static final String PACKAGE_MANAGEMENT_BASE_ENDPOINT = "/vnfpkgm/v1/vnf_packages";
    public static final String PACKAGE_MANAGEMENT_VNF_PKG_INFO_ENDPOINT = PACKAGE_MANAGEMENT_BASE_ENDPOINT + "/{vnfPkgId}";
    public static final String PACKAGE_MANAGEMENT_VNFD_ENDPOINT = PACKAGE_MANAGEMENT_BASE_ENDPOINT + "/{vnfPkgId}/vnfd";
    public static final String PACKAGE_MANAGEMENT_PACKAGE_CONTENT_ENDPOINT = PACKAGE_MANAGEMENT_BASE_ENDPOINT + "/{vnfPkgId}/package_content";
    public static final String PACKAGE_MANAGEMENT_PACKAGE_ARTIFACT_ENDPOINT = PACKAGE_MANAGEMENT_BASE_ENDPOINT + "/{vnfPkgId}/artifacts/";

    private static final String VNF_PACKAGE_FILENAME = "examples/VnfPackage-vMRF.zip";

    @MockBean
    private PackageManagementService packageManagementService;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testQueryPackageInfoEmptyList() throws Exception {
        VnfPkgInfo vnfPkgInfo = objectMapper.readValue(loadFileIntoString("examples/vnfPackageId.pkgInfo"), VnfPkgInfo.class);
        when(packageManagementService.getAllVnfPackageInfos(isNull())).thenReturn(Collections.singletonList(vnfPkgInfo));

        final ResponseEntity<List<VnfPkgInfo>> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                                                .exchange(PACKAGE_MANAGEMENT_BASE_ENDPOINT, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()),
                                                                                          new ParameterizedTypeReference<List<VnfPkgInfo>>() {});

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).hasSize(1);
    }

    @Test
    public void testRequestPackageInfo() throws Exception {
        String vnfd = loadFileIntoString("examples/VnfPkgInfo.json");
        VnfPkgInfo vnfPkgInfo = objectMapper.readValue(vnfd, VnfPkgInfo.class);
        String vnfPkgId = TestConstants.TEST_VNF_PKG_ID;
        when(packageManagementService.getVnfPackageInfo(eq(vnfPkgId))).thenReturn(vnfPkgInfo);

        final ResponseEntity<VnfPkgInfo> responseEntity = testRestTemplate.withBasicAuth("user", "password").getForEntity(PACKAGE_MANAGEMENT_VNF_PKG_INFO_ENDPOINT, VnfPkgInfo.class, vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getId()).isNotNull();
        assertThat(responseEntity.getBody().getId()).isEqualTo(vnfPkgId);
    }

    @Test
    public void testRequestPackageInfoByVnfdId() throws Exception {

        String vnfd = loadFileIntoString("examples/VnfPkgInfo.json");
        VnfPkgInfo vnfPkgInfo = objectMapper.readValue(vnfd, VnfPkgInfo.class);
        String vnfPkgId = TestConstants.TEST_VNF_PKG_ID;
        when(packageManagementService.getVnfPackageInfo(eq(vnfPkgId))).thenReturn(vnfPkgInfo);

        final ResponseEntity<List<VnfPkgInfo>> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                                                .exchange(PACKAGE_MANAGEMENT_BASE_ENDPOINT + "?vnfdId={vnfPkgId}", HttpMethod.GET, new HttpEntity<>(new HttpHeaders()),
                                                                                          new ParameterizedTypeReference<List<VnfPkgInfo>>() {}, vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody()).hasSize(1);
        assertThat(responseEntity.getBody().get(0).getId()).isEqualTo(vnfPkgId);
    }

    @Test
    public void testRequestPackageInfoPackageNotFound() throws Exception {

        String vnfPkgId = TestConstants.TEST_VNF_PKG_ID;
        when(packageManagementService.getVnfPackageInfo(eq(vnfPkgId))).thenThrow(new VNFPackageNotFoundException("Unable to find VNF Package"));

        final ResponseEntity<ProblemDetails> responseEntity = testRestTemplate.withBasicAuth("user", "password").getForEntity(PACKAGE_MANAGEMENT_VNF_PKG_INFO_ENDPOINT, ProblemDetails.class, vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getDetail()).isEqualTo("Unable to find VNF Package");
    }

    @Test
    public void testRequestVNFDAsZip() throws Exception {

        byte[] vnfdAsByteArray = loadFileIntoByteArray(VNF_PACKAGE_FILENAME);
        ByteArrayResource vnfdAsResource = new ByteArrayResource(vnfdAsByteArray);

        String vnfPkgId = UUID.randomUUID().toString();

        when(packageManagementService.getVnfdAsZip(eq(vnfPkgId))).thenReturn(vnfdAsResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.parseMediaType("application/zip")));
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        final ResponseEntity<Resource> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                                        .exchange(PACKAGE_MANAGEMENT_VNFD_ENDPOINT, HttpMethod.GET, httpEntity, Resource.class, vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("application/zip"));
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(ByteStreams.toByteArray(responseEntity.getBody().getInputStream())).isEqualTo(vnfdAsByteArray);
    }

    @Test
    public void testRequestVNFDPackageNotFound() throws Exception {

        String vnfPkgId = TestConstants.TEST_VNF_PKG_ID;
        when(packageManagementService.getVnfdAsZip(eq(vnfPkgId))).thenThrow(new VNFPackageNotFoundException("Unable to find VNF Package"));

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.parseMediaType("application/zip"), MediaType.APPLICATION_JSON));
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        final ResponseEntity<ProblemDetails> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                                              .exchange(PACKAGE_MANAGEMENT_VNFD_ENDPOINT, HttpMethod.GET, httpEntity, ProblemDetails.class, vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getDetail()).isEqualTo("Unable to find VNF Package");
    }

    @Test
    public void testRequestVNFDAsYaml() throws Exception {

        String vnfd = loadFileIntoString("examples/Vnfd.yaml");

        String vnfPkgId = UUID.randomUUID().toString();

        when(packageManagementService.getVnfdAsYaml(eq(vnfPkgId))).thenReturn(vnfd);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.TEXT_PLAIN));
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        final ResponseEntity<String> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                                      .exchange(PACKAGE_MANAGEMENT_VNFD_ENDPOINT, HttpMethod.GET, httpEntity, String.class, vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody()).isEqualTo(vnfd);
    }

    @Test
    public void testRequestVNFDAsZipAndYaml() throws Exception {

        byte[] vnfdAsByteArray = loadFileIntoByteArray(VNF_PACKAGE_FILENAME);
        ByteArrayResource vnfdAsResource = new ByteArrayResource(vnfdAsByteArray);

        String vnfPkgId = UUID.randomUUID().toString();

        when(packageManagementService.getVnfdAsZip(eq(vnfPkgId))).thenReturn(vnfdAsResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.parseMediaType("application/zip"), MediaType.TEXT_PLAIN));
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        final ResponseEntity<Resource> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                                        .exchange(PACKAGE_MANAGEMENT_VNFD_ENDPOINT, HttpMethod.GET, httpEntity, Resource.class, vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(ByteStreams.toByteArray(responseEntity.getBody().getInputStream())).isEqualTo(vnfdAsByteArray);
    }

    @Test
    public void testRequestVNFDUnexpectedContentTypes() throws Exception {

        String vnfPkgId = UUID.randomUUID().toString();

        // Check with no Accept types specified
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<?> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                           .exchange(PACKAGE_MANAGEMENT_VNFD_ENDPOINT, HttpMethod.GET, httpEntity, Resource.class, vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);

        // Check with only invalid Accept types specified
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
        httpEntity = new HttpEntity<>(headers);
        responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                         .exchange(PACKAGE_MANAGEMENT_VNFD_ENDPOINT, HttpMethod.GET, httpEntity, Resource.class, vnfPkgId);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);        
        
        // Check with additional invalid Accept types specified
        headers.setAccept(Arrays.asList(MediaType.parseMediaType("application/zip"), MediaType.TEXT_PLAIN, MediaType.APPLICATION_OCTET_STREAM));
        httpEntity = new HttpEntity<>(headers);
        responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                         .exchange(PACKAGE_MANAGEMENT_VNFD_ENDPOINT, HttpMethod.GET, httpEntity, Resource.class, vnfPkgId);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);

        // Check when Accept type of text/plain but multiple vnfds found within the package
        when(packageManagementService.getVnfdAsYaml(eq(vnfPkgId))).thenThrow(new UnexpectedPackageContentsException("Unexpected package contents"));
        headers.setAccept(Arrays.asList(MediaType.TEXT_PLAIN));
        httpEntity = new HttpEntity<>(headers);
        responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                         .exchange(PACKAGE_MANAGEMENT_VNFD_ENDPOINT, HttpMethod.GET, httpEntity, String.class, vnfPkgId);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);        
    }

    @Test
    public void testRequestVNFPackageContent() throws Exception {

        byte[] vnfPackageAsByteArray = loadFileIntoByteArray(VNF_PACKAGE_FILENAME);
        ByteArrayResource vnfPackageAsResource = new ByteArrayResource(vnfPackageAsByteArray);

        String vnfPkgId = UUID.randomUUID().toString();

        when(packageManagementService.getVnfPackageContent(eq(vnfPkgId), nullable(String.class))).thenReturn(vnfPackageAsResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.parseMediaType("application/zip")));
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        final ResponseEntity<Resource> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                                        .exchange(PACKAGE_MANAGEMENT_PACKAGE_CONTENT_ENDPOINT, HttpMethod.GET, httpEntity, Resource.class, vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("application/zip"));
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(ByteStreams.toByteArray(responseEntity.getBody().getInputStream())).isEqualTo(vnfPackageAsByteArray);
    }

    @Test
    public void testRequestVNFPackagePartialContent() throws Exception {

        byte[] vnfPackageAsByteArray = loadFileIntoByteArray(VNF_PACKAGE_FILENAME);
        ByteArrayResource vnfPackageAsResource = new ByteArrayResource(vnfPackageAsByteArray);

        String vnfPkgId = UUID.randomUUID().toString();
        String contentRange = "1000-2000";

        when(packageManagementService.getVnfPackageContent(eq(vnfPkgId), eq(contentRange))).thenReturn(vnfPackageAsResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.parseMediaType("application/zip")));
        headers.set(HttpHeaders.CONTENT_RANGE, contentRange);
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        final ResponseEntity<Resource> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                                        .exchange(PACKAGE_MANAGEMENT_PACKAGE_CONTENT_ENDPOINT, HttpMethod.GET, httpEntity, Resource.class, vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(responseEntity.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("application/zip"));
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(ByteStreams.toByteArray(responseEntity.getBody().getInputStream())).isEqualTo(vnfPackageAsByteArray);
    }

    @Test
    public void testRequestVNFPackageContentPackageNotFound() throws Exception {

        String vnfPkgId = TestConstants.TEST_VNF_PKG_ID;
        when(packageManagementService.getVnfPackageContent(eq(vnfPkgId), nullable(String.class))).thenThrow(new VNFPackageNotFoundException("Unable to find VNF Package"));

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.parseMediaType("application/zip"), MediaType.APPLICATION_JSON));
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        final ResponseEntity<ProblemDetails> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                                              .exchange(PACKAGE_MANAGEMENT_PACKAGE_CONTENT_ENDPOINT, HttpMethod.GET, httpEntity, ProblemDetails.class, vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getDetail()).isEqualTo("Unable to find VNF Package");
    }

    @Test
    public void testRequestVNFPackageContentPackageConflict() throws Exception {

        String vnfPkgId = TestConstants.TEST_VNF_PKG_ID;
        when(packageManagementService.getVnfPackageContent(eq(vnfPkgId), nullable(String.class))).thenThrow(new PackageStateConflictException("Invalid state"));

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.parseMediaType("application/zip"), MediaType.APPLICATION_JSON));
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        final ResponseEntity<ProblemDetails> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                                              .exchange(PACKAGE_MANAGEMENT_PACKAGE_CONTENT_ENDPOINT, HttpMethod.GET, httpEntity, ProblemDetails.class, vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getDetail()).isEqualTo("Invalid state");
    }

    @Test
    public void testRequestVNFPackageContentRangeNotSatisfiable() throws Exception {

        String vnfPkgId = TestConstants.TEST_VNF_PKG_ID;
        when(packageManagementService.getVnfPackageContent(eq(vnfPkgId), nullable(String.class))).thenThrow(new ContentRangeNotSatisfiableException("Invalid state"));

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.parseMediaType("application/zip"), MediaType.APPLICATION_JSON));
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        final ResponseEntity<ProblemDetails> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                                              .exchange(PACKAGE_MANAGEMENT_PACKAGE_CONTENT_ENDPOINT, HttpMethod.GET, httpEntity, ProblemDetails.class, vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getDetail()).isEqualTo("Invalid state");
    }

    @Test
    public void testRequestVNFPackageArtifact() throws Exception {

        String vnfPackageArtifact = loadFileIntoString("examples/Vnfd.yaml");
        ByteArrayResource vnfPackageArtifactAsResource = new ByteArrayResource(vnfPackageArtifact.getBytes());

        String vnfPkgId = UUID.randomUUID().toString();
        String artifactPath = "Definitions/Vnfd.yaml";

        when(packageManagementService.getVnfPackageArtifact(eq(vnfPkgId), eq(artifactPath), nullable(String.class))).thenReturn(vnfPackageArtifactAsResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON));
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        // don't allow rest template to expand the artifactPath variable into the uri or it will end up encoded and the uri rejected
        final ResponseEntity<Resource> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                                        .exchange(PACKAGE_MANAGEMENT_PACKAGE_ARTIFACT_ENDPOINT + artifactPath, HttpMethod.GET, httpEntity, Resource.class, vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(ByteStreams.toByteArray(responseEntity.getBody().getInputStream())).isEqualTo(vnfPackageArtifact.getBytes());
    }

    @Test
    public void testRequestVNFPackageArtifactPartialContent() throws Exception {

        String vnfPackageArtifact = loadFileIntoString("examples/Vnfd.yaml");
        ByteArrayResource vnfPackageArtifactAsResource = new ByteArrayResource(vnfPackageArtifact.getBytes());

        String vnfPkgId = UUID.randomUUID().toString();
        String artifactPath = "Definitions/Vnfd.yaml";
        String contentRange = "1000-2000";

        when(packageManagementService.getVnfPackageArtifact(eq(vnfPkgId), eq(artifactPath), eq(contentRange))).thenReturn(vnfPackageArtifactAsResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.CONTENT_RANGE, contentRange);
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        // don't allow rest template to expand the artifactPath variable into the uri or it will end up encoded and the uri rejected
        final ResponseEntity<Resource> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                                        .exchange(PACKAGE_MANAGEMENT_PACKAGE_ARTIFACT_ENDPOINT + artifactPath, HttpMethod.GET, httpEntity, Resource.class, vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(responseEntity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(ByteStreams.toByteArray(responseEntity.getBody().getInputStream())).isEqualTo(vnfPackageArtifact.getBytes());
    }

    @Test
    public void testRequestVNFPackageArtifactPackageNotFound() throws Exception {

        String vnfPkgId = TestConstants.TEST_VNF_PKG_ID;
        String artifactPath = "Definitions/Vnfd.yaml";
        when(packageManagementService.getVnfPackageArtifact(eq(vnfPkgId), eq(artifactPath), nullable(String.class))).thenThrow(new VNFPackageNotFoundException("Unable to find VNF Package"));

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON));
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        // don't allow rest template to expand the artifactPath variable into the uri or it will end up encoded and the uri rejected
        final ResponseEntity<ProblemDetails> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                                              .exchange(PACKAGE_MANAGEMENT_PACKAGE_ARTIFACT_ENDPOINT + artifactPath, HttpMethod.GET, httpEntity, ProblemDetails.class,
                                                                                        vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getDetail()).isEqualTo("Unable to find VNF Package");
    }

    @Test
    public void testRequestVNFPackageArtifactPackageConflict() throws Exception {

        String vnfPkgId = TestConstants.TEST_VNF_PKG_ID;
        String artifactPath = "Definitions/Vnfd.yaml";
        when(packageManagementService.getVnfPackageArtifact(eq(vnfPkgId), eq(artifactPath), nullable(String.class))).thenThrow(new PackageStateConflictException("Invalid state"));

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON));
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        // don't allow rest template to expand the artifactPath variable into the uri or it will end up encoded and the uri rejected
        final ResponseEntity<ProblemDetails> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                                              .exchange(PACKAGE_MANAGEMENT_PACKAGE_ARTIFACT_ENDPOINT + artifactPath, HttpMethod.GET, httpEntity, ProblemDetails.class,
                                                                                        vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getDetail()).isEqualTo("Invalid state");
    }

    @Test
    public void testRequestVNFPackageArtifactRangeNotSatisfiable() throws Exception {

        String vnfPkgId = TestConstants.TEST_VNF_PKG_ID;
        String artifactPath = "Definitions/Vnfd.yaml";
        when(packageManagementService.getVnfPackageArtifact(eq(vnfPkgId), eq(artifactPath), nullable(String.class))).thenThrow(new ContentRangeNotSatisfiableException("Invalid state"));

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON));
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        // don't allow rest template to expand the artifactPath variable into the uri or it will end up encoded and the uri rejected
        final ResponseEntity<ProblemDetails> responseEntity = testRestTemplate.withBasicAuth("user", "password")
                                                                              .exchange(PACKAGE_MANAGEMENT_PACKAGE_ARTIFACT_ENDPOINT + artifactPath, HttpMethod.GET, httpEntity, ProblemDetails.class,
                                                                                        vnfPkgId);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getDetail()).isEqualTo("Invalid state");
    }

}
