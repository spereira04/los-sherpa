package com.lossherpa.service;

import com.lossherpa.error.ReglaNegocioException;

import java.time.LocalDate;
import java.time.Period;

/**
 * La edad nunca se persiste: se calcula a partir de la fecha de nacimiento.
 * Se aisla en una clase propia para poder testearla sin levantar Spring.
 */
public final class CalculadoraEdad {

    private static final int EDAD_MINIMA = 13;
    private static final int EDAD_MAXIMA = 120;

    private CalculadoraEdad() {
    }

    public static int edad(LocalDate fechaNacimiento, LocalDate hoy) {
        if (fechaNacimiento == null || hoy == null) {
            throw new IllegalArgumentException("Las fechas no pueden ser nulas");
        }
        if (fechaNacimiento.isAfter(hoy)) {
            throw new ReglaNegocioException("La fecha de nacimiento no puede ser futura");
        }
        return Period.between(fechaNacimiento, hoy).getYears();
    }

    public static int edad(LocalDate fechaNacimiento) {
        return edad(fechaNacimiento, LocalDate.now());
    }

    /** Rango aceptado al registrarse o al editar el perfil. */
    public static void validarFechaNacimiento(LocalDate fechaNacimiento, LocalDate hoy) {
        int edad = edad(fechaNacimiento, hoy);
        if (edad < EDAD_MINIMA) {
            throw new ReglaNegocioException(
                    "Hay que tener al menos " + EDAD_MINIMA + " anos para usar la plataforma");
        }
        if (edad > EDAD_MAXIMA) {
            throw new ReglaNegocioException("La fecha de nacimiento no es valida");
        }
    }

    public static void validarFechaNacimiento(LocalDate fechaNacimiento) {
        validarFechaNacimiento(fechaNacimiento, LocalDate.now());
    }
}
