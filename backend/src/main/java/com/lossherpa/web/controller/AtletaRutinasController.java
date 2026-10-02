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

/** Rutinas del atleta autenticado. Ningun endpoint recibe el id del atleta. */
@RestController
@RequestMapping("/api/atleta")
public class AtletaRutinasController {

    private final ServicioRutinas servicioRutinas;

    public AtletaRutinasController(ServicioRutinas servicioRutinas) {
        this.servicioRutinas = servicioRutinas;
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

}
