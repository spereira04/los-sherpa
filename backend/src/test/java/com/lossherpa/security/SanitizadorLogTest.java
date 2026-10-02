package com.lossherpa.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** RS17: el saneado de datos de usuario antes de escribirlos en el log. */
class SanitizadorLogTest {

    @Test
    @DisplayName("elimina saltos de linea para impedir la inyeccion de entradas falsas")
    void eliminaSaltosDeLinea() {
        String malicioso = "atacante\n{\"evento\":\"login_ok\",\"usuarioId\":\"admin\"}";
        String limpio = SanitizadorLog.limpiar(malicioso);
        assertThat(limpio).doesNotContain("\n").doesNotContain("\r");
    }

    @Test
    @DisplayName("escapa las comillas para no romper el JSON del evento")
    void escapaComillas() {
        assertThat(SanitizadorLog.limpiar("dice \"hola\"")).isEqualTo("dice \\\"hola\\\"");
    }

    @Test
    @DisplayName("elimina caracteres de control y secuencias ANSI")
    void eliminaCaracteresDeControl() {
        String limpio = SanitizadorLog.limpiar("rojo\u001b[31mtexto\u0000fin");
        assertThat(limpio).doesNotContain("\u001b").doesNotContain("\u0000");
    }

    @Test
    @DisplayName("trunca los valores muy largos")
    void truncaValoresLargos() {
        String limpio = SanitizadorLog.limpiar("a".repeat(5000));
        assertThat(limpio).hasSizeLessThan(300).endsWith("[truncado]");
    }

    @Test
    @DisplayName("reemplaza null y vacio por un guion")
    void reemplazaNuloPorGuion() {
        assertThat(SanitizadorLog.limpiar(null)).isEqualTo("-");
        assertThat(SanitizadorLog.limpiar("   ")).isEqualTo("-");
    }
}
