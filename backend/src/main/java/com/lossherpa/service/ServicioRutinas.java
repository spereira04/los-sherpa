package com.lossherpa.service;

import com.lossherpa.domain.EjercicioRutina;
import com.lossherpa.domain.Rol;
import com.lossherpa.domain.Rutina;
import com.lossherpa.domain.Usuario;
import com.lossherpa.error.ConflictoException;
import com.lossherpa.error.RecursoNoEncontradoException;
import com.lossherpa.error.ReglaNegocioException;
import com.lossherpa.repository.EjecucionRepository;
import com.lossherpa.repository.RutinaRepository;
import com.lossherpa.security.UsuarioActual;
import com.lossherpa.web.dto.in.ActualizarRutinaRequest;
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
    private final EjecucionRepository ejecuciones;
    private final ServicioUsuarios servicioUsuarios;
    private final ServicioVinculacion servicioVinculacion;

    public ServicioRutinas(RutinaRepository rutinas, EjecucionRepository ejecuciones,
                           ServicioUsuarios servicioUsuarios,
                           ServicioVinculacion servicioVinculacion) {
        this.rutinas = rutinas;
        this.ejecuciones = ejecuciones;
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
                .orElseThrow(() -> rutinaDelAtletaNoEncontrada(
                        rutinas, idRutina, UsuarioActual.id()));
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

    @Transactional
    public Rutina actualizarAsignada(UUID idAtleta, UUID idRutina,
                                   ActualizarRutinaRequest datos) {
        Usuario entrenador = servicioUsuarios.autenticadoConRol(Rol.ENTRENADOR);
        Rutina rutina = rutinas.buscarAsignadaDeEntrenador(idRutina, entrenador.getId())
                .orElseThrow(() -> rutinaAsignadaNoEncontrada(idRutina, entrenador.getId()));

        if (!rutina.getAtleta().getId().equals(idAtleta)) {
            throw new RecursoNoEncontradoException("La rutina no pertenece a ese atleta",
                    "la rutina es de otro atleta del entrenador");
        }
        if (datos.tieneCamposNoPermitidos()) {
            throw new ReglaNegocioException(
                    "Los campos de autorizacion no son editables en la rutina");
        }
        if (rutina.archivada()) {
            throw new RecursoNoEncontradoException("La rutina ya no existe o fue archivada",
                    "rutina archivada");
        }
        if (ejecuciones.existsByRutinaId(rutina.getId())) {
            throw new ConflictoException(
                    "No se puede modificar una rutina con historial de ejecuciones");
        }

        rutina.cambiarNombre(datos.nombre().trim());
        rutina.reemplazarEjercicios(datos.ejercicios().stream()
                .map(ejercicio -> new EjercicioRutina(rutina, 0, ejercicio.nombre().trim(),
                        ejercicio.series(), ejercicio.repeticiones(),
                        ejercicio.observaciones() == null ? null : ejercicio.observaciones().trim()))
                .toList());
        return rutina;
    }

    @Transactional
    public void eliminarAsignada(UUID idAtleta, UUID idRutina) {
        Usuario entrenador = servicioUsuarios.autenticadoConRol(Rol.ENTRENADOR);
        Rutina rutina = rutinas.buscarAsignadaDeEntrenador(idRutina, entrenador.getId())
                .orElseThrow(() -> rutinaAsignadaNoEncontrada(idRutina, entrenador.getId()));

        if (!rutina.getAtleta().getId().equals(idAtleta)) {
            throw new RecursoNoEncontradoException("La rutina no pertenece a ese atleta",
                    "la rutina es de otro atleta del entrenador");
        }
        rutina.archivar();
    }

    /**
     * RS12: motivo real del 404 de una rutina del atleta. La rutina se lee por id solo para
     * elegir el texto de la auditoria; la respuesta al cliente es la misma en todos los casos.
     */
    static RecursoNoEncontradoException rutinaDelAtletaNoEncontrada(RutinaRepository rutinas,
                                                                    UUID idRutina,
                                                                    UUID idAtleta) {
        String motivo = rutinas.findById(idRutina)
                .map(r -> r.getAtleta().getId().equals(idAtleta)
                        ? "rutina archivada"
                        : "rutina de otro usuario")
                .orElse("rutina inexistente");
        return RecursoNoEncontradoException.conMotivo(motivo);
    }

    /** RS12: igual que la anterior, para la consulta del entrenador. */
    private RecursoNoEncontradoException rutinaAsignadaNoEncontrada(UUID idRutina,
                                                                    UUID idEntrenador) {
        String motivo = rutinas.findById(idRutina)
                .map(r -> {
                    if (r.getEntrenadorCreador() == null
                            || !r.getEntrenadorCreador().getId().equals(idEntrenador)) {
                        return "rutina no asignada por el entrenador";
                    }
                    return r.archivada()
                            ? "rutina archivada"
                            : "sin vinculo vigente con el atleta de la rutina";
                })
                .orElse("rutina inexistente");
        return RecursoNoEncontradoException.conMotivo(motivo);
    }

    private Rutina armar(Rutina rutina, CrearRutinaRequest datos) {
        datos.ejercicios().forEach(ejercicio -> rutina.agregarEjercicio(
                ejercicio.nombre().trim(), ejercicio.series(), ejercicio.repeticiones(),
                ejercicio.observaciones() == null ? null : ejercicio.observaciones().trim()));
        return rutina;
    }
}
