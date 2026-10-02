package com.lossherpa.web.controller;

import com.lossherpa.service.ServicioVinculacion;
import com.lossherpa.web.dto.in.CrearSolicitudRequest;
import com.lossherpa.web.dto.out.MiEntrenadorResponse;
import com.lossherpa.web.dto.out.PerfilPublicoResponse;
import com.lossherpa.web.dto.out.SolicitudEnviadaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Vinculacion desde el lado del atleta.
 * El rol lo exige la cadena de filtros (/api/atleta/** -> hasRole ATLETA) y de nuevo el
 * servicio. Ningun endpoint recibe el id del atleta: sale del usuario autenticado (RS04).
 */
@RestController
@RequestMapping("/api/atleta")
public class AtletaVinculacionController {

    private final ServicioVinculacion servicioVinculacion;

    public AtletaVinculacionController(ServicioVinculacion servicioVinculacion) {
        this.servicioVinculacion = servicioVinculacion;
    }

    /** Listado de entrenadores con busqueda por nombre o apellido. */
    @GetMapping("/entrenadores")
    public List<PerfilPublicoResponse> buscarEntrenadores(
            @RequestParam(required = false) String nombre) {
        return servicioVinculacion.buscarEntrenadores(nombre).stream()
                .map(PerfilPublicoResponse::deEntrenador)
                .toList();
    }

    @GetMapping("/solicitudes")
    public List<SolicitudEnviadaResponse> misSolicitudes() {
        return servicioVinculacion.misSolicitudes().stream()
                .map(SolicitudEnviadaResponse::de)
                .toList();
    }

    @PostMapping("/solicitudes")
    public ResponseEntity<SolicitudEnviadaResponse> enviarSolicitud(
            @Valid @RequestBody CrearSolicitudRequest datos) {
        var creada = servicioVinculacion.enviarSolicitud(datos.idEntrenador());
        return ResponseEntity.status(HttpStatus.CREATED).body(SolicitudEnviadaResponse.de(creada));
    }

    @PostMapping("/solicitudes/{idSolicitud}/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarSolicitud(@PathVariable UUID idSolicitud) {
        servicioVinculacion.cancelarSolicitud(idSolicitud);
    }

    /** Perfil publico del entrenador activo. 404 si el atleta no tiene entrenador. */
    @GetMapping("/entrenador")
    public MiEntrenadorResponse miEntrenador() {
        return MiEntrenadorResponse.de(servicioVinculacion.miVinculoComoAtleta());
    }

    @DeleteMapping("/vinculo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desvincular() {
        servicioVinculacion.desvincularComoAtleta();
    }
}
