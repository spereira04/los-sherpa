package com.lossherpa.repository;

import com.lossherpa.domain.Ejecucion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * RS04: el historial se consulta siempre acotado al dueno (atleta) o a la relacion
 * vigente del entrenador sobre las rutinas que el mismo asigno.
 */
public interface EjecucionRepository extends JpaRepository<Ejecucion, UUID> {

    /** Historial propio del atleta, con filtro opcional por rango de fechas. */
    /** Borrado de un usuario por el admin: se cargan para que orphanRemoval limpie las series. */
    @Query("select e from Ejecucion e where e.atleta.id = :atletaId")
    List<Ejecucion> listarTodasDelAtleta(@Param("atletaId") UUID atletaId);

    @EntityGraph(attributePaths = {"rutina", "series", "series.ejercicio"})
    @Query("""
            select distinct e from Ejecucion e
            where e.atleta.id = :atletaId
              and (:desde is null or e.fecha >= :desde)
              and (:hasta is null or e.fecha <= :hasta)
            order by e.fecha desc, e.creadaEn desc
            """)
    List<Ejecucion> listarPorAtleta(@Param("atletaId") UUID atletaId,
                                    @Param("desde") LocalDate desde,
                                    @Param("hasta") LocalDate hasta);

    /**
     * RS04 + RS10: el entrenador solo ve ejecuciones de rutinas que el asigno y
     * unicamente mientras el vinculo siga activo. Las ejecuciones de rutinas PROPIAS
     * del atleta nunca entran en el resultado.
     */
    @EntityGraph(attributePaths = {"rutina", "series", "series.ejercicio"})
    @Query("""
            select distinct e from Ejecucion e
            where e.atleta.id = :atletaId
              and e.rutina.origen = com.lossherpa.domain.OrigenRutina.ASIGNADA
              and e.rutina.entrenadorCreador.id = :entrenadorId
              and exists (select 1 from Vinculo v
                          where v.atleta.id = :atletaId
                            and v.entrenador.id = :entrenadorId
                            and v.fechaFin is null)
              and (:desde is null or e.fecha >= :desde)
              and (:hasta is null or e.fecha <= :hasta)
            order by e.fecha desc, e.creadaEn desc
            """)
    List<Ejecucion> listarAsignadasPorEntrenador(@Param("entrenadorId") UUID entrenadorId,
                                                 @Param("atletaId") UUID atletaId,
                                                 @Param("desde") LocalDate desde,
                                                 @Param("hasta") LocalDate hasta);
}
