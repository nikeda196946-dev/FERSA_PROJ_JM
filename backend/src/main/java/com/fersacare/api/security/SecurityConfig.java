package com.fersacare.api.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

/**
 * FERSA Care não emite seus próprios tokens: o Supabase Auth faz login e
 * emite o JWT; aqui a API só VALIDA esse token (API Gateway / Resource
 * Server, no vocabulário OAuth2). Dois pontos importantes:
 *
 * 1) Projetos com assinatura assimétrica validam localmente pelo JWKS
 *    público. Projetos que ainda usam o segredo legado HS256 validam com
 *    SUPABASE_JWT_SECRET, mantido somente no ambiente do backend.
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
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public JwtDecoder jwtDecoder(
        @Value("${fersa.supabase.jwks-url}") String jwksUrl,
        @Value("${fersa.supabase.issuer}") String issuer,
        @Value("${fersa.supabase.jwt-secret:}") String jwtSecret
    ) {
        NimbusJwtDecoder jwksDecoder = NimbusJwtDecoder.withJwkSetUri(jwksUrl)
            .jwsAlgorithm(SignatureAlgorithm.ES256)
            .build();
        jwksDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));

        if (jwtSecret == null || jwtSecret.isBlank()) {
            return jwksDecoder;
        }

        var key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder hs256Decoder = NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
        hs256Decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));

        return token -> {
            try {
                if (JWSAlgorithm.HS256.equals(SignedJWT.parse(token).getHeader().getAlgorithm())) {
                    return hs256Decoder.decode(token);
                }
                return jwksDecoder.decode(token);
            } catch (ParseException e) {
                throw new JwtException("Malformed Supabase JWT.", e);
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        SupabaseJwtAuthenticationConverter authenticationConverter
    ) throws Exception {
        AuthenticationEntryPoint jwtFailureLogger = (request, response, exception) -> {
            String detail = exception.getCause() != null
                ? exception.getCause().getMessage()
                : exception.getMessage();
            String algoritmo = "indisponível";
            String keyId = "sem kid";
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                try {
                    var header = SignedJWT.parse(authorization.substring(7)).getHeader();
                    algoritmo = header.getAlgorithm().getName();
                    keyId = header.getKeyID() == null ? "sem kid" : header.getKeyID();
                } catch (ParseException ignored) {
                    // Nunca registrar o token; só metadados públicos do cabeçalho.
                }
            }
            log.warn("Autenticação negada em {} (alg={}, kid={}): {}", request.getRequestURI(), algoritmo, keyId, detail);
            response.sendError(401);
        };

        http
            .csrf(csrf -> csrf.disable()) // API stateless com token, sem cookies de sessão — CSRF não se aplica
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/config").permitAll()
                // Área pública do sistema de manuais: busca, PDF da fonte e chat
                .requestMatchers(HttpMethod.GET, "/api/manuais", "/api/manuais/*/pdf", "/api/categorias").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/chat").permitAll()
                // CMS de manuais/categorias: só ADMIN (os controllers repetem com @PreAuthorize)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint(jwtFailureLogger)
                .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter))
            );
        return http.build();
    }
}
