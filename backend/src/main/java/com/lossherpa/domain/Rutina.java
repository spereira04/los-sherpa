package com.lossherpa.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Rutina perteneciente siempre a un atleta (el dueno del dato).
 * No se edita en esta version: solo se crea y se lee.
 */
@Entity
@Table(name = "rutinas")
public class Rutina {

    // RS07: UUIDv4 como identificador publico y clave primaria.
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false, length = 120)
    private String nombre;

    /** Dueno del recurso. RS04: toda consulta de rutinas filtra por esta columna. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atleta_id", nullable = false, updatable = false)
    private Usuario atleta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private OrigenRutina origen;

    /** Solo si origen = ASIGNADA. Null para las rutinas propias del atleta. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrenador_creador_id", updatable = false)
    private Usuario entrenadorCreador;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant creadaEn = Instant.now();

    @OneToMany(mappedBy = "rutina", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    private List<EjercicioRutina> ejercicios = new ArrayList<>();

    protected Rutina() {
        // requerido por JPA
    }

    /** Rutina propia del atleta. */
    public static Rutina propia(String nombre, Usuario atleta) {
        Rutina rutina = new Rutina();
        rutina.nombre = nombre;
        rutina.atleta = atleta;
        rutina.origen = OrigenRutina.PROPIA;
        return rutina;
    }

    /** Rutina asignada por un entrenador a su atleta vinculado. */
    public static Rutina asignada(String nombre, Usuario atleta, Usuario entrenador) {
        Rutina rutina = new Rutina();
        rutina.nombre = nombre;
        rutina.atleta = atleta;
        rutina.origen = OrigenRutina.ASIGNADA;
        rutina.entrenadorCreador = entrenador;
        return rutina;
    }

    public void agregarEjercicio(String nombreEjercicio, int series, int repeticiones) {
        ejercicios.add(new EjercicioRutina(this, ejercicios.size() + 1, nombreEjercicio, series,
                repeticiones));
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Usuario getAtleta() {
        return atleta;
    }

    public OrigenRutina getOrigen() {
        return origen;
    }

    public Usuario getEntrenadorCreador() {
        return entrenadorCreador;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public List<EjercicioRutina> getEjercicios() {
        return Collections.unmodifiableList(ejercicios);
    }
}
