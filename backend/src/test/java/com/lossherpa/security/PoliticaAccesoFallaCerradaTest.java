package com.lossherpa.security;

import com.lossherpa.domain.Rol;
import com.lossherpa.domain.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** RS18: la politica no deja pasar nada cuando no puede decidir. Sin Spring, solo la clase. */
class PoliticaAccesoFallaCerradaTest {

    private final PoliticaAcceso politica = new PoliticaAcceso();

    private PoliticaAcceso.Regla regla(PoliticaAcceso.Acceso acceso) {
        return new PoliticaAcceso.Regla(HttpMethod.GET, "/api/prueba", acceso,
                PoliticaAcceso.Atributo.NINGUNO);
    }

    private Authentication atleta() {
        Usuario usuario = new Usuario("ana@test.com", "hash", Rol.ATLETA, "Ana", "Suarez",
                LocalDate.of(1998, 3, 12));
        UsuarioAutenticado principal = new UsuarioAutenticado(usuario);
        return new UsernamePasswordAuthenticationToken(principal, null,
                principal.getAuthorities());
    }

    @Test
    @DisplayName("una excepcion al obtener la autenticacion se convierte en denegacion, sin el mensaje")
    void laExcepcionInesperadaDenega() {
        assertThatThrownBy(() -> politica.exigir(regla(PoliticaAcceso.Acceso.ATLETA), () -> {
            throw new IllegalStateException("contexto de seguridad corrupto");
        }))
                .isInstanceOf(AccesoDenegadoPorPolitica.class)
                .satisfies(e -> assertThat(((AccesoDenegadoPorPolitica) e).getMotivo())
                        .isEqualTo("error al evaluar la politica (IllegalStateException)"));
    }

    @Test
    @DisplayName("una regla DENEGADO deniega siempre, sin consultar nada mas")
    void unaReglaDeniegaSiempre() {
        assertThatThrownBy(() -> politica.exigir(regla(PoliticaAcceso.Acceso.DENEGADO),
                this::atleta))
                .isInstanceOf(AccesoDenegadoPorPolitica.class);
    }

    @Test
    @DisplayName("la falta de sesion sigue siendo un 401 (no se la trata como falla de la politica)")
    void sinSesionSigueSiendoNoAutenticado() {
        assertThatThrownBy(() -> politica.exigir(regla(PoliticaAcceso.Acceso.ATLETA), () -> null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @DisplayName("el rol equivocado sigue siendo la denegacion normal con su motivo")
    void elRolEquivocadoConservaSuMotivo() {
        assertThatThrownBy(() -> politica.exigir(regla(PoliticaAcceso.Acceso.ADMIN),
                this::atleta))
                .isInstanceOf(AccesoDenegadoPorPolitica.class)
                .satisfies(e -> assertThat(((AccesoDenegadoPorPolitica) e).getMotivo())
                        .isEqualTo("el endpoint exige el rol ADMIN"));
    }

    @Test
    @DisplayName("una regla PUBLICO no consulta la autenticacion, asi que no puede fallar por eso")
    void publicoNoPideLaAutenticacion() {
        AtomicBoolean consultada = new AtomicBoolean();
        politica.exigir(regla(PoliticaAcceso.Acceso.PUBLICO), () -> {
            consultada.set(true);
            throw new IllegalStateException();
        });
        assertThat(consultada).isFalse();
    }
}
