package com.lossherpa.security;

import com.lossherpa.domain.Rol;
import com.lossherpa.domain.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Principal de la sesion. Solo guarda id, email y rol.
 *
 * RS10: se reconstruye desde la base de datos en cada request (FiltroSesionVigente),
 * asi que no actua como cache de permisos.
 */
public class UsuarioAutenticado implements UserDetails {

    private final UUID id;
    private final String email;
    private final Rol rol;
    private final String passwordHash;

    public UsuarioAutenticado(Usuario usuario) {
        this.id = usuario.getId();
        this.email = usuario.getEmail();
        this.rol = usuario.getRol();
        this.passwordHash = usuario.getPasswordHash();
    }

    public UUID getId() {
        return id;
    }

    public Rol getRol() {
        return rol;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /** Igualdad por id: la usa el SessionRegistry para encontrar las sesiones de un usuario. */
    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof UsuarioAutenticado o && Objects.equals(id, o.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
