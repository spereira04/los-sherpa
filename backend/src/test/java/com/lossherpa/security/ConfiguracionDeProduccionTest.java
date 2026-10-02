package com.lossherpa.security;

import com.lossherpa.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RS14: guardas sobre la configuracion del perfil por defecto.
 *
 * La suite de tests baja el costo de BCrypt a 4 por velocidad. Estos tests existen para que
 * ese atajo no se filtre a produccion ni pase desapercibido si alguien cambia el default.
 */
class ConfiguracionDeProduccionTest {

    @Test
    @DisplayName("el costo de BCrypt por defecto es 12")
    void elCostoPorDefectoEsDoce() {
        Parameter parametro = Arrays.stream(SecurityConfig.class.getConstructors()[0]
                        .getParameters())
                .filter(p -> p.isAnnotationPresent(Value.class))
                .filter(p -> p.getAnnotation(Value.class).value().contains("costo-bcrypt"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No se encontro el parametro del costo de BCrypt"));

        assertThat(parametro.getAnnotation(Value.class).value())
                .as("si se baja el default, produccion queda con hashes debiles")
                .isEqualTo("${sherpa.seguridad.costo-bcrypt:12}");
    }

    @Test
    @DisplayName("application.yml no baja el costo de BCrypt")
    void elYmlDeProduccionNoBajaElCosto() throws IOException {
        assertThat(leer("application.yml")).doesNotContain("costo-bcrypt");
    }

    /**
     * RS14: ninguna clave sensible del perfil por defecto puede tener un valor literal.
     * Referenciar una variable de entorno (${VAR}) si esta permitido; lo que no se admite es
     * una contrasena, un secreto o un token escrito en el archivo.
     */
    @Test
    @DisplayName("RS14: el perfil por defecto no define ninguna credencial literal")
    void elPerfilPorDefectoNoTraeCredenciales() throws IOException {
        Pattern claveSensibleConValor = Pattern.compile(
                "(?i)^\\s*[a-z0-9_.-]*(password|passwd|secret|token|api[-_]?key|credential)"
                        + "[a-z0-9_.-]*\\s*:\\s*(?!\\$\\{)\\S+");

        List<String> sospechosas = leer("application.yml").lines()
                .filter(linea -> !linea.trim().startsWith("#"))
                .filter(linea -> claveSensibleConValor.matcher(linea).find())
                .toList();

        assertThat(sospechosas)
                .as("claves sensibles con valor literal en application.yml")
                .isEmpty();
    }

    @Test
    @DisplayName("RS14: el perfil por defecto no menciona las credenciales de ejemplo del seed")
    void elPerfilPorDefectoNoUsaElSeed() throws IOException {
        assertThat(leer("application.yml")).doesNotContain("SEED_PASSWORD");
    }

    @Test
    @DisplayName("RS14: el seed esta apagado en el perfil por defecto")
    void elSeedEstaApagadoPorDefecto() throws IOException {
        assertThat(leer("application.yml")).contains("habilitado: false");
    }

    private String leer(String recurso) throws IOException {
        return new String(new ClassPathResource(recurso).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
    }
}
