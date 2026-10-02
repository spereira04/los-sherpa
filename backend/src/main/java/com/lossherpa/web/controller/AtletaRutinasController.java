package com.lossherpa.web.controller;

import com.lossherpa.service.ServicioEjecuciones;
import com.lossherpa.service.ServicioRutinas;
import com.lossherpa.web.dto.in.CrearRutinaRequest;
import com.lossherpa.web.dto.in.RegistrarEjecucionRequest;
import com.lossherpa.web.dto.out.EjecucionResponse;
import com.lossherpa.web.dto.out.RutinaResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Rutinas y ejecuciones del atleta autenticado. Ningun endpoint recibe el id del atleta. */
@RestController
@RequestMapping("/api/atleta")
public class AtletaRutinasController {

    private final ServicioRutinas servicioRutinas;
    private final ServicioEjecuciones servicioEjecuciones;

    public AtletaRutinasController(ServicioRutinas servicioRutinas,
                                   ServicioEjecuciones servicioEjecuciones) {
        this.servicioRutinas = servicioRutinas;
        this.servicioEjecuciones = servicioEjecuciones;
    }

    /** Asignadas y propias, todas juntas. */
    @GetMapping("/rutinas")
    public List<RutinaResponse> misRutinas() {
        return servicioRutinas.misRutinas().stream().map(RutinaResponse::de).toList();
    }

    @GetMapping("/rutinas/{idRutina}")
    public RutinaResponse verRutina(@PathVariable UUID idRutina) {
        return RutinaResponse.de(servicioRutinas.verMiRutina(idRutina));
    }

    @PostMapping("/rutinas")
    public ResponseEntity<RutinaResponse> crearRutinaPropia(
            @Valid @RequestBody CrearRutinaRequest datos) {
        var creada = servicioRutinas.crearPropia(datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(RutinaResponse.de(creada));
    }

    /** Completar una rutina: se carga la carga en kg de cada serie. */
    @PostMapping("/rutinas/{idRutina}/ejecuciones")
    public ResponseEntity<EjecucionResponse> registrarEjecucion(
            @PathVariable UUID idRutina,
            @Valid @RequestBody RegistrarEjecucionRequest datos) {
        var registrada = servicioEjecuciones.registrar(idRutina, datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(EjecucionResponse.de(registrada));
    }

    @GetMapping("/ejecuciones")
    public List<EjecucionResponse> miHistorial(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate hasta) {
        return servicioEjecuciones.miHistorial(desde, hasta).stream()
                .map(EjecucionResponse::de)
                .toList();
    }
}
