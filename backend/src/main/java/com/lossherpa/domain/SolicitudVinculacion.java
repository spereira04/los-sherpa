package com.lossherpa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** Solicitud de vinculacion que nace siempre del atleta hacia un entrenador. */
@Entity
@Table(name = "solicitudes_vinculacion")
public class SolicitudVinculacion {

    // RS07: UUIDv4 como identificador publico y clave primaria.
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atleta_id", nullable = false, updatable = false)
    private Usuario atleta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrenador_id", nullable = false, updatable = false)
    private Usuario entrenador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn = Instant.now();

    @Column(name = "resuelta_en")
    private Instant resueltaEn;

    protected SolicitudVinculacion() {
        // requerido por JPA
    }

    public SolicitudVinculacion(Usuario atleta, Usuario entrenador) {
        this.atleta = atleta;
        this.entrenador = entrenador;
    }

    public UUID getId() {
        return id;
    }

    public Usuario getAtleta() {
        return atleta;
    }

    public Usuario getEntrenador() {
        return entrenador;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getResueltaEn() {
        return resueltaEn;
    }

    public boolean estaPendiente() {
        return estado == EstadoSolicitud.PENDIENTE;
    }

    /** Transicion unica de estado: una solicitud se resuelve una sola vez. */
    public void resolver(EstadoSolicitud nuevoEstado) {
        this.estado = nuevoEstado;
        this.resueltaEn = Instant.now();
    }
}
