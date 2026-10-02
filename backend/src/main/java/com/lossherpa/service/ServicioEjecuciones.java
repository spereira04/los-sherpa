package com.lossherpa.service;

import com.lossherpa.domain.Ejecucion;
import com.lossherpa.domain.EjercicioRutina;
import com.lossherpa.domain.Rol;
import com.lossherpa.domain.Rutina;
import com.lossherpa.domain.Usuario;
import com.lossherpa.error.RecursoNoEncontradoException;
import com.lossherpa.error.ReglaNegocioException;
import com.lossherpa.repository.EjecucionRepository;
import com.lossherpa.repository.RutinaRepository;
import com.lossherpa.security.UsuarioActual;
import com.lossherpa.web.dto.in.RegistrarEjecucionRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ejecuciones (historial de entrenamientos).
 *
 * Una rutina se puede completar muchas veces. Las ejecuciones no se editan ni se borran.
 *
 * RS04: registrar exige que la rutina sea del atleta autenticado, y leer el historial
 * filtra por el atleta autenticado o, para el entrenador, por las rutinas que el asigno con
 * vinculo activo.
 */
@Service
public class ServicioEjecuciones {

    private final EjecucionRepository ejecuciones;
    private final RutinaRepository rutinas;
    private final ServicioUsuarios servicioUsuarios;
    private final ServicioVinculacion servicioVinculacion;

    public ServicioEjecuciones(EjecucionRepository ejecuciones, RutinaRepository rutinas,
                               ServicioUsuarios servicioUsuarios,
                               ServicioVinculacion servicioVinculacion) {
        this.ejecuciones = ejecuciones;
        this.rutinas = rutinas;
        this.servicioUsuarios = servicioUsuarios;
        this.servicioVinculacion = servicioVinculacion;
    }

    /**
     * Registra una ejecucion de una rutina del atleta autenticado.
     *
     * Se exige que las series cargadas coincidan exactamente con las series que la rutina
     * define: ni de menos, ni de mas, ni repetidas, ni de un ejercicio de otra rutina.
     */
    @Transactional
    public Ejecucion registrar(UUID idRutina, RegistrarEjecucionRequest datos) {
        servicioUsuarios.autenticadoConRol(Rol.ATLETA);

        // RS04: (id de rutina + atleta autenticado). La rutina de otro atleta da 404.
        Rutina rutina = rutinas.buscarDeAtleta(idRutina, UsuarioActual.id())
                .orElseThrow(RecursoNoEncontradoException::generico);

        Map<UUID, EjercicioRutina> ejerciciosDeLaRutina = new HashMap<>();
        rutina.getEjercicios().forEach(e -> ejerciciosDeLaRutina.put(e.getId(), e));

        Set<String> esperadas = new HashSet<>();
        rutina.getEjercicios().forEach(ejercicio -> {
            for (int nro = 1; nro <= ejercicio.getSeries(); nro++) {
                esperadas.add(clave(ejercicio.getId(), nro));
            }
        });

        Ejecucion ejecucion = new Ejecucion(rutina, datos.fecha());
        Set<String> recibidas = new HashSet<>();

        for (RegistrarEjecucionRequest.SerieRequest serie : datos.series()) {
            // El idEjercicio del cliente solo vale si pertenece a ESTA rutina.
            EjercicioRutina ejercicio = ejerciciosDeLaRutina.get(serie.idEjercicio());
            if (ejercicio == null) {
                throw new ReglaNegocioException(
                        "Hay una serie que no corresponde a un ejercicio de esta rutina");
            }
            if (serie.nroSerie() > ejercicio.getSeries()) {
                throw new ReglaNegocioException(
                        "El ejercicio \"" + ejercicio.getNombre() + "\" tiene "
                                + ejercicio.getSeries() + " series");
            }
            if (!recibidas.add(clave(ejercicio.getId(), serie.nroSerie()))) {
                throw new ReglaNegocioException("Hay una serie cargada dos veces");
            }
            ejecucion.registrarSerie(ejercicio, serie.nroSerie(), serie.cargaKg());
        }

        if (!recibidas.equals(esperadas)) {
            throw new ReglaNegocioException(
                    "Hay que cargar la carga de todas las series de la rutina");
        }

        return ejecuciones.save(ejecucion);
    }

    @Transactional(readOnly = true)
    public List<Ejecucion> miHistorial(LocalDate desde, LocalDate hasta) {
        servicioUsuarios.autenticadoConRol(Rol.ATLETA);
        validarRango(desde, hasta);
        return ejecuciones.listarPorAtleta(UsuarioActual.id(), desde, hasta);
    }

    /**
     * RS04 + RS10 + reglas de visibilidad: solo ejecuciones de rutinas que este entrenador
     * asigno, y solo mientras el vinculo siga activo. Las ejecuciones de rutinas PROPIAS
     * del atleta no entran nunca.
     */
    @Transactional(readOnly = true)
    public List<Ejecucion> historialDeAtleta(UUID idAtleta, LocalDate desde, LocalDate hasta) {
        Usuario entrenador = servicioUsuarios.autenticadoConRol(Rol.ENTRENADOR);
        servicioVinculacion.vinculoActivoCon(entrenador.getId(), idAtleta);
        validarRango(desde, hasta);
        return ejecuciones.listarAsignadasPorEntrenador(entrenador.getId(), idAtleta, desde,
                hasta);
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ReglaNegocioException(
                    "La fecha de inicio no puede ser posterior a la de fin");
        }
    }

    private String clave(UUID idEjercicio, int nroSerie) {
        return idEjercicio + "#" + nroSerie;
    }
}
