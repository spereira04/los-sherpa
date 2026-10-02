package com.lossherpa;

import com.lossherpa.domain.Rol;
import com.lossherpa.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ContextoCargaTest {

    @Autowired
    private UsuarioRepository usuarios;

    @Test
    @DisplayName("el contexto levanta y el esquema se crea")
    void elContextoLevanta() {
        assertThat(usuarios.count()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("RS14: sin ADMIN_EMAIL/ADMIN_PASSWORD no se crea ningun admin")
    void sinVariablesDeEntornoNoHayAdmin() {
        assertThat(usuarios.findAll().stream().anyMatch(u -> u.getRol() == Rol.ADMIN)).isFalse();
    }
}
