package com.fersacare.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * FERSA Care não emite seus próprios tokens: o Supabase Auth faz login e
 * emite o JWT; aqui a API só VALIDA esse token (API Gateway / Resource
 * Server, no vocabulário OAuth2). Dois pontos importantes:
 *
 * 1) jwk-set-uri aponta para a chave PÚBLICA do projeto Supabase
 *    (https://&lt;ref&gt;.supabase.co/auth/v1/jwks) — a partir de
 *    outubro/2025 todo projeto novo do Supabase já assina os tokens
 *    de forma assimétrica, então validar localmente contra o JWKS
 *    funciona sem precisarmos chamar o Supabase a cada request.
 * 2) O issuer é validado explicitamente (createDefaultWithIssuer),
 *    para rejeitar um JWT válido mas emitido por outro projeto Supabase.
 *
 * Rotas: /api/manuais, /api/categorias e /api/chat são públicas (área
 * sem login do sistema de manuais); /api/admin/** exige perfil ADMIN — o
 * "login de admin" é o mesmo login do Supabase, com profiles.perfil = ADMIN.
 * O resto continua exigindo qualquer usuário autenticado.
 *
 * O CORS (quem pode chamar a API a partir do navegador) está em
 * config.CorsConfig — o Spring Security detecta aquele bean sozinho.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize nos controllers/services
public class SecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder(
        @Value("${fersa.supabase.jwks-url}") String jwksUrl,
        @Value("${fersa.supabase.issuer}") String issuer
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUrl).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        SupabaseJwtAuthenticationConverter authenticationConverter
    ) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API stateless com token, sem cookies de sessão — CSRF não se aplica
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                // Área pública do sistema de manuais: busca, PDF da fonte e chat
                .requestMatchers(HttpMethod.GET, "/api/manuais", "/api/manuais/*/pdf", "/api/categorias").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/chat").permitAll()
                // CMS de manuais/categorias: só ADMIN (os controllers repetem com @PreAuthorize)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter))
            );
        return http.build();
    }
}
