package com.lossherpa.web.controller;

import com.lossherpa.service.ServicioVinculacion;
import com.lossherpa.web.dto.out.AtletaActivoResponse;
import com.lossherpa.web.dto.out.ExatletaResponse;
import com.lossherpa.web.dto.out.PerfilPublicoResponse;
import com.lossherpa.web.dto.out.SolicitudRecibidaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Atletas, solicitudes e historial desde el lado del entrenador.
 * El rol lo exige la cadena de filtros (/api/entrenador/** -> hasRole ENTRENADOR) y de nuevo
 * el servicio, que ademas exige un vinculo ACTIVO para todo lo que sea de un atleta (RS04).
 */
@RestController
@RequestMapping("/api/entrenador")
public class EntrenadorAtletasController {

    private final ServicioVinculacion servicioVinculacion;

    public EntrenadorAtletasController(ServicioVinculacion servicioVinculacion) {
        this.servicioVinculacion = servicioVinculacion;
    }

    @GetMapping("/atletas")
    public List<AtletaActivoResponse> misAtletas() {
        return servicioVinculacion.misAtletasActivos().stream()
                .map(AtletaActivoResponse::de)
                .toList();
    }

    /** Historial: solo nombre, apellido y fechas. Sin id ni ningun otro dato (RS07). */
    @GetMapping("/exatletas")
    public List<ExatletaResponse> misExatletas() {
        return servicioVinculacion.misExatletas().stream()
                .map(ExatletaResponse::de)
                .toList();
    }

    @GetMapping("/solicitudes")
    public List<SolicitudRecibidaResponse> solicitudesPendientes() {
        return servicioVinculacion.solicitudesPendientes().stream()
                .map(SolicitudRecibidaResponse::de)
                .toList();
    }

    @PostMapping("/solicitudes/{idSolicitud}/aceptar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void aceptar(@PathVariable UUID idSolicitud) {
        servicioVinculacion.aceptarSolicitud(idSolicitud);
    }

    @PostMapping("/solicitudes/{idSolicitud}/rechazar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rechazar(@PathVariable UUID idSolicitud) {
        servicioVinculacion.rechazarSolicitud(idSolicitud);
    }

    /** RS04: exige vinculo activo. Un atleta ajeno o un exatleta dan 404. */
    @GetMapping("/atletas/{idAtleta}")
    public PerfilPublicoResponse verAtleta(@PathVariable UUID idAtleta) {
        return PerfilPublicoResponse.deAtleta(servicioVinculacion.verAtletaActivo(idAtleta));
    }

    @DeleteMapping("/atletas/{idAtleta}/vinculo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desvincular(@PathVariable UUID idAtleta) {
        servicioVinculacion.desvincularComoEntrenador(idAtleta);
    }
}
