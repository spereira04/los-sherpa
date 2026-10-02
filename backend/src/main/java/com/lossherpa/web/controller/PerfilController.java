package com.lossherpa.web.controller;

import com.lossherpa.service.ServicioPerfil;
import com.lossherpa.web.dto.in.ActualizarPerfilRequest;
import com.lossherpa.web.dto.out.PerfilResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Perfil propio, para cualquiera de los tres roles. */
@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final ServicioPerfil servicioPerfil;

    public PerfilController(ServicioPerfil servicioPerfil) {
        this.servicioPerfil = servicioPerfil;
    }

    @GetMapping
    public PerfilResponse miPerfil() {
        return PerfilResponse.de(servicioPerfil.miPerfil());
    }

    /** RS04: actualiza siempre el perfil del usuario autenticado, no recibe ningun id. */
    @PutMapping
    public PerfilResponse actualizar(@Valid @RequestBody ActualizarPerfilRequest datos) {
        return PerfilResponse.de(servicioPerfil.actualizar(datos));
    }
}
