package com.licenta.v1.ecommercebackend.api.security;

import com.licenta.v1.ecommercebackend.api.security.jwt.AuthEntryPointJwt;
import com.licenta.v1.ecommercebackend.api.security.jwt.AuthTokenFilter;
import com.licenta.v1.ecommercebackend.model.LocalUser;
import com.licenta.v1.ecommercebackend.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true,
)
public class WebSecurityConfig implements WebMvcConfigurer {

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*");
    }

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(withDefaults())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        // Public endpoints
                        auth.requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/api/test/**").permitAll()
                                .requestMatchers("/api/books/view").permitAll()
                                .requestMatchers("/api/books/{id}").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/books/sort/title").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/books/sort/price").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/books/sort/pages").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/books/search/title").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/books/search/author").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/books/search/genre").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/books/search/publisher").permitAll()

                                // Private endpoints
                                .requestMatchers("/api/books/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/orders/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("USER")
                                .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasRole("USER")
                                .requestMatchers(HttpMethod.GET, "/api/orders/**").hasRole("USER")
                                .requestMatchers(HttpMethod.GET, "/api/orders/user").hasRole("USER")
                                .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
