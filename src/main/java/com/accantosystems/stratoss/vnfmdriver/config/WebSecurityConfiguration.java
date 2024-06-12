package com.accantosystems.stratoss.vnfmdriver.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration("WebSecurityConfiguration")
@EnableWebSecurity
public class WebSecurityConfiguration {

    private final VNFMDriverProperties vnfmDriverProperties;

    @Autowired
    public WebSecurityConfiguration(VNFMDriverProperties vnfmDriverProperties) {
        this.vnfmDriverProperties = vnfmDriverProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disabling CSRF protection
            .authorizeHttpRequests(authz -> authz
                    .requestMatchers("/vnflcm/**").hasRole("USER")
                    .requestMatchers("/grant/**").hasRole("USER")
                    .requestMatchers("/vnfpkgm/v1/**").hasRole("USER")
                    .requestMatchers("/management/**").hasRole("USER")
                    .anyRequest().denyAll() // Denying all other requests
            )
            .httpBasic(Customizer.withDefaults()); // Using HTTP Basic authentication

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/api/**", "/management/health", "/management/info", "/vnfpkgm/v2/**");
    }

    @Bean
    public UserDetailsService userDetailsService() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withDefaultPasswordEncoder().username("user").password("password").roles("USER").build());
        manager.createUser(User.withDefaultPasswordEncoder().username("user_with_no_roles").password("password").roles("NONE").build());
        manager.createUser(User.withDefaultPasswordEncoder().username("locked_user").password("password").roles("USER").accountLocked(true).build());
        return manager;
    }

}
