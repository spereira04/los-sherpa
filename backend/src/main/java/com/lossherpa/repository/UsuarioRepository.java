package com.lossherpa.repository;

import com.lossherpa.domain.Rol;
import com.lossherpa.domain.Usuario;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Usuario> findByIdAndRol(UUID id, Rol rol);

    /**
     * Usuario que el admin puede gestionar. Excluye a los ADMIN por construccion, asi que
     * un admin no puede modificarse ni borrarse a si mismo ni tocar a otro admin: ese id
     * simplemente no se encuentra.
     */
    @Query("""
            select u from Usuario u
            where u.id = :id and u.rol <> com.lossherpa.domain.Rol.ADMIN
            """)
    Optional<Usuario> buscarGestionablePorAdmin(@Param("id") UUID id);

    /** Listado publico de entrenadores con filtro por nombre o apellido. */
    @Query("""
            select u from Usuario u
            where u.rol = com.lossherpa.domain.Rol.ENTRENADOR
              and (:texto is null
                   or lower(u.nombre) like lower(concat('%', :texto, '%'))
                   or lower(u.apellido) like lower(concat('%', :texto, '%')))
            order by u.apellido asc, u.nombre asc
            """)
    List<Usuario> buscarEntrenadores(@Param("texto") String texto);

    /** Listado del admin: solo entrenadores y atletas, nunca otros admins. */
    @Query("""
            select u from Usuario u
            where u.rol <> com.lossherpa.domain.Rol.ADMIN
              and (:rol is null or u.rol = :rol)
              and (:texto is null
                   or lower(u.nombre) like lower(concat('%', :texto, '%'))
                   or lower(u.apellido) like lower(concat('%', :texto, '%'))
                   or lower(u.email) like lower(concat('%', :texto, '%')))
            order by u.apellido asc, u.nombre asc
            """)
    List<Usuario> buscarParaAdmin(@Param("rol") Rol rol, @Param("texto") String texto,
                                  Pageable pageable);
}
