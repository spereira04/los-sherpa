package com.lossherpa.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * RS13 - Deny by default en los estaticos.
 *
 * Solo las rutas conocidas del SPA devuelven index.html, y se devuelve leyendo el archivo
 * del classpath en lugar de hacer forward: asi no depende del manejador de recursos y no
 * hay ninguna ruta que pueda resolver a algo distinto del index.
 *
 * No hay catch-all /**: cualquier otro path cae en anyRequest().denyAll() de SecurityConfig.
 */
@Controller
public class ControladorSpa {

    private static final Resource INDEX = new ClassPathResource("static/index.html");

    @GetMapping({
            "/",
            "/index.html",
            "/login",
            "/registro",
            "/entrenador", "/entrenador/**",
            "/atleta", "/atleta/**",
            "/admin", "/admin/**"
    })
    public ResponseEntity<Resource> spa() {
        if (!INDEX.exists()) {
            // Sin build del frontend (por ejemplo corriendo solo el backend en dev).
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                // El index no se cachea para que un deploy nuevo no quede pegado.
                .cacheControl(CacheControl.noStore())
                .body(INDEX);
    }
}
