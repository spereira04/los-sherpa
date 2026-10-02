package com.lossherpa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "usuarios",
        uniqueConstraints = @UniqueConstraint(name = "uk_usuarios_email", columnNames = "email")
)
public class Usuario {

    // RS07: el identificador publico es un UUIDv4 y es tambien la clave primaria real.
    // No existe ningun id secuencial que pueda filtrarse en una respuesta o en una URL.
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    /** Inmutable por requerimiento: el email no se puede modificar. */
    @Column(nullable = false, updatable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /** Inmutable por requerimiento: el rol no se puede cambiar despues del registro. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private Rol rol;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String apellido;

    /** Se guarda la fecha de nacimiento; la edad se calcula siempre (ver CalculadoraEdad). */
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(length = 1000)
    private String descripcion;

    /** Solo atletas. Null para los demas roles. */
    @Column(name = "peso_kg")
    private Double pesoKg;

    /** Solo entrenadores. Null para los demas roles. */
    @Column(name = "anios_servicio")
    private Integer aniosServicio;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    protected Usuario() {
        // requerido por JPA
    }

    public Usuario(String email, String passwordHash, Rol rol, String nombre, String apellido,
                   LocalDate fechaNacimiento) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.nombre = nombre;
        this.apellido = apellido;
        this.fechaNacimiento = fechaNacimiento;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(Double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public Integer getAniosServicio() {
        return aniosServicio;
    }

    public void setAniosServicio(Integer aniosServicio) {
        this.aniosServicio = aniosServicio;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public boolean esAtleta() {
        return rol == Rol.ATLETA;
    }

    public boolean esEntrenador() {
        return rol == Rol.ENTRENADOR;
    }
}
