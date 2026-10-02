package com.lossherpa.config;

import com.lossherpa.security.AuditorAcceso;
import com.lossherpa.security.FiltroSesionVigente;
import com.lossherpa.security.ManejadorAccesoDenegado;
import com.lossherpa.security.PuntoEntradaNoAutenticado;
import com.lossherpa.security.ServicioDetallesUsuario;
import com.lossherpa.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuracion central de seguridad.
 *
 * RS13 - Deny by default: la cadena termina en anyRequest().denyAll(). Todo lo que se sirve
 *        esta enumerado: los tres endpoints publicos, las rutas de API por rol y la lista
 *        blanca de archivos del build del frontend.
 * RS14 - Sin configuracion por defecto: no hay usuario ni password generados por Spring
 *        Security, no hay http basic ni formulario de login propio, CORS restringido por
 *        variable de entorno y cabeceras de seguridad explicitas.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Rutas del SPA que deben devolver index.html. Todo lo demas se deniega (RS13). */
    static final String[] RUTAS_SPA = {
            "/", "/index.html", "/favicon.ico", "/assets/**",
            "/login", "/registro",
            "/entrenador", "/entrenador/**",
            "/atleta", "/atleta/**",
            "/admin", "/admin/**"
    };

    private final String[] origenesCors;
    private final int costoBcrypt;

    public SecurityConfig(@Value("${sherpa.cors.origenes:http://localhost:5173}")
                          String origenesCors,
                          @Value("${sherpa.seguridad.costo-bcrypt:12}") int costoBcrypt) {
        this.origenesCors = origenesCors.split("\\s*,\\s*");
        this.costoBcrypt = costoBcrypt;
    }

    /**
     * RS14: BCrypt con factor de trabajo 12, por encima del default de Spring (10).
     * La politica de contrasenas vive en PoliticaContrasena.
     *
     * El costo es configurable solo para que la suite de tests pueda bajarlo (cada test
     * hashea varias veces y a costo 12 la suite se vuelve inusable). El default sigue
     * siendo 12 y no hay forma de bajarlo sin tocar la configuracion del entorno;
     * SeguridadDeProduccionTest verifica que el perfil por defecto use 12.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(costoBcrypt);
    }

    @Bean
    public AuthenticationManager authenticationManager(ServicioDetallesUsuario detalles,
                                                      PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider();
        proveedor.setUserDetailsService(detalles);
        proveedor.setPasswordEncoder(passwordEncoder);
        // No revela si el email existe: siempre "Credenciales invalidas" (RS14).
        proveedor.setHideUserNotFoundExceptions(true);
        return new ProviderManager(proveedor);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /** Necesario para que el SessionRegistry se entere de las sesiones que se destruyen. */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    /**
     * Regeneracion del id de sesion al loguearse (fijacion de sesion) + registro de la
     * sesion para poder expulsarla despues (RS10, borrado de usuario por el admin).
     */
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(SessionRegistry registro) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new RegisterSessionAuthenticationStrategy(registro)));
    }

    @Bean
    public FiltroSesionVigente filtroSesionVigente(UsuarioRepository usuarios,
                                                   AuditorAcceso auditor) {
        return new FiltroSesionVigente(usuarios, auditor);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // RS14: CORS restringido al origen del dev server de Vite, configurable por entorno.
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origenesCors));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(1800L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", config);
        return fuente;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           FiltroSesionVigente filtroSesionVigente,
                                           PuntoEntradaNoAutenticado puntoEntrada,
                                           ManejadorAccesoDenegado manejadorDenegado,
                                           SessionAuthenticationStrategy estrategiaSesion,
                                           SecurityContextRepository contextRepository)
            throws Exception {

        // CSRF compatible con SPA: cookie XSRF-TOKEN legible por JS, el cliente la reenvia
        // en la cabecera X-XSRF-TOKEN. Se desactiva el envoltorio anti-BREACH porque el SPA
        // nunca renderiza el token en el HTML.
        CsrfTokenRequestAttributeHandler manejadorCsrf = new CsrfTokenRequestAttributeHandler();
        manejadorCsrf.setCsrfRequestAttributeName(null);

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(manejadorCsrf))
            .securityContext(ctx -> ctx.securityContextRepository(contextRepository))
            .sessionManagement(sesion -> sesion
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .sessionAuthenticationStrategy(estrategiaSesion)
                    .sessionFixation(fijacion -> fijacion.changeSessionId()))

            // RS14: sin formulario ni basic auth por default de Spring Security.
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .anonymous(anon -> anon.disable())

            .logout(logout -> logout
                    .logoutUrl("/api/auth/logout")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("SHERPASESSION", "XSRF-TOKEN")
                    .logoutSuccessHandler((req, res, auth) ->
                            res.setStatus(HttpStatus.NO_CONTENT.value())))

            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(puntoEntrada)
                    .accessDeniedHandler(manejadorDenegado))

            // RS14: cabeceras de seguridad explicitas.
            .headers(headers -> headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; "
                            + "script-src 'self'; "
                            + "style-src 'self'; "
                            + "img-src 'self' data:; "
                            + "font-src 'self'; "
                            + "connect-src 'self'; "
                            + "form-action 'self'; "
                            + "frame-ancestors 'none'; "
                            + "base-uri 'self'; "
                            + "object-src 'none'"))
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(ref -> ref.policy(
                            ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                    // Sin TLS en local: HSTS se habilita recien al publicar con https.
                    .httpStrictTransportSecurity(hsts -> hsts.disable())
                    .permissionsPolicy(pp -> pp.policy(
                            "geolocation=(), camera=(), microphone=(), payment=()")))

            .authorizeHttpRequests(auth -> auth
                    // --- Lista blanca de endpoints publicos (RS13) ---
                    .requestMatchers("/api/csrf").permitAll()
                    .requestMatchers("/api/auth/login", "/api/auth/registro").permitAll()

                    // --- API por rol: el rol se valida aca y otra vez en el servicio ---
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/entrenador/**").hasRole("ENTRENADOR")
                    .requestMatchers("/api/atleta/**").hasRole("ATLETA")
                    .requestMatchers("/api/auth/yo", "/api/auth/logout").authenticated()
                    .requestMatchers("/api/perfil").authenticated()

                    // --- Estaticos: solo los archivos del build del frontend (RS13) ---
                    .requestMatchers(RUTAS_SPA).permitAll()

                    // RS13: deny by default. Cualquier otro path (incluidos /actuator,
                    // /h2-console, /swagger-ui o un path de API inexistente) se deniega.
                    .anyRequest().denyAll())

            // RS10: se refresca el principal desde la base ANTES de autorizar.
            .addFilterBefore(filtroSesionVigente, AuthorizationFilter.class);

        return http.build();
    }
}
