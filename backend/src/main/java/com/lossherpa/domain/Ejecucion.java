package com.lossherpa.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Una rutina se puede completar muchas veces: cada ejecucion es una entrada del historial.
 * No se edita ni se borra.
 */
@Entity
@Table(name = "ejecuciones")
public class Ejecucion {

    // RS07: UUIDv4 como identificador publico y clave primaria.
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rutina_id", nullable = false, updatable = false)
    private Rutina rutina;

    /**
     * Dueno del dato, denormalizado desde la rutina.
     * RS04: permite filtrar el historial por el atleta autenticado sin pasar por la rutina.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atleta_id", nullable = false, updatable = false)
    private Usuario atleta;

    @Column(nullable = false, updatable = false)
    private LocalDate fecha;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn = Instant.now();

    @OneToMany(mappedBy = "ejecucion", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<SerieEjecutada> series = new ArrayList<>();

    protected Ejecucion() {
        // requerido por JPA
    }

    public Ejecucion(Rutina rutina, LocalDate fecha) {
        this.rutina = rutina;
        this.atleta = rutina.getAtleta();
        this.fecha = fecha;
    }

    public void registrarSerie(EjercicioRutina ejercicio, int nroSerie, double cargaKg) {
        series.add(new SerieEjecutada(this, ejercicio, nroSerie, cargaKg));
    }

    public UUID getId() {
        return id;
    }

    public Rutina getRutina() {
        return rutina;
    }

    public Usuario getAtleta() {
        return atleta;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public List<SerieEjecutada> getSeries() {
        return Collections.unmodifiableList(series);
    }
}
