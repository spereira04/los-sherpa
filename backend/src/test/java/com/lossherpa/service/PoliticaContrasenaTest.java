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
        assertThatCode(() -> politica.validar("cuatro caballos verdes saltan".toCharArray()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("acepta 12 caracteres exactos, sin exigir mayusculas ni digitos")
    void aceptaLargoMinimoExacto() {
        assertThatCode(() -> politica.validar("abcdefghijkm".toCharArray()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rechaza menos de 12 caracteres")
    void rechazaDemasiadoCorta() {
        assertThatThrownBy(() -> politica.validar("Corta1!aB".toCharArray()))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("al menos 12");
    }

    @Test
    @DisplayName("rechaza mas de 72 bytes, el limite de BCrypt")
    void rechazaDemasiadoLarga() {
        String larga = "caminata larga por la montana nevada al amanecer con amigos y un termo"
                + " de cafe caliente";
        assertThat(larga).hasSize(87);
        assertThatThrownBy(() -> politica.validar(larga.toCharArray()))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("72 bytes");
    }

    @Test
    @DisplayName("acepta exactamente 72 bytes")
    void aceptaElLimiteExacto() {
        String justa = "caminata larga por la montana nevada al amanecer con amigos y un termo!!";
        assertThat(justa).hasSize(72);
        assertThatCode(() -> politica.validar(justa.toCharArray())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("cuenta bytes y no caracteres: con tildes, enie y emoji el limite es menor")
    void cuentaBytesNoCaracteres() {
        // 68 caracteres pero 77 bytes: cada tilde y la enie ocupan 2 y los simbolos 3.
        String conAcentos = "caminata larga por la montaña nevada al amanecer con amigos ⛰️ ✨ é á";
        assertThat(conAcentos.length()).isLessThan(72);
        assertThat(conAcentos.getBytes(java.nio.charset.StandardCharsets.UTF_8).length)
                .isGreaterThan(72);
        assertThatThrownBy(() -> politica.validar(conAcentos.toCharArray()))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("72 bytes");

        String conEmoji = "subimos el cerro al amanecer " + "🏔".repeat(11);
        assertThat(conEmoji.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(73);
        assertThat(PoliticaContrasena.superaLargoMaximo(conEmoji.toCharArray())).isTrue();
        String conEmojiJusto = "subimos el cerro al amanecer " + "🏔".repeat(10) + "!!!";
        assertThat(conEmojiJusto.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(72);
        assertThat(PoliticaContrasena.superaLargoMaximo(conEmojiJusto.toCharArray())).isFalse();
    }

    @Test
    @DisplayName("rechaza una contrasena de la lista de filtradas")
    void rechazaContrasenaComun() {
        assertThatThrownBy(() -> politica.validar("contrasenasegura2026".toCharArray()))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("filtradas");
    }

    @Test
    @DisplayName("rechaza una contrasena comun con digitos y simbolos pegados al final")
    void rechazaContrasenaComunConSufijo() {
        assertThatThrownBy(() -> politica.validar("entrenamiento2026!".toCharArray()))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("filtradas");
        assertThatThrownBy(() -> politica.validar("contrasena123456".toCharArray()))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("filtradas");
    }

    @Test
    @DisplayName("rechaza una palabra comun repetida hasta llegar al largo minimo")
    void rechazaPalabraComunRepetida() {
        assertThatThrownBy(() -> politica.validar("amoramoramoramor".toCharArray()))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("filtradas");
    }

    @Test
    @DisplayName("rechaza un unico caracter repetido")
    void rechazaCaracterRepetido() {
        assertThatThrownBy(() -> politica.validar("aaaaaaaaaaaaaaa".toCharArray()))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("repetido");
    }

    @Test
    @DisplayName("rechaza la contrasena que contiene el email o el nombre del usuario")
    void rechazaDatosDelUsuario() {
        assertThatThrownBy(() -> politica.validar("ana.gomez.entrena".toCharArray(),
                List.of("ana.gomez@mail.com", "Ana", "Gomez")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tu email");
    }

    @Test
    @DisplayName("no exige mayusculas, digitos ni simbolos")
    void sinReglasDeComposicion() {
        assertThatCode(() -> politica.validar("solominusculasyespacios".toCharArray()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("acepta espacios y emoji")
    void aceptaEspaciosYEmoji() {
        assertThatCode(() -> politica.validar("subo peso 100kg 💪".toCharArray()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rechaza null y vacio")
    void rechazaNulaOVacia() {
        assertThatThrownBy(() -> politica.validar((char[]) null))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> politica.validar(new char[0]))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("normaliza a NFKC para que la comparacion sea estable")
    void normalizaUnicode() {
        char[] conCombinante = "montaña de hierro".toCharArray();
        char[] precompuesta = "montaña de hierro".toCharArray();
        assertThat(PoliticaContrasena.normalizar(conCombinante))
                .isEqualTo(PoliticaContrasena.normalizar(precompuesta));
    }

    @Test
    @DisplayName("la descripcion de la politica menciona el largo minimo")
    void describeLaPolitica() {
        assertThat(politica.descripcion()).contains("12");
    }
}
