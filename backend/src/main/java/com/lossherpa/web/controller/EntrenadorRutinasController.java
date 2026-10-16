package com.lossherpa.web.controller;

import com.lossherpa.error.ReglaNegocioException;
import com.lossherpa.service.ServicioEjecuciones;
import com.lossherpa.service.ServicioRutinas;
import com.lossherpa.web.dto.in.ActualizarRutinaRequest;
import com.lossherpa.web.dto.in.CrearRutinaRequest;
import com.lossherpa.web.dto.out.EjecucionResponse;
import com.lossherpa.web.dto.out.RutinaResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Rutinas y ejecuciones de un atleta, desde el lado del entrenador.
 *
 * Todo exige vinculo ACTIVO y se limita a lo que este entrenador asigno. Las rutinas
 * propias del atleta y sus ejecuciones no aparecen por ningun camino.
 */
@RestController
@RequestMapping("/api/entrenador/atletas/{idAtleta}")
public class EntrenadorRutinasController {

    private final ServicioRutinas servicioRutinas;
    private final ServicioEjecuciones servicioEjecuciones;

    public EntrenadorRutinasController(ServicioRutinas servicioRutinas,
                                       ServicioEjecuciones servicioEjecuciones) {
        this.servicioRutinas = servicioRutinas;
        this.servicioEjecuciones = servicioEjecuciones;
    }

    @GetMapping("/rutinas")
    public List<RutinaResponse> rutinasQueAsigne(@PathVariable UUID idAtleta) {
        return servicioRutinas.rutinasQueAsigneA(idAtleta).stream()
                .map(RutinaResponse::de)
                .toList();
    }

    @PostMapping("/rutinas")
    public ResponseEntity<RutinaResponse> crearRutina(@PathVariable UUID idAtleta,
                                                      @Valid @RequestBody
                                                      CrearRutinaRequest datos) {
        var creada = servicioRutinas.crearParaAtleta(idAtleta, datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(RutinaResponse.de(creada));
    }

    @PutMapping("/rutinas/{idRutina}")
    public RutinaResponse actualizarRutina(@PathVariable UUID idAtleta,
                                          @PathVariable UUID idRutina,
                                          @Valid @RequestBody ActualizarRutinaRequest datos) {
        if (datos.tieneCamposNoPermitidos()) {
            throw new ReglaNegocioException(
                    "Los campos de autorizacion no son editables en la rutina");
        }
        return RutinaResponse.de(servicioRutinas.actualizarAsignada(idAtleta, idRutina, datos));
    }

    @DeleteMapping("/rutinas/{idRutina}")
    public ResponseEntity<Void> eliminarRutina(@PathVariable UUID idAtleta,
                                              @PathVariable UUID idRutina) {
        servicioRutinas.eliminarAsignada(idAtleta, idRutina);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ejecuciones")
    public List<EjecucionResponse> historial(
            @PathVariable UUID idAtleta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta) {
        return servicioEjecuciones.historialDeAtleta(idAtleta, desde, hasta).stream()
                .map(EjecucionResponse::de)
                .toList();
    }
}
