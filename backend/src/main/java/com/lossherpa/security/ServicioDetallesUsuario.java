package com.lossherpa.security;

import com.lossherpa.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ServicioDetallesUsuario implements UserDetailsService {

    private final UsuarioRepository usuarios;

    public ServicioDetallesUsuario(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return usuarios.findByEmailIgnoreCase(email)
                .map(UsuarioAutenticado::new)
                // Mensaje generico: no revela si el email existe (RS14).
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales invalidas"));
    }
}
