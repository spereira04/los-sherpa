package com.lossherpa.repository;

import com.lossherpa.domain.EstadoSolicitud;
import com.lossherpa.domain.SolicitudVinculacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RS04: las busquedas por id siempre incluyen al usuario autenticado en el where.
 * Nunca se recupera una solicitud solo por su id para chequear el dueno despues.
 */
public interface SolicitudVinculacionRepository extends JpaRepository<SolicitudVinculacion, UUID> {

    @Query("""
            select s from SolicitudVinculacion s
            join fetch s.entrenador e
            where s.atleta.id = :atletaId
            order by s.creadaEn desc
            """)
    List<SolicitudVinculacion> listarPorAtleta(@Param("atletaId") UUID atletaId);

    @Query("""
            select s from SolicitudVinculacion s
            join fetch s.atleta a
            where s.entrenador.id = :entrenadorId
              and s.estado = com.lossherpa.domain.EstadoSolicitud.PENDIENTE
            order by s.creadaEn asc
            """)
    List<SolicitudVinculacion> listarPendientesPorEntrenador(
            @Param("entrenadorId") UUID entrenadorId);

    /** RS04: el id del atleta autenticado forma parte del criterio de busqueda. */
    @Query("""
            select s from SolicitudVinculacion s
            where s.id = :solicitudId and s.atleta.id = :atletaId
            """)
    Optional<SolicitudVinculacion> buscarDeAtleta(@Param("solicitudId") UUID solicitudId,
                                                  @Param("atletaId") UUID atletaId);

    /** RS04: el id del entrenador autenticado forma parte del criterio de busqueda. */
    @Query("""
            select s from SolicitudVinculacion s
            join fetch s.atleta
            where s.id = :solicitudId and s.entrenador.id = :entrenadorId
            """)
    Optional<SolicitudVinculacion> buscarDeEntrenador(@Param("solicitudId") UUID solicitudId,
                                                      @Param("entrenadorId") UUID entrenadorId);

    @Query("""
            select count(s) > 0 from SolicitudVinculacion s
            where s.atleta.id = :atletaId
              and s.entrenador.id = :entrenadorId
              and s.estado = com.lossherpa.domain.EstadoSolicitud.PENDIENTE
            """)
    boolean existePendiente(@Param("atletaId") UUID atletaId,
                            @Param("entrenadorId") UUID entrenadorId);

    /** Al aceptar una solicitud se cancelan las demas pendientes del atleta (misma transaccion). */
    @Query("""
            select s from SolicitudVinculacion s
            where s.atleta.id = :atletaId
              and s.estado = com.lossherpa.domain.EstadoSolicitud.PENDIENTE
              and s.id <> :solicitudExcluida
            """)
    List<SolicitudVinculacion> listarOtrasPendientesDelAtleta(
            @Param("atletaId") UUID atletaId,
            @Param("solicitudExcluida") UUID solicitudExcluida);

    /** Borrado de un usuario por el admin: sus solicitudes, en cualquiera de los dos roles. */
    @Query("select s from SolicitudVinculacion s where s.atleta.id = :id or s.entrenador.id = :id")
    List<SolicitudVinculacion> listarTodasDe(@Param("id") UUID idUsuario);

    @Query("""
            select s from SolicitudVinculacion s
            where s.atleta.id = :atletaId and s.estado = :estado
            """)
    List<SolicitudVinculacion> listarPorAtletaYEstado(@Param("atletaId") UUID atletaId,
                                                      @Param("estado") EstadoSolicitud estado);
}
