package com.lossherpa.web.controller;

import com.lossherpa.service.ServicioRutinas;
import com.lossherpa.web.dto.in.CrearRutinaRequest;
import com.lossherpa.web.dto.out.RutinaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Rutinas de un atleta, desde el lado del entrenador.
 *
 * Todo exige vinculo ACTIVO y se limita a lo que este entrenador asigno. Las rutinas
 * propias del atleta no aparecen por ningun camino.
 */
@RestController
@RequestMapping("/api/entrenador/atletas/{idAtleta}")
public class EntrenadorRutinasController {

    private final ServicioRutinas servicioRutinas;

    public EntrenadorRutinasController(ServicioRutinas servicioRutinas) {
        this.servicioRutinas = servicioRutinas;
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

}
