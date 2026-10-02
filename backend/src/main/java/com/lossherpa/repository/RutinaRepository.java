package com.lossherpa.repository;

import com.lossherpa.domain.Rutina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RS04: ninguna consulta recupera una rutina solo por su id.
 * El dueno (atleta) o la relacion vigente (vinculo activo + autoria) estan siempre en el where.
 *
 * Con open-in-view = false la sesion se cierra al salir del servicio, asi que cada consulta
 * trae con left join fetch los ejercicios y el entrenador creador, que son lo que el DTO de
 * salida necesita.
 */
public interface RutinaRepository extends JpaRepository<Rutina, UUID> {

    /** Borrado de un usuario por el admin: se cargan para que la cascada limpie ejercicios. */
    @Query("select r from Rutina r where r.atleta.id = :atletaId")
    List<Rutina> listarTodasDelAtleta(@Param("atletaId") UUID atletaId);

    /**
     * Al eliminar un entrenador, sus rutinas asignadas NO se borran: son del atleta, que
     * tiene derecho a conservarlas. Solo se suelta la referencia al creador.
     *
     * Es la unica escritura sobre entrenador_creador_id en toda la app; la columna esta
     * marcada updatable = false justamente para que no la toque nada mas.
     */
    @Modifying
    @Query("update Rutina r set r.entrenadorCreador = null where r.entrenadorCreador.id = :id")
    int soltarEntrenadorCreador(@Param("id") UUID idEntrenador);

    /** El atleta ve todas sus rutinas: asignadas y propias. */
    @Query("""
            select distinct r from Rutina r
            left join fetch r.ejercicios
            left join fetch r.entrenadorCreador
            where r.atleta.id = :atletaId
            order by r.creadaEn desc
            """)
    List<Rutina> listarPorAtleta(@Param("atletaId") UUID atletaId);

    /** RS04: lectura de una rutina propia del atleta autenticado. */
    @Query("""
            select distinct r from Rutina r
            left join fetch r.ejercicios
            left join fetch r.entrenadorCreador
            where r.id = :rutinaId and r.atleta.id = :atletaId
            """)
    Optional<Rutina> buscarDeAtleta(@Param("rutinaId") UUID rutinaId,
                                    @Param("atletaId") UUID atletaId);

    /**
     * RS04 + RS10: el entrenador solo ve rutinas que el asigno, de un atleta con vinculo
     * ACTIVO. Las rutinas PROPIAS del atleta quedan fuera del resultado por construccion.
     */
    @Query("""
            select distinct r from Rutina r
            left join fetch r.ejercicios
            left join fetch r.entrenadorCreador
            where r.atleta.id = :atletaId
              and r.origen = com.lossherpa.domain.OrigenRutina.ASIGNADA
              and r.entrenadorCreador.id = :entrenadorId
              and exists (select 1 from Vinculo v
                          where v.atleta.id = :atletaId
                            and v.entrenador.id = :entrenadorId
                            and v.fechaFin is null)
            order by r.creadaEn desc
            """)
    List<Rutina> listarAsignadasPorEntrenador(@Param("entrenadorId") UUID entrenadorId,
                                              @Param("atletaId") UUID atletaId);

    /** RS04 + RS10: lectura puntual de una rutina asignada, con vinculo activo obligatorio. */
    @Query("""
            select distinct r from Rutina r
            left join fetch r.ejercicios
            left join fetch r.entrenadorCreador
            where r.id = :rutinaId
              and r.origen = com.lossherpa.domain.OrigenRutina.ASIGNADA
              and r.entrenadorCreador.id = :entrenadorId
              and exists (select 1 from Vinculo v
                          where v.atleta.id = r.atleta.id
                            and v.entrenador.id = :entrenadorId
                            and v.fechaFin is null)
            """)
    Optional<Rutina> buscarAsignadaDeEntrenador(@Param("rutinaId") UUID rutinaId,
                                                @Param("entrenadorId") UUID entrenadorId);
}
