package com.lossherpa.web.dto.out;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lossherpa.domain.Usuario;
import com.lossherpa.service.CalculadoraEdad;

import java.util.UUID;

/**
 * Perfil que un usuario puede ver de otro.
 *
 * RS07: no incluye email, ni fecha de nacimiento, ni rol, ni hash. La edad va calculada.
 * Hay una fabrica por rol para que cada una exponga solo los campos de ese rol: asi no se
 * puede filtrar el peso de un atleta en el perfil de un entrenador ni al revés.
 *
 * RF7 / RS06: el telefono solo viaja en las fabricas "Vinculado" (vinculo vigente). Ni la
 * busqueda de entrenadores ni las solicitudes pendientes lo incluyen; si es null la clave
 * ni se serializa.
 */
public record PerfilPublicoResponse(
        UUID id,
        String nombre,
        String apellido,
        int edad,
        String descripcion,
        Integer aniosServicio,
        Double pesoKg,
        @JsonInclude(JsonInclude.Include.NON_NULL) String telefono
) {

    /** Lo que un atleta ve de su entrenador. */
    public static PerfilPublicoResponse deEntrenador(Usuario entrenador) {
        return new PerfilPublicoResponse(
                entrenador.getId(),
                entrenador.getNombre(),
                entrenador.getApellido(),
                CalculadoraEdad.edad(entrenador.getFechaNacimiento()),
                entrenador.getDescripcion(),
                entrenador.getAniosServicio(),
                null,
                null);
    }

    /** Lo que un entrenador ve de su atleta activo. */
    public static PerfilPublicoResponse deAtleta(Usuario atleta) {
        return new PerfilPublicoResponse(
                atleta.getId(),
                atleta.getNombre(),
                atleta.getApellido(),
                CalculadoraEdad.edad(atleta.getFechaNacimiento()),
                atleta.getDescripcion(),
                null,
                atleta.getPesoKg(),
                null);
    }

    /** Entrenador con vinculo vigente con el atleta que consulta: incluye el telefono. */
    public static PerfilPublicoResponse deEntrenadorVinculado(Usuario entrenador) {
        PerfilPublicoResponse base = deEntrenador(entrenador);
        return new PerfilPublicoResponse(base.id(), base.nombre(), base.apellido(), base.edad(),
                base.descripcion(), base.aniosServicio(), null, entrenador.getTelefono());
    }

    /** Atleta con vinculo vigente con el entrenador que consulta: incluye el telefono. */
    public static PerfilPublicoResponse deAtletaVinculado(Usuario atleta) {
        PerfilPublicoResponse base = deAtleta(atleta);
        return new PerfilPublicoResponse(base.id(), base.nombre(), base.apellido(), base.edad(),
                base.descripcion(), null, base.pesoKg(), atleta.getTelefono());
    }
}
