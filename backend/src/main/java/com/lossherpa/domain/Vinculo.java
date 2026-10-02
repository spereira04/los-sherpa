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

import java.time.LocalDate;
import java.util.UUID;

/**
 * Vinculo entrenador-atleta. Activo mientras fechaFin sea null.
 * Invariante de negocio: un atleta tiene como maximo un vinculo activo.
 */
@Entity
@Table(name = "vinculos")
public class Vinculo {

    // RS07: UUIDv4 como identificador publico y clave primaria.
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrenador_id", nullable = false, updatable = false)
    private Usuario entrenador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atleta_id", nullable = false, updatable = false)
    private Usuario atleta;

    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDate fechaInicio = LocalDate.now();

    /** Null mientras el vinculo esta activo. */
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    protected Vinculo() {
        // requerido por JPA
    }

    public Vinculo(Usuario entrenador, Usuario atleta) {
        this.entrenador = entrenador;
        this.atleta = atleta;
    }

    public UUID getId() {
        return id;
    }

    public Usuario getEntrenador() {
        return entrenador;
    }

    public Usuario getAtleta() {
        return atleta;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public boolean estaActivo() {
        return fechaFin == null;
    }

    /** RS10: cerrar el vinculo quita el acceso del entrenador a partir del request siguiente. */
    public void cerrar(LocalDate fecha) {
        this.fechaFin = fecha;
    }
}
