package com.lossherpa.security;

import com.lossherpa.support.CapturadorDeAuditoria;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RS18 - Dispatch a /error con un contenedor de servlets real (Tomcat embebido).
 *
 * MockMvc no hace el forward de error del contenedor, asi que una excepcion que escapa de
 * todo handler solo se puede observar levantando el servidor. Un filtro de prueba lanza una
 * excepcion con un mensaje "sensible" para /api/boom: la excepcion no la ve el
 * RestControllerAdvice, sube hasta el contenedor y vuelve como dispatch ERROR hacia /error.
 *
 * Resultado documentado en SECURITY.md: /error esta en la politica como DENEGADO (RS13), asi
 * que la respuesta es el 403 generico y el cliente nunca ve la causa.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FallaCerradaRealTest {

    private static final String SECRETO = "SELECT * FROM usuario WHERE email='ana@test.com'";
    private static final String GENERICO_403 =
            "{\"error\":\"No tiene permisos para realizar esta accion\"}";

    @TestConfiguration
    static class FiltroQueExplota {

        @Bean
        FilterRegistrationBean<Filter> filtroQueExplota() {
            FilterRegistrationBean<Filter> registro = new FilterRegistrationBean<>(
                    (request, response, chain) -> {
                        if (((HttpServletRequest) request).getRequestURI().startsWith("/api/boom")) {
                            throw new IllegalStateException(SECRETO);
                        }
                        chain.doFilter(request, response);
                    });
            // Antes de la cadena de Spring Security: nada de lo que viene despues lo ataja.
            registro.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
            return registro;
        }
    }

    @LocalServerPort
    private int puerto;

    private HttpResponse<String> pedir(String path, String accept) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + path))
                        .header("Accept", accept)
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("RS18: una excepcion que escapa de todo handler termina en el 403 generico, sin stack trace ni HTML")
    void laExcepcionNoManejadaDaUnaRespuestaGenerica() throws Exception {
        for (String accept : new String[]{"application/json", "text/html", "*/*"}) {
            HttpResponse<String> respuesta = pedir("/api/boom", accept);

            assertThat(respuesta.statusCode()).as("Accept: " + accept).isEqualTo(403);
            assertThat(respuesta.headers().firstValue("Content-Type").orElse(""))
                    .startsWith("application/json");
            assertThat(respuesta.body()).isEqualTo(GENERICO_403);
        }
    }

    @Test
    @DisplayName("RS18: la causa de la excepcion no sale por HTTP, solo queda del lado del servidor")
    void laCausaNoSale() throws Exception {
        HttpResponse<String> respuesta = pedir("/api/boom", "text/html");

        assertThat(respuesta.body())
                .doesNotContain("IllegalStateException")
                .doesNotContain("SELECT")
                .doesNotContain("at com.lossherpa")
                .doesNotContain("<html")
                .doesNotContain("Tomcat");
        assertThat(respuesta.headers().map()).doesNotContainKey("Server");
    }

    @Test
    @DisplayName("RS18: el error queda auditado como error interno y con el recurso que fallo, no como /error")
    void elErrorNoManejadoSeAudita() throws Exception {
        try (CapturadorDeAuditoria auditoria = new CapturadorDeAuditoria()) {
            pedir("/api/boom", "application/json");

            assertThat(auditoria.ultimaLinea())
                    .contains("\"evento\":\"acceso_denegado\"")
                    .contains("\"estado\":403")
                    .contains("\"recurso\":\"/api/boom\"")
                    .contains("error interno no manejado (HTTP 500)")
                    .doesNotContain("SELECT")
                    .doesNotContain("ana@test.com");
        }
    }

    @Test
    @DisplayName("RS18/RS13: pedir /error directamente no devuelve nada propio: 403 generico")
    void errorDirectoEstaDenegado() throws Exception {
        HttpResponse<String> respuesta = pedir("/error", "application/json");

        assertThat(respuesta.statusCode()).isEqualTo(403);
        assertThat(respuesta.body()).isEqualTo(GENERICO_403);
    }
}
