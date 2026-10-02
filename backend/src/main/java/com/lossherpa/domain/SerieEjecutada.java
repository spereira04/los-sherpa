package com.lossherpa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/** Carga en kg levantada en una serie concreta de un ejercicio, dentro de una ejecucion. */
@Entity
@Table(name = "series_ejecutadas")
public class SerieEjecutada {

    // RS07: UUIDv4 como identificador publico y clave primaria.
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejecucion_id", nullable = false, updatable = false)
    private Ejecucion ejecucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejercicio_rutina_id", nullable = false, updatable = false)
    private EjercicioRutina ejercicio;

    @Column(name = "nro_serie", nullable = false, updatable = false)
    private int nroSerie;

    @Column(name = "carga_kg", nullable = false, updatable = false)
    private double cargaKg;

    protected SerieEjecutada() {
        // requerido por JPA
    }

    SerieEjecutada(Ejecucion ejecucion, EjercicioRutina ejercicio, int nroSerie, double cargaKg) {
        this.ejecucion = ejecucion;
        this.ejercicio = ejercicio;
        this.nroSerie = nroSerie;
        this.cargaKg = cargaKg;
    }

    public UUID getId() {
        return id;
    }

    public Ejecucion getEjecucion() {
        return ejecucion;
    }

    public EjercicioRutina getEjercicio() {
        return ejercicio;
    }

    public int getNroSerie() {
        return nroSerie;
    }

    public double getCargaKg() {
        return cargaKg;
    }
}
