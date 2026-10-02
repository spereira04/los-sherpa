package com.lossherpa.service;

import com.lossherpa.error.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** RS15: tests unitarios del calculo de edad (la edad nunca se persiste). */
class CalculadoraEdadTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    @Test
    @DisplayName("calcula los anos cumplidos")
    void calculaAnosCumplidos() {
        assertThat(CalculadoraEdad.edad(LocalDate.of(1996, 5, 20), HOY)).isEqualTo(30);
    }

    @Test
    @DisplayName("el dia del cumpleanos ya cuenta el ano nuevo")
    void elDiaDelCumpleanosCuenta() {
        assertThat(CalculadoraEdad.edad(LocalDate.of(2000, 10, 1), HOY)).isEqualTo(26);
    }

    @Test
    @DisplayName("un dia antes del cumpleanos todavia no cuenta")
    void unDiaAntesNoCuenta() {
        assertThat(CalculadoraEdad.edad(LocalDate.of(2000, 10, 2), HOY)).isEqualTo(25);
    }

    @Test
    @DisplayName("maneja el 29 de febrero de un ano bisiesto")
    void manejaAnoBisiesto() {
        assertThat(CalculadoraEdad.edad(LocalDate.of(2004, 2, 29), LocalDate.of(2026, 2, 28)))
                .isEqualTo(21);
        assertThat(CalculadoraEdad.edad(LocalDate.of(2004, 2, 29), LocalDate.of(2026, 3, 1)))
                .isEqualTo(22);
    }

    @Test
    @DisplayName("rechaza una fecha de nacimiento futura")
    void rechazaFechaFutura() {
        assertThatThrownBy(() -> CalculadoraEdad.edad(LocalDate.of(2027, 1, 1), HOY))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("futura");
    }

    @Test
    @DisplayName("rechaza menores de 13 al validar el registro")
    void rechazaMenorDeEdadMinima() {
        assertThatThrownBy(() -> CalculadoraEdad.validarFechaNacimiento(
                LocalDate.of(2020, 1, 1), HOY))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("al menos 13");
    }

    @Test
    @DisplayName("rechaza una edad imposible")
    void rechazaEdadImposible() {
        assertThatThrownBy(() -> CalculadoraEdad.validarFechaNacimiento(
                LocalDate.of(1850, 1, 1), HOY))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("acepta una fecha dentro del rango valido")
    void aceptaFechaValida() {
        CalculadoraEdad.validarFechaNacimiento(LocalDate.of(1990, 6, 15), HOY);
        assertThat(CalculadoraEdad.edad(LocalDate.of(1990, 6, 15), HOY)).isEqualTo(36);
    }
}
