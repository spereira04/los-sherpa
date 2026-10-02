package com.lossherpa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * RS13 - Solo se sirven los archivos del build del frontend.
 *
 * El mapeo automatico de Spring Boot (/** -> classpath:/static/) esta desactivado en
 * application.yml (spring.web.resources.add-mappings = false). Aca se declaran a mano los
 * unicos patrones servibles:
 *
 *  - /assets/**     los bundles que genera Vite
 *  - /favicon.ico
 *
 * El index.html lo entrega ControladorSpa. Ademas, el resolver rechaza los directorios:
 * sin esto, pedir /assets/ devolvia el listado del directorio, que es justo lo que RS13
 * prohibe.
 */
@Configuration
public class ConfiguracionRecursosEstaticos implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        registro.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .resourceChain(true)
                .addResolver(new ResolverSoloArchivos());

        registro.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new ResolverSoloArchivos());
    }

    /** RS13: un directorio nunca es un recurso servible. */
    private static final class ResolverSoloArchivos extends PathResourceResolver {

        @Override
        protected Resource getResource(String rutaDelRecurso, Resource ubicacion)
                throws IOException {
            Resource recurso = super.getResource(rutaDelRecurso, ubicacion);
            if (recurso == null) {
                return null;
            }
            if (rutaDelRecurso.isEmpty() || rutaDelRecurso.endsWith("/")) {
                return null;
            }
            if (recurso.isFile() && recurso.getFile().isDirectory()) {
                return null;
            }
            return recurso;
        }
    }
}
