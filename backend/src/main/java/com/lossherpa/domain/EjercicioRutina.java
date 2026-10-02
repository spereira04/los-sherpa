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

@Entity
@Table(name = "ejercicios_rutina")
public class EjercicioRutina {

    // RS07: UUIDv4 como identificador publico y clave primaria.
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rutina_id", nullable = false, updatable = false)
    private Rutina rutina;

    @Column(nullable = false, updatable = false)
    private int orden;

    @Column(nullable = false, updatable = false, length = 120)
    private String nombre;

    @Column(nullable = false, updatable = false)
    private int series;

    @Column(nullable = false, updatable = false)
    private int repeticiones;

    protected EjercicioRutina() {
        // requerido por JPA
    }

    EjercicioRutina(Rutina rutina, int orden, String nombre, int series, int repeticiones) {
        this.rutina = rutina;
        this.orden = orden;
        this.nombre = nombre;
        this.series = series;
        this.repeticiones = repeticiones;
    }

    public UUID getId() {
        return id;
    }

    public Rutina getRutina() {
        return rutina;
    }

    public int getOrden() {
        return orden;
    }

    public String getNombre() {
        return nombre;
    }

    public int getSeries() {
        return series;
    }

    public int getRepeticiones() {
        return repeticiones;
    }
}
