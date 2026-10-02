package com.lossherpa.service;

import com.lossherpa.domain.Rol;
import com.lossherpa.domain.Rutina;
import com.lossherpa.domain.Usuario;
import com.lossherpa.error.RecursoNoEncontradoException;
import com.lossherpa.repository.RutinaRepository;
import com.lossherpa.security.UsuarioActual;
import com.lossherpa.web.dto.in.CrearRutinaRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Rutinas.
 *
 * Reglas de visibilidad que se cumplen aca:
 *  - el atleta ve TODAS sus rutinas (asignadas y propias);
 *  - el entrenador ve solo las que EL asigno, y solo mientras el vinculo este activo;
 *  - el entrenador nunca ve las rutinas PROPIAS del atleta.
 *
 * RS04: el origen y el dueno de la rutina los decide este servicio a partir de quien esta
 * autenticado. El cliente no puede elegirlos.
 */
@Service
public class ServicioRutinas {

    private final RutinaRepository rutinas;
    private final ServicioUsuarios servicioUsuarios;
    private final ServicioVinculacion servicioVinculacion;

    public ServicioRutinas(RutinaRepository rutinas, ServicioUsuarios servicioUsuarios,
                           ServicioVinculacion servicioVinculacion) {
        this.rutinas = rutinas;
        this.servicioUsuarios = servicioUsuarios;
        this.servicioVinculacion = servicioVinculacion;
    }

    // ------------------------------------------------------------------ atleta

    @Transactional(readOnly = true)
    public List<Rutina> misRutinas() {
        servicioUsuarios.autenticadoConRol(Rol.ATLETA);
        return rutinas.listarPorAtleta(UsuarioActual.id());
    }

    /** RS04: la busqueda incluye al atleta autenticado. Una rutina ajena da 404. */
    @Transactional(readOnly = true)
    public Rutina verMiRutina(UUID idRutina) {
        servicioUsuarios.autenticadoConRol(Rol.ATLETA);
        return rutinas.buscarDeAtleta(idRutina, UsuarioActual.id())
                .orElseThrow(RecursoNoEncontradoException::generico);
    }

    @Transactional
    public Rutina crearPropia(CrearRutinaRequest datos) {
        Usuario atleta = servicioUsuarios.autenticadoConRol(Rol.ATLETA);
        return rutinas.save(armar(Rutina.propia(datos.nombre().trim(), atleta), datos));
    }

    // -------------------------------------------------------------- entrenador

    /** RS04 + RS10: exige vinculo activo y devuelve solo las rutinas asignadas por el. */
    @Transactional(readOnly = true)
    public List<Rutina> rutinasQueAsigneA(UUID idAtleta) {
        Usuario entrenador = servicioUsuarios.autenticadoConRol(Rol.ENTRENADOR);
        // Si no hay vinculo activo, esto ya corta con 404 antes de leer rutinas.
        servicioVinculacion.vinculoActivoCon(entrenador.getId(), idAtleta);
        return rutinas.listarAsignadasPorEntrenador(entrenador.getId(), idAtleta);
    }

    @Transactional
    public Rutina crearParaAtleta(UUID idAtleta, CrearRutinaRequest datos) {
        Usuario entrenador = servicioUsuarios.autenticadoConRol(Rol.ENTRENADOR);

        // El atleta se toma DEL VINCULO ACTIVO, no de un findById sobre usuarios.
        Usuario atleta = servicioVinculacion
                .vinculoActivoCon(entrenador.getId(), idAtleta)
                .getAtleta();

        return rutinas.save(armar(
                Rutina.asignada(datos.nombre().trim(), atleta, entrenador), datos));
    }

    private Rutina armar(Rutina rutina, CrearRutinaRequest datos) {
        datos.ejercicios().forEach(ejercicio -> rutina.agregarEjercicio(
                ejercicio.nombre().trim(), ejercicio.series(), ejercicio.repeticiones()));
        return rutina;
    }
}
