package com.lossherpa.service;

import com.lossherpa.error.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** RS15: tests unitarios de la politica de contrasenas (no necesita contexto de Spring). */
class PoliticaContrasenaTest {

    private final PoliticaContrasena politica = new PoliticaContrasena();

    @Test
    @DisplayName("acepta una frase larga sin reglas de composicion")
    void aceptaFraseLarga() {
        assertThatCode(() -> politica.validar("cuatro caballos verdes saltan"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("acepta 12 caracteres exactos, sin exigir mayusculas ni digitos")
    void aceptaLargoMinimoExacto() {
        assertThatCode(() -> politica.validar("abcdefghijkm")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rechaza menos de 12 caracteres")
    void rechazaDemasiadoCorta() {
        assertThatThrownBy(() -> politica.validar("Corta1!aB"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("al menos 12");
    }

    @Test
    @DisplayName("rechaza mas de 128 caracteres")
    void rechazaDemasiadoLarga() {
        assertThatThrownBy(() -> politica.validar("a".repeat(129) + "zq"))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("rechaza una contrasena de la lista de filtradas")
    void rechazaContrasenaComun() {
        assertThatThrownBy(() -> politica.validar("contrasenasegura2026"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("filtradas");
    }

    @Test
    @DisplayName("rechaza una contrasena comun con digitos y simbolos pegados al final")
    void rechazaContrasenaComunConSufijo() {
        assertThatThrownBy(() -> politica.validar("entrenamiento2026!"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("filtradas");
        assertThatThrownBy(() -> politica.validar("contrasena123456"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("filtradas");
    }

    @Test
    @DisplayName("rechaza una palabra comun repetida hasta llegar al largo minimo")
    void rechazaPalabraComunRepetida() {
        assertThatThrownBy(() -> politica.validar("amoramoramoramor"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("filtradas");
    }

    @Test
    @DisplayName("rechaza un unico caracter repetido")
    void rechazaCaracterRepetido() {
        assertThatThrownBy(() -> politica.validar("aaaaaaaaaaaaaaa"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("repetido");
    }

    @Test
    @DisplayName("rechaza la contrasena que contiene el email o el nombre del usuario")
    void rechazaDatosDelUsuario() {
        assertThatThrownBy(() -> politica.validar("ana.gomez.entrena",
                List.of("ana.gomez@mail.com", "Ana", "Gomez")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tu email");
    }

    @Test
    @DisplayName("no exige mayusculas, digitos ni simbolos")
    void sinReglasDeComposicion() {
        assertThatCode(() -> politica.validar("solominusculasyespacios"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("acepta espacios y emoji")
    void aceptaEspaciosYEmoji() {
        assertThatCode(() -> politica.validar("subo peso 100kg 💪"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rechaza null y vacio")
    void rechazaNulaOVacia() {
        assertThatThrownBy(() -> politica.validar(null))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> politica.validar(""))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("normaliza a NFKC para que la comparacion sea estable")
    void normalizaUnicode() {
        String conCombinante = "montaña de hierro";
        String precompuesta = "montaña de hierro";
        assertThat(PoliticaContrasena.normalizar(conCombinante))
                .isEqualTo(PoliticaContrasena.normalizar(precompuesta));
    }

    @Test
    @DisplayName("la descripcion de la politica menciona el largo minimo")
    void describeLaPolitica() {
        assertThat(politica.descripcion()).contains("12");
    }
}
