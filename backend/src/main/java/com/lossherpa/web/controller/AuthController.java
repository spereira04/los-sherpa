package com.lossherpa.web.controller;

import com.lossherpa.domain.Usuario;
import com.lossherpa.error.CredencialesInvalidasException;
import com.lossherpa.repository.UsuarioRepository;
import com.lossherpa.security.AuditorAcceso;
import com.lossherpa.security.UsuarioAutenticado;
import com.lossherpa.service.PoliticaContrasena;
import com.lossherpa.service.ServicioAutenticacion;
import com.lossherpa.service.ServicioUsuarios;
import com.lossherpa.web.dto.in.LoginRequest;
import com.lossherpa.web.dto.in.RegistroRequest;
import com.lossherpa.web.dto.out.CsrfResponse;
import com.lossherpa.web.dto.out.SesionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.CharBuffer;
import java.util.Arrays;

/**
 * Autenticacion. El logout lo maneja el LogoutFilter de Spring Security configurado en
 * SecurityConfig (POST /api/auth/logout), que invalida la sesion y borra las cookies.
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final ServicioAutenticacion servicioAutenticacion;
    private final UsuarioRepository usuarios;
    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy estrategiaSesion;
    private final SecurityContextRepository contextRepository;
    private final AuditorAcceso auditor;

    public AuthController(ServicioAutenticacion servicioAutenticacion,
                          UsuarioRepository usuarios,
                          AuthenticationManager authenticationManager,
                          SessionAuthenticationStrategy estrategiaSesion,
                          SecurityContextRepository contextRepository,
                          AuditorAcceso auditor) {
        this.servicioAutenticacion = servicioAutenticacion;
        this.usuarios = usuarios;
        this.authenticationManager = authenticationManager;
        this.estrategiaSesion = estrategiaSesion;
        this.contextRepository = contextRepository;
        this.auditor = auditor;
    }

    /**
     * Entrega el token CSRF y deja la cookie XSRF-TOKEN. Es uno de los tres endpoints
     * publicos de la lista blanca (RS13).
     */
    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken(), token.getHeaderName());
    }

    /**
     * Registro publico. No loguea automaticamente: devuelve 201 y el frontend manda al login.
     * El rol solo puede ser ENTRENADOR o ATLETA (ver RolRegistro).
     */
    @PostMapping("/auth/registro")
    public ResponseEntity<SesionResponse> registrar(@Valid @RequestBody RegistroRequest datos) {
        Usuario creado = servicioAutenticacion.registrar(datos);
        return ResponseEntity.status(HttpStatus.CREATED).body(SesionResponse.de(creado));
    }

    /**
     * Login unico para los tres roles. El frontend usa el rol de la respuesta para elegir
     * el panel. La regeneracion del id de sesion la hace la estrategia de sesion.
     */
    @PostMapping("/auth/login")
    public SesionResponse login(@Valid @RequestBody LoginRequest datos,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        String email = ServicioUsuarios.normalizarEmail(datos.email());
        // La contrasena viaja como char[]; se borra en el finally apenas termina de usarse,
        // en vez de quedar como String inmutable hasta que pase el GC. El wrap en CharBuffer
        // evita crear nosotros una copia extra en String antes de pasarla a Spring Security
        // (que internamente si hace su propia copia para comparar contra el hash: eso ya
        // esta fuera de nuestro control).
        char[] normalizada = PoliticaContrasena.normalizar(datos.contrasena());
        try {
            Authentication autenticacion = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            email, CharBuffer.wrap(normalizada)));

            // Regenera el id de sesion y registra la sesion en el SessionRegistry (RS10).
            estrategiaSesion.onAuthentication(autenticacion, request, response);

            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(autenticacion);
            SecurityContextHolder.setContext(contexto);
            contextRepository.saveContext(contexto, request, response);

            UsuarioAutenticado principal = (UsuarioAutenticado) autenticacion.getPrincipal();
            return usuarios.findById(principal.getId())
                    .map(SesionResponse::de)
                    .orElseThrow(CredencialesInvalidasException::new);

        } catch (AuthenticationException e) {
            // RS12: queda el intento con el email, nunca la contrasena.
            auditor.loginFallido(request, email, "credenciales invalidas");
            throw new CredencialesInvalidasException();
        } finally {
            Arrays.fill(normalizada, '\0');
            Arrays.fill(datos.contrasena(), '\0');
        }
    }

    /** Usuario de la sesion actual. Lo usa el frontend al cargar para restaurar el estado. */
    @GetMapping("/auth/yo")
    public SesionResponse yo() {
        return usuarios.findById(com.lossherpa.security.UsuarioActual.id())
                .map(SesionResponse::de)
                .orElseThrow(CredencialesInvalidasException::new);
    }
}
