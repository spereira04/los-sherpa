package com.lossherpa.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.JpaSystemException;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;


import static org.assertj.core.api.Assertions.assertThat;

/** RF3: una violacion de indice unico (carrera) tiene que ser 409 generico, no 500. */
class ManejadorIntegridadTest {

    private final ManejadorGlobalErrores manejador = new ManejadorGlobalErrores(null);

    @Test
    @DisplayName("una violacion de constraint de SQLite envuelta en JpaSystemException da 409 generico")
    void constraintDaConflicto() {
        var causa = new SQLiteException(
                "[SQLITE_CONSTRAINT_UNIQUE] UNIQUE constraint failed: vinculos.atleta_id",
                SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE);
        var ex = new JpaSystemException(new RuntimeException("could not execute statement", causa));

        var respuesta = manejador.sistemaJpa(ex);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().get("error").toString())
                .doesNotContain("vinculos").doesNotContain("SQLITE");
    }

    @Test
    @DisplayName("otro error de SQLite (no constraint) sigue siendo 500")
    void otroErrorSigueSiendo500() {
        var causa = new SQLiteException("[SQLITE_IOERR] disk I/O error", SQLiteErrorCode.SQLITE_IOERR);
        var ex = new JpaSystemException(new RuntimeException("x", causa));

        assertThat(manejador.sistemaJpa(ex).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
