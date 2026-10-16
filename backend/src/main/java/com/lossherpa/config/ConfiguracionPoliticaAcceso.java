package com.lossherpa.config;

import com.lossherpa.security.InterceptorPoliticaAcceso;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * RS01 - Registra el segundo punto de aplicacion de la politica sobre todos los handlers de
 * MVC: controladores, recursos estaticos y el controlador de errores de Spring Boot.
 */
@Configuration
public class ConfiguracionPoliticaAcceso implements WebMvcConfigurer {

    private final InterceptorPoliticaAcceso interceptor;

    public ConfiguracionPoliticaAcceso(InterceptorPoliticaAcceso interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(interceptor);
    }
}
