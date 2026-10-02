package com.lossherpa.repository;

import com.lossherpa.domain.Vinculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RS04: todas las consultas reciben el id del usuario autenticado y filtran por el.
 * No existe un findById generico que permita leer el vinculo de un tercero.
 *
 * La aplicacion corre con open-in-view = false: la sesion de Hibernate se cierra al salir
 * del servicio. Por eso cada consulta trae con join fetch la contraparte del vinculo que el
 * DTO de salida va a necesitar. Si falta un fetch, el error salta como 500 en los tests en
 * lugar de disimularse con una consulta extra por fila.
 */
public interface VinculoRepository extends JpaRepository<Vinculo, UUID> {

    @Query("""
            select v from Vinculo v
            join fetch v.entrenador
            where v.atleta.id = :atletaId and v.fechaFin is null
            """)
    Optional<Vinculo> buscarActivoPorAtleta(@Param("atletaId") UUID atletaId);

    /** RS10: la ausencia de resultado es la perdida inmediata de acceso del entrenador. */
    @Query("""
            select v from Vinculo v
            join fetch v.atleta
            where v.entrenador.id = :entrenadorId
              and v.atleta.id = :atletaId
              and v.fechaFin is null
            """)
    Optional<Vinculo> buscarActivoPorEntrenadorYAtleta(@Param("entrenadorId") UUID entrenadorId,
                                                       @Param("atletaId") UUID atletaId);

    @Query("""
            select v from Vinculo v
            join fetch v.atleta a
            where v.entrenador.id = :entrenadorId and v.fechaFin is null
            order by a.apellido asc, a.nombre asc
            """)
    List<Vinculo> listarActivosPorEntrenador(@Param("entrenadorId") UUID entrenadorId);

    /** Historial de exatletas: solo se usa para exponer nombre, apellido y fechas. */
    @Query("""
            select v from Vinculo v
            join fetch v.atleta a
            where v.entrenador.id = :entrenadorId and v.fechaFin is not null
            order by v.fechaFin desc
            """)
    List<Vinculo> listarFinalizadosPorEntrenador(@Param("entrenadorId") UUID entrenadorId);

    /** Borrado de un usuario por el admin: sus vinculos, en cualquiera de los dos roles. */
    @Query("select v from Vinculo v where v.atleta.id = :id or v.entrenador.id = :id")
    List<Vinculo> listarTodosDe(@Param("id") UUID idUsuario);

    @Query("""
            select count(v) > 0 from Vinculo v
            where v.atleta.id = :atletaId and v.fechaFin is null
            """)
    boolean atletaTieneVinculoActivo(@Param("atletaId") UUID atletaId);
}
