package com.lossherpa.support;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Enumera los endpoints reales de la aplicacion recorriendo TODOS los HandlerMapping del
 * contexto, no solo los controladores: tambien los recursos estaticos, la welcome page de
 * Spring Boot y el BasicErrorController.
 *
 * Si aparece un HandlerMapping de un tipo que no sabe leer, falla: un tipo nuevo de handler
 * no puede quedar fuera de los tests que recorren los endpoints sin que nadie se entere.
 */
public final class InspectorDeEndpoints {

    /**
     * Un endpoint: metodos HTTP (vacio = cualquier metodo) + patron + el handler.
     * handlerMethod es null para los handlers que no son metodos de controlador.
     */
    public record Endpoint(Set<HttpMethod> metodos, String patron, Object handler,
                           HandlerMethod handlerMethod) {

        public boolean aceptaCualquierMetodo() {
            return metodos.isEmpty();
        }

        public String descripcion() {
            String metodo = metodos.isEmpty() ? "*"
                    : String.join("|", metodos.stream().map(HttpMethod::name).sorted().toList());
            return metodo + " " + patron;
        }
    }

    private InspectorDeEndpoints() {
    }

    public static List<Endpoint> de(ApplicationContext contexto) {
        List<Endpoint> endpoints = new ArrayList<>();
        for (Map.Entry<String, HandlerMapping> entrada
                : contexto.getBeansOfType(HandlerMapping.class).entrySet()) {
            endpoints.addAll(de(entrada.getKey(), entrada.getValue()));
        }
        endpoints.sort(Comparator.comparing(Endpoint::patron)
                .thenComparing(Endpoint::descripcion));
        return endpoints;
    }

    /**
     * Endpoints de controladores sueltos, registrados en un contexto minimo aparte del de la
     * aplicacion. Sirve para demostrar que los tests de cobertura e identidad fallan con un
     * controlador mal hecho sin tener que agregarlo a la app.
     */
    public static List<Endpoint> deControladores(Class<?>... controladores) {
        try (StaticApplicationContext contexto = new StaticApplicationContext()) {
            for (Class<?> controlador : controladores) {
                contexto.registerSingleton(controlador.getSimpleName(), controlador);
            }
            contexto.refresh();
            RequestMappingHandlerMapping mapeo = new RequestMappingHandlerMapping();
            mapeo.setApplicationContext(contexto);
            mapeo.afterPropertiesSet();
            return de("controladores de prueba", mapeo);
        }
    }

    public static List<Endpoint> de(String nombre, HandlerMapping mapeo) {
        if (mapeo instanceof RequestMappingHandlerMapping anotaciones) {
            return deAnotaciones(anotaciones);
        }
        if (mapeo instanceof AbstractUrlHandlerMapping urls) {
            return deUrls(urls);
        }
        if (mapeo instanceof RouterFunctionMapping funciones) {
            if (funciones.getRouterFunction() != null) {
                throw new IllegalStateException("Hay RouterFunctions declaradas en '" + nombre
                        + "': el inspector no las sabe enumerar");
            }
            return List.of();
        }
        throw new IllegalStateException("HandlerMapping desconocido '" + nombre + "' ("
                + mapeo.getClass().getName() + "): agregalo a InspectorDeEndpoints");
    }

    private static List<Endpoint> deAnotaciones(RequestMappingHandlerMapping mapeo) {
        List<Endpoint> endpoints = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entrada
                : mapeo.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = entrada.getKey();
            Set<HttpMethod> metodos = new TreeSet<>(Comparator.comparing(HttpMethod::name));
            info.getMethodsCondition().getMethods()
                    .forEach(metodo -> metodos.add(HttpMethod.valueOf(metodo.name())));
            for (String patron : info.getPatternValues()) {
                endpoints.add(new Endpoint(Set.copyOf(metodos), patron, entrada.getValue(),
                        entrada.getValue()));
            }
        }
        return endpoints;
    }

    /**
     * Recursos estaticos y welcome page. Los handlers de URL solo atienden GET (y el HEAD
     * implicito, que la politica no declara y por eso queda denegado).
     */
    private static List<Endpoint> deUrls(AbstractUrlHandlerMapping mapeo) {
        List<Endpoint> endpoints = new ArrayList<>();
        mapeo.getHandlerMap().forEach((patron, handler) ->
                endpoints.add(new Endpoint(Set.of(HttpMethod.GET), patron, handler, null)));
        if (mapeo.getRootHandler() != null) {
            endpoints.add(new Endpoint(Set.of(HttpMethod.GET), "/", mapeo.getRootHandler(),
                    null));
        }
        return endpoints;
    }
}
