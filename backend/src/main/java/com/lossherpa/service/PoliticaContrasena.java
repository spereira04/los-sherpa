package com.lossherpa.service;

import com.lossherpa.error.ReglaNegocioException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Politica de contrasenas alineada con ASVS v5 (capitulo de autenticacion).
 *
 * Reglas:
 *  - Largo minimo 12 caracteres y maximo 128 (el maximo evita DoS al hashear).
 *  - Sin reglas de composicion arbitrarias: no se exigen mayusculas, digitos ni simbolos.
 *  - Se acepta cualquier caracter Unicode, incluidos espacios y emoji. Se normaliza a NFKC
 *    para que la misma frase tecleada distinto siga validando.
 *  - Se rechazan las contrasenas de una lista de ~10.000 filtradas conocidas, comparando
 *    tambien la raiz de la contrasena (sin digitos ni simbolos en los extremos) y la
 *    repeticion de una misma palabra.
 *  - Se rechazan las contrasenas que contienen datos del propio usuario (email, nombre,
 *    apellido) y las formadas por un solo caracter repetido.
 */
@Component
public class PoliticaContrasena {

    public static final int LARGO_MINIMO = 12;
    public static final int LARGO_MAXIMO = 128;

    private static final String ARCHIVO_COMUNES = "seguridad/contrasenas-comunes.txt";
    private static final int LARGO_MINIMO_FRAGMENTO_USUARIO = 4;

    private final Set<String> comunes;

    public PoliticaContrasena() {
        this.comunes = cargarComunes();
    }

    /**
     * Valida la contrasena. Lanza ReglaNegocioException con un mensaje en espanol apto para
     * mostrar al usuario.
     *
     * @param datosDelUsuario email, nombre y apellido, para impedir que la contrasena los
     *                        contenga. Puede venir vacio.
     */
    public void validar(String contrasena, Collection<String> datosDelUsuario) {
        if (contrasena == null || contrasena.isEmpty()) {
            throw new ReglaNegocioException("La contrasena es obligatoria");
        }

        String normalizada = normalizar(contrasena);

        if (normalizada.length() < LARGO_MINIMO) {
            throw new ReglaNegocioException(
                    "La contrasena debe tener al menos " + LARGO_MINIMO + " caracteres");
        }
        if (normalizada.length() > LARGO_MAXIMO) {
            throw new ReglaNegocioException(
                    "La contrasena no puede superar los " + LARGO_MAXIMO + " caracteres");
        }

        String enMinusculas = normalizada.toLowerCase(Locale.ROOT);

        if (unSoloCaracterRepetido(normalizada)) {
            throw new ReglaNegocioException(
                    "La contrasena no puede ser un mismo caracter repetido");
        }
        if (esComun(enMinusculas)) {
            throw new ReglaNegocioException(
                    "Esa contrasena aparece en listas de contrasenas filtradas. Elegi otra");
        }
        if (datosDelUsuario != null) {
            for (String dato : datosDelUsuario) {
                if (contieneDatoDelUsuario(enMinusculas, dato)) {
                    throw new ReglaNegocioException(
                            "La contrasena no puede contener tu email, nombre ni apellido");
                }
            }
        }
    }

    public void validar(String contrasena) {
        validar(contrasena, List.of());
    }

    /** ASVS: normalizacion Unicode para que la comparacion sea estable. */
    public static String normalizar(String contrasena) {
        return Normalizer.normalize(contrasena, Normalizer.Form.NFKC);
    }

    /** Descripcion de la politica, para mostrarla en el formulario de registro. */
    public String descripcion() {
        return "Minimo " + LARGO_MINIMO + " caracteres, maximo " + LARGO_MAXIMO
                + ". Se aceptan espacios y cualquier simbolo. No se admiten contrasenas"
                + " muy usadas ni que contengan tu email, nombre o apellido.";
    }

    /**
     * Una contrasena de 12+ caracteres casi nunca aparece tal cual en una lista de filtradas,
     * asi que comparar solo el valor exacto no sirve de nada. Antes de buscar se reduce la
     * contrasena a su raiz: se quitan los digitos y simbolos de los extremos y se colapsa la
     * repeticion de una misma palabra. Asi "entrenamiento2026!" y "holaholahola" quedan
     * rechazadas porque su raiz ("entrenamiento", "hola") si esta en la lista.
     */
    private boolean esComun(String enMinusculas) {
        if (comunes.contains(enMinusculas)) {
            return true;
        }
        String raiz = enMinusculas
                .replaceAll("^[^\\p{L}]+", "")
                .replaceAll("[^\\p{L}]+$", "");
        if (raiz.length() >= LARGO_MINIMO_FRAGMENTO_USUARIO && comunes.contains(raiz)) {
            return true;
        }
        return esPalabraRepetida(raiz);
    }

    /** "holaholahola" -> "hola". Detecta la misma palabra comun repetida 2 a 6 veces. */
    private boolean esPalabraRepetida(String raiz) {
        for (int veces = 2; veces <= 6; veces++) {
            if (raiz.length() % veces != 0) {
                continue;
            }
            int largoBase = raiz.length() / veces;
            if (largoBase < LARGO_MINIMO_FRAGMENTO_USUARIO) {
                continue;
            }
            String base = raiz.substring(0, largoBase);
            if (base.repeat(veces).equals(raiz) && comunes.contains(base)) {
                return true;
            }
        }
        return false;
    }

    private boolean unSoloCaracterRepetido(String contrasena) {
        return contrasena.chars().distinct().count() == 1;
    }

    private boolean contieneDatoDelUsuario(String contrasenaEnMinusculas, String dato) {
        if (dato == null || dato.isBlank()) {
            return false;
        }
        String limpio = dato.toLowerCase(Locale.ROOT).trim();
        // Del email alcanza con la parte local: "ana.gomez" de "ana.gomez@mail.com".
        int arroba = limpio.indexOf('@');
        if (arroba > 0) {
            limpio = limpio.substring(0, arroba);
        }
        if (limpio.length() < LARGO_MINIMO_FRAGMENTO_USUARIO) {
            return false;
        }
        return contrasenaEnMinusculas.contains(limpio);
    }

    private Set<String> cargarComunes() {
        Set<String> conjunto = new HashSet<>(12_000);
        ClassPathResource recurso = new ClassPathResource(ARCHIVO_COMUNES);
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(recurso.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                String limpia = linea.trim().toLowerCase(Locale.ROOT);
                if (!limpia.isEmpty()) {
                    conjunto.add(limpia);
                }
            }
        } catch (IOException e) {
            // Sin la lista no se puede garantizar la politica: mejor fallar al arrancar.
            throw new UncheckedIOException(
                    "No se pudo cargar la lista de contrasenas comunes", e);
        }
        return Set.copyOf(conjunto);
    }
}
