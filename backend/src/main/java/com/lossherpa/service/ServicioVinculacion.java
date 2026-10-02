package com.lossherpa.service;

import com.lossherpa.domain.EstadoSolicitud;
import com.lossherpa.domain.Rol;
import com.lossherpa.domain.SolicitudVinculacion;
import com.lossherpa.domain.Usuario;
import com.lossherpa.domain.Vinculo;
import com.lossherpa.error.ConflictoException;
import com.lossherpa.error.RecursoNoEncontradoException;
import com.lossherpa.error.ReglaNegocioException;
import com.lossherpa.repository.SolicitudVinculacionRepository;
import com.lossherpa.repository.UsuarioRepository;
import com.lossherpa.repository.VinculoRepository;
import com.lossherpa.security.UsuarioActual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Solicitudes de vinculacion y vinculos.
 *
 * RS04: todas las operaciones parten del usuario autenticado (UsuarioActual) y lo pasan al
 * repositorio como parte del criterio de busqueda. Ninguna busca un recurso por id para
 * chequear el dueno despues.
 *
 * RS10: aceptar y desvincular son transaccionales y el efecto es inmediato, porque las
 * consultas de rutinas y ejecuciones exigen el vinculo activo en su propio where.
 */
@Service
public class ServicioVinculacion {

    private final UsuarioRepository usuarios;
    private final SolicitudVinculacionRepository solicitudes;
    private final VinculoRepository vinculos;
    private final ServicioUsuarios servicioUsuarios;

    public ServicioVinculacion(UsuarioRepository usuarios,
                               SolicitudVinculacionRepository solicitudes,
                               VinculoRepository vinculos,
                               ServicioUsuarios servicioUsuarios) {
        this.usuarios = usuarios;
        this.solicitudes = solicitudes;
        this.vinculos = vinculos;
        this.servicioUsuarios = servicioUsuarios;
    }

    // ------------------------------------------------------------------ atleta

    /** Listado publico de entrenadores con filtro por nombre o apellido. */
    @Transactional(readOnly = true)
    public List<Usuario> buscarEntrenadores(String texto) {
        servicioUsuarios.autenticadoConRol(Rol.ATLETA);
        String filtro = (texto == null || texto.isBlank()) ? null : texto.trim();
        return usuarios.buscarEntrenadores(filtro);
    }

    @Transactional(readOnly = true)
    public List<SolicitudVinculacion> misSolicitudes() {
        servicioUsuarios.autenticadoConRol(Rol.ATLETA);
        return solicitudes.listarPorAtleta(UsuarioActual.id());
    }

    /**
     * El atleta puede tener varias solicitudes pendientes a la vez, pero no mas de una al
     * mismo entrenador, y ninguna si ya tiene un entrenador activo.
     */
    @Transactional
    public SolicitudVinculacion enviarSolicitud(UUID idEntrenador) {
        Usuario atleta = servicioUsuarios.autenticadoConRol(Rol.ATLETA);

        if (vinculos.atletaTieneVinculoActivo(atleta.getId())) {
            throw new ConflictoException(
                    "Ya tenes un entrenador activo. Desvinculate antes de pedir otro");
        }

        // El destinatario tiene que existir y ser entrenador. Si no, 404 generico: no se
        // confirma la existencia de un usuario ajeno a partir de un id inventado (RS07).
        Usuario entrenador = usuarios.findByIdAndRol(idEntrenador, Rol.ENTRENADOR)
                .orElseThrow(() -> new RecursoNoEncontradoException("El entrenador no existe"));

        if (solicitudes.existePendiente(atleta.getId(), entrenador.getId())) {
            throw new ConflictoException("Ya tenes una solicitud pendiente con ese entrenador");
        }

        return solicitudes.save(new SolicitudVinculacion(atleta, entrenador));
    }

    /** RS04: la solicitud se busca por (id + atleta autenticado), no solo por id. */
    @Transactional
    public void cancelarSolicitud(UUID idSolicitud) {
        servicioUsuarios.autenticadoConRol(Rol.ATLETA);

        SolicitudVinculacion solicitud = solicitudes
                .buscarDeAtleta(idSolicitud, UsuarioActual.id())
                .orElseThrow(RecursoNoEncontradoException::generico);

        if (!solicitud.estaPendiente()) {
            throw new ConflictoException("Esa solicitud ya no esta pendiente");
        }
        solicitud.resolver(EstadoSolicitud.CANCELADA);
    }

    @Transactional(readOnly = true)
    public Vinculo miVinculoComoAtleta() {
        servicioUsuarios.autenticadoConRol(Rol.ATLETA);
        return vinculos.buscarActivoPorAtleta(UsuarioActual.id())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Todavia no tenes un entrenador"));
    }

    /** Cualquiera de los dos puede desvincular. Aca lo hace el atleta. */
    @Transactional
    public void desvincularComoAtleta() {
        servicioUsuarios.autenticadoConRol(Rol.ATLETA);

        Vinculo vinculo = vinculos.buscarActivoPorAtleta(UsuarioActual.id())
                .orElseThrow(() -> new ConflictoException("No tenes un entrenador activo"));

        // RS10: al cerrar el vinculo el entrenador pierde el acceso en el request siguiente.
        vinculo.cerrar(LocalDate.now());
    }

    // -------------------------------------------------------------- entrenador

    @Transactional(readOnly = true)
    public List<SolicitudVinculacion> solicitudesPendientes() {
        servicioUsuarios.autenticadoConRol(Rol.ENTRENADOR);
        return solicitudes.listarPendientesPorEntrenador(UsuarioActual.id());
    }

    /**
     * Aceptar es la operacion mas delicada: en la misma transaccion crea el vinculo y
     * cancela las demas solicitudes pendientes del atleta.
     */
    @Transactional
    public Vinculo aceptarSolicitud(UUID idSolicitud) {
        Usuario entrenador = servicioUsuarios.autenticadoConRol(Rol.ENTRENADOR);

        // RS04: (id + entrenador autenticado). Una solicitud de otro entrenador da 404.
        SolicitudVinculacion solicitud = solicitudes
                .buscarDeEntrenador(idSolicitud, entrenador.getId())
                .orElseThrow(RecursoNoEncontradoException::generico);

        if (!solicitud.estaPendiente()) {
            throw new ConflictoException("Esa solicitud ya fue resuelta");
        }

        Usuario atleta = solicitud.getAtleta();

        // Puede haber aceptado otro entrenador entre el envio y este momento.
        if (vinculos.atletaTieneVinculoActivo(atleta.getId())) {
            solicitud.resolver(EstadoSolicitud.CANCELADA);
            throw new ConflictoException("El atleta ya tiene un entrenador activo");
        }

        solicitud.resolver(EstadoSolicitud.ACEPTADA);

        // Un atleta tiene como maximo un entrenador: las demas pendientes se cancelan aca,
        // en la misma transaccion.
        solicitudes.listarOtrasPendientesDelAtleta(atleta.getId(), solicitud.getId())
                .forEach(otra -> otra.resolver(EstadoSolicitud.CANCELADA));

        return vinculos.save(new Vinculo(entrenador, atleta));
    }

    @Transactional
    public void rechazarSolicitud(UUID idSolicitud) {
        Usuario entrenador = servicioUsuarios.autenticadoConRol(Rol.ENTRENADOR);

        SolicitudVinculacion solicitud = solicitudes
                .buscarDeEntrenador(idSolicitud, entrenador.getId())
                .orElseThrow(RecursoNoEncontradoException::generico);

        if (!solicitud.estaPendiente()) {
            throw new ConflictoException("Esa solicitud ya fue resuelta");
        }
        solicitud.resolver(EstadoSolicitud.RECHAZADA);
    }

    @Transactional(readOnly = true)
    public List<Vinculo> misAtletasActivos() {
        servicioUsuarios.autenticadoConRol(Rol.ENTRENADOR);
        return vinculos.listarActivosPorEntrenador(UsuarioActual.id());
    }

    @Transactional(readOnly = true)
    public List<Vinculo> misExatletas() {
        servicioUsuarios.autenticadoConRol(Rol.ENTRENADOR);
        return vinculos.listarFinalizadosPorEntrenador(UsuarioActual.id());
    }

    /**
     * RS04 + RS10: el atleta se obtiene DESDE el vinculo activo, no con un findById sobre
     * usuarios. Si el vinculo no existe o ya se cerro, no hay nada que devolver.
     */
    @Transactional(readOnly = true)
    public Usuario verAtletaActivo(UUID idAtleta) {
        Usuario entrenador = servicioUsuarios.autenticadoConRol(Rol.ENTRENADOR);
        return vinculoActivoCon(entrenador.getId(), idAtleta).getAtleta();
    }

    @Transactional
    public void desvincularComoEntrenador(UUID idAtleta) {
        Usuario entrenador = servicioUsuarios.autenticadoConRol(Rol.ENTRENADOR);
        vinculoActivoCon(entrenador.getId(), idAtleta).cerrar(LocalDate.now());
    }

    /**
     * Punto unico donde el entrenador prueba su relacion vigente con un atleta.
     * Lo reutiliza el servicio de rutinas y ejecuciones (etapa 4).
     */
    @Transactional(readOnly = true)
    public Vinculo vinculoActivoCon(UUID idEntrenador, UUID idAtleta) {
        if (idAtleta == null) {
            throw new ReglaNegocioException("Falta el atleta");
        }
        return vinculos.buscarActivoPorEntrenadorYAtleta(idEntrenador, idAtleta)
                .orElseThrow(RecursoNoEncontradoException::generico);
    }
}
