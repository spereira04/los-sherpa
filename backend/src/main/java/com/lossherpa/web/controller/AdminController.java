package com.lossherpa.web.controller;

import com.lossherpa.domain.Rol;
import com.lossherpa.service.ServicioAdmin;
import com.lossherpa.web.dto.in.ActualizarPerfilRequest;
import com.lossherpa.web.dto.in.CrearUsuarioAdminRequest;
import com.lossherpa.web.dto.out.UsuarioAdminResponse;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Gestion de usuarios. El rol lo exige la cadena de filtros (/api/admin/** -> hasRole ADMIN)
 * y de nuevo el repositorio, que excluye a los ADMIN de todo lo gestionable.
 */
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminController {

    private final ServicioAdmin servicioAdmin;

    public AdminController(ServicioAdmin servicioAdmin) {
        this.servicioAdmin = servicioAdmin;
    }

    @GetMapping
    public List<UsuarioAdminResponse> listar(@RequestParam(required = false) Rol rol,
                                             @RequestParam(required = false) String q) {
        return servicioAdmin.listar(rol, q).stream().map(UsuarioAdminResponse::de).toList();
    }

    @GetMapping("/{idUsuario}")
    public UsuarioAdminResponse ver(@PathVariable UUID idUsuario) {
        return UsuarioAdminResponse.de(servicioAdmin.ver(idUsuario));
    }

    @PostMapping
    public ResponseEntity<UsuarioAdminResponse> crear(
            @Valid @RequestBody CrearUsuarioAdminRequest datos) {
        var creado = servicioAdmin.crear(datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioAdminResponse.de(creado));
    }

    @PutMapping("/{idUsuario}")
    public UsuarioAdminResponse actualizar(@PathVariable UUID idUsuario,
                                           @Valid @RequestBody ActualizarPerfilRequest datos) {
        return UsuarioAdminResponse.de(servicioAdmin.actualizar(idUsuario, datos));
    }

    /** RS10: al volver, las sesiones de ese usuario ya no valen. */
    @DeleteMapping("/{idUsuario}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID idUsuario) {
        servicioAdmin.eliminar(idUsuario);
    }
}
