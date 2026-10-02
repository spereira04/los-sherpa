package com.lossherpa.web.dto.in;

import com.lossherpa.domain.Rol;

/**
 * Roles que el registro publico puede elegir.
 *
 * ADMIN no existe en este enum a proposito: no es que se valide y se rechace, es que no se
 * puede ni deserializar. Un body con "rol":"ADMIN" falla al parsear y devuelve 400 antes de
 * llegar al servicio. Es la forma mas fuerte de cumplir "el registro nunca crea un admin".
 */
public enum RolRegistro {
    ENTRENADOR,
    ATLETA;

    public Rol aRolDeDominio() {
        return this == ENTRENADOR ? Rol.ENTRENADOR : Rol.ATLETA;
    }
}
