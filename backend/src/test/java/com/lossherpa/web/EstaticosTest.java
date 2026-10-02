package com.lossherpa.web;

import com.lossherpa.support.TestIntegracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RS13 (deny by default, incluidos los estaticos) y RS14 (cabeceras y sin endpoints de debug).
 *
 * Los archivos de prueba viven en src/test/resources/static: index.html, assets/app.js y
 * privado/secreto.txt. El ultimo existe DENTRO del directorio servido pero en un path que no
 * esta en la lista blanca, asi que no tiene que servirse.
 */
class EstaticosTest extends TestIntegracion {

    @Test
    @DisplayName("la raiz sirve el index.html del build")
    void laRaizSirveElIndex() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("spa-de-prueba")));
    }

    @Test
    @DisplayName("los assets del build se sirven")
    void losAssetsSeSirven() throws Exception {
        mvc.perform(get("/assets/app.js")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("las rutas del SPA devuelven el index para que el router las resuelva")
    void lasRutasDelSpaDevuelvenElIndex() throws Exception {
        for (String ruta : new String[] {
                "/login", "/registro", "/atleta/rutinas", "/entrenador/atletas", "/admin/usuarios"
        }) {
            mvc.perform(get(ruta))
                    .andExpect(status().isOk())
                    .andExpect(content().string(
                            org.hamcrest.Matchers.containsString("spa-de-prueba")));
        }
    }

    @Test
    @DisplayName("RS13: un archivo que existe en un path no permitido NO se sirve")
    void unPathNoPermitidoNoSeSirve() throws Exception {
        String respuesta = mvc.perform(get("/privado/secreto.txt"))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        assertThat(respuesta).doesNotContain("ESTE-ARCHIVO-NO-SE-DEBE-SERVIR");
    }

    @Test
    @DisplayName("RS13: los archivos de configuracion del classpath no se sirven")
    void laConfiguracionNoSeSirve() throws Exception {
        for (String ruta : new String[] {
                "/application.yml", "/application-dev.yml", "/logback-spring.xml",
                "/seguridad/contrasenas-comunes.txt", "/.env", "/BOOT-INF/classes/application.yml"
        }) {
            mvc.perform(get(ruta))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("RS14: actuator, consolas y endpoints de debug estan denegados")
    void losEndpointsDeDebugEstanDenegados() throws Exception {
        for (String ruta : new String[] {
                "/actuator", "/actuator/health", "/actuator/env", "/h2-console",
                "/swagger-ui.html", "/v3/api-docs", "/error"
        }) {
            mvc.perform(get(ruta)).andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("RS13: un endpoint de API inexistente se deniega, incluso con sesion")
    void unEndpointDeApiInexistenteSeDeniega() throws Exception {
        crearAtleta("ana@test.com", "Ana", "Suarez");
        MockHttpSession sesion = iniciarSesion("ana@test.com");

        mvc.perform(get("/api/no-existe")).andExpect(status().isForbidden());
        mvc.perform(get("/api/no-existe").session(sesion)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RS13: no hay listado de directorios, y el status es 404 (no 500)")
    void noHayListadoDeDirectorios() throws Exception {
        // Sin el resolver que rechaza directorios, esto devolvia el listado con "app.js".
        // Y sin el handler de NoResourceFoundException devolvia 500 en lugar de 404.
        String assets = mvc.perform(get("/assets/"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        assertThat(assets).doesNotContain("app.js");

        String raizDeStatic = mvc.perform(get("/privado/"))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();
        assertThat(raizDeStatic).doesNotContain("secreto.txt");
    }

    @Test
    @DisplayName("RS14: un estatico inexistente da 404, nunca un error interno")
    void unEstaticoInexistenteNoDa500() throws Exception {
        for (String ruta : new String[] {
                "/assets/", "/assets/no-existe.js", "/favicon.ico"
        }) {
            int estado = mvc.perform(get(ruta)).andReturn().getResponse().getStatus();
            assertThat(estado)
                    .as("estado de %s", ruta)
                    .isLessThan(500);
        }
    }

    @Test
    @DisplayName("RS14: las cabeceras de seguridad estan presentes")
    void lasCabecerasDeSeguridadEstanPresentes() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "same-origin"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("default-src 'self'"),
                                org.hamcrest.Matchers.containsString("frame-ancestors 'none'"),
                                org.hamcrest.Matchers.containsString("object-src 'none'"))))
                .andExpect(header().string("Permissions-Policy",
                        org.hamcrest.Matchers.containsString("geolocation=()")));
    }

    @Test
    @DisplayName("RS14: la respuesta de un path denegado no expone nada interno")
    void elErrorDeUnPathDenegadoEsGenerico() throws Exception {
        String respuesta = mvc.perform(get("/actuator/health"))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        assertThat(respuesta)
                .doesNotContain("Exception")
                .doesNotContain("org.springframework")
                .doesNotContain("com.lossherpa")
                .doesNotContain("Tomcat");
    }
}
