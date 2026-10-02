package com.lossherpa.service;

import com.lossherpa.domain.Rol;
import com.lossherpa.domain.Usuario;
import com.lossherpa.error.RecursoNoEncontradoException;
import com.lossherpa.repository.EjecucionRepository;
import com.lossherpa.repository.RutinaRepository;
import com.lossherpa.repository.SolicitudVinculacionRepository;
import com.lossherpa.repository.UsuarioRepository;
import com.lossherpa.repository.VinculoRepository;
import com.lossherpa.security.UsuarioAutenticado;
import com.lossherpa.web.dto.in.ActualizarPerfilRequest;
import com.lossherpa.web.dto.in.CrearUsuarioAdminRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Gestion de usuarios por el admin.
 *
 * RS10: eliminar o modificar un usuario tiene efecto en el request siguiente. El borrado es
 * transaccional y, al terminar, las sesiones de ese usuario quedan sin valor: el
 * FiltroSesionVigente vuelve a leer el usuario de la base en cada request, no lo encuentra,
 * invalida la sesion HTTP y responde 401. Ademas se marcan como expiradas las sesiones
 * registradas, para que el SessionRegistry no las siga reportando como activas.
 */
@Service
public class ServicioAdmin {

    private static final Logger LOG = LoggerFactory.getLogger(ServicioAdmin.class);
    private static final int LIMITE_LISTADO = 200;

    private final UsuarioRepository usuarios;
    private final EjecucionRepository ejecuciones;
    private final RutinaRepository rutinas;
    private final VinculoRepository vinculos;
    private final SolicitudVinculacionRepository solicitudes;
    private final ServicioAutenticacion servicioAutenticacion;
    private final SessionRegistry registroDeSesiones;

    public ServicioAdmin(UsuarioRepository usuarios, EjecucionRepository ejecuciones,
                         RutinaRepository rutinas, VinculoRepository vinculos,
                         SolicitudVinculacionRepository solicitudes,
                         ServicioAutenticacion servicioAutenticacion,
                         SessionRegistry registroDeSesiones) {
        this.usuarios = usuarios;
        this.ejecuciones = ejecuciones;
        this.rutinas = rutinas;
        this.vinculos = vinculos;
        this.solicitudes = solicitudes;
        this.servicioAutenticacion = servicioAutenticacion;
        this.registroDeSesiones = registroDeSesiones;
    }

    /** Listado de entrenadores y atletas. Los otros admins no aparecen nunca. */
    @Transactional(readOnly = true)
    public List<Usuario> listar(Rol rol, String texto) {
        if (rol == Rol.ADMIN) {
            // Pedir el listado de admins no es un error: simplemente no hay nada que mostrar.
            return List.of();
        }
        String filtro = (texto == null || texto.isBlank()) ? null : texto.trim();
        return usuarios.buscarParaAdmin(rol, filtro, PageRequest.of(0, LIMITE_LISTADO));
    }

    @Transactional(readOnly = true)
    public Usuario ver(UUID idUsuario) {
        return gestionable(idUsuario);
    }

    /** El rol viene de RolRegistro, que no incluye ADMIN: no se pueden crear admins. */
    @Transactional
    public Usuario crear(CrearUsuarioAdminRequest datos) {
        return servicioAutenticacion.registrar(datos.aRegistro());
    }

    /**
     * Modificacion: misma lista blanca que el perfil propio. El email y el rol siguen siendo
     * inmutables incluso para el admin.
     */
    @Transactional
    public Usuario actualizar(UUID idUsuario, ActualizarPerfilRequest datos) {
        Usuario usuario = gestionable(idUsuario);

        CalculadoraEdad.validarFechaNacimiento(datos.fechaNacimiento());
        ServicioUsuarios.validarCamposPorRol(usuario.getRol(), datos.pesoKg(),
                datos.aniosServicio());

        usuario.setNombre(datos.nombre().trim());
        usuario.setApellido(datos.apellido().trim());
        usuario.setFechaNacimiento(datos.fechaNacimiento());
        usuario.setDescripcion(ServicioUsuarios.limpiarTextoOpcional(datos.descripcion()));

        if (usuario.getRol() == Rol.ATLETA) {
            usuario.setPesoKg(datos.pesoKg());
        } else if (usuario.getRol() == Rol.ENTRENADOR) {
            usuario.setAniosServicio(datos.aniosServicio());
        }
        return usuarios.save(usuario);
    }

    /**
     * Baja de un usuario, en una sola transaccion.
     *
     * Decision: las rutinas que un entrenador le asigno a sus atletas NO se borran, porque
     * son del atleta. Solo se suelta la referencia al creador. En cambio, lo que es del
     * usuario eliminado (sus rutinas como atleta y sus ejecuciones) se borra con el.
     */
    @Transactional
    public void eliminar(UUID idUsuario) {
        Usuario usuario = gestionable(idUsuario);

        // 1. Historial propio: se cargan para que orphanRemoval limpie las series.
        ejecuciones.deleteAll(ejecuciones.listarTodasDelAtleta(usuario.getId()));

        // 2. Rutinas de las que el usuario es dueno (cascada a sus ejercicios).
        rutinas.deleteAll(rutinas.listarTodasDelAtleta(usuario.getId()));

        // 3. Rutinas que el usuario asigno como entrenador: quedan con el atleta.
        int rutinasConservadas = rutinas.soltarEntrenadorCreador(usuario.getId());

        // 4. Vinculos y solicitudes en cualquiera de los dos roles.
        vinculos.deleteAll(vinculos.listarTodosDe(usuario.getId()));
        solicitudes.deleteAll(solicitudes.listarTodasDe(usuario.getId()));

        usuarios.delete(usuario);

        // RS10: las sesiones de ese usuario dejan de servir en el request siguiente.
        expirarSesionesDe(usuario.getId());

        // No se loguea el email: alcanza con el id y el rol.
        LOG.info("Usuario eliminado por el admin: rol={} rutinasAsignadasConservadas={}",
                usuario.getRol(), rutinasConservadas);
    }

    private Usuario gestionable(UUID idUsuario) {
        return usuarios.buscarGestionablePorAdmin(idUsuario)
                .orElseThrow(RecursoNoEncontradoException::generico);
    }

    private void expirarSesionesDe(UUID idUsuario) {
        for (Object principal : registroDeSesiones.getAllPrincipals()) {
            if (principal instanceof UsuarioAutenticado autenticado
                    && autenticado.getId().equals(idUsuario)) {
                registroDeSesiones.getAllSessions(principal, false)
                        .forEach(SessionInformation::expireNow);
            }
        }
    }
}
