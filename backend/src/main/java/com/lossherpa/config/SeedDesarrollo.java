package com.lossherpa.config;

import com.lossherpa.domain.Ejecucion;
import com.lossherpa.domain.Rol;
import com.lossherpa.domain.Rutina;
import com.lossherpa.domain.SolicitudVinculacion;
import com.lossherpa.domain.Usuario;
import com.lossherpa.domain.Vinculo;
import com.lossherpa.repository.EjecucionRepository;
import com.lossherpa.repository.RutinaRepository;
import com.lossherpa.repository.SolicitudVinculacionRepository;
import com.lossherpa.repository.UsuarioRepository;
import com.lossherpa.repository.VinculoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.CharBuffer;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * Datos de ejemplo.
 *
 * RS14: solo existe bajo el perfil `dev` Y con sherpa.seed.habilitado = true, que esta en
 * application-dev.yml. En el perfil por defecto esta clase no se instancia, asi que las
 * credenciales de ejemplo no existen en produccion. La contrasena compartida sale de
 * sherpa.seed.contrasena (variable SEED_PASSWORD), nunca esta escrita en el codigo.
 *
 * Solo siembra si no hay ningun entrenador ni atleta, para no duplicar en cada arranque.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "sherpa.seed.habilitado", havingValue = "true")
@Order(20)
public class SeedDesarrollo implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SeedDesarrollo.class);

    private final UsuarioRepository usuarios;
    private final VinculoRepository vinculos;
    private final SolicitudVinculacionRepository solicitudes;
    private final RutinaRepository rutinas;
    private final EjecucionRepository ejecuciones;
    private final PasswordEncoder passwordEncoder;
    private final char[] contrasena;

    public SeedDesarrollo(UsuarioRepository usuarios, VinculoRepository vinculos,
                          SolicitudVinculacionRepository solicitudes, RutinaRepository rutinas,
                          EjecucionRepository ejecuciones, PasswordEncoder passwordEncoder,
                          @Value("${sherpa.seed.contrasena}") String contrasena) {
        this.usuarios = usuarios;
        this.vinculos = vinculos;
        this.solicitudes = solicitudes;
        this.rutinas = rutinas;
        this.ejecuciones = ejecuciones;
        this.passwordEncoder = passwordEncoder;
        // Spring solo puede resolver @Value como String (la variable de entorno ya es texto
        // antes de llegar aca, eso esta fuera de nuestro control): se convierte a char[] de
        // una y no se guarda la referencia al String para no retenerlo en un campo de larga
        // vida del bean.
        this.contrasena = contrasena.toCharArray();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean yaHayDatos = usuarios.findAll().stream().anyMatch(u -> u.getRol() != Rol.ADMIN);
        if (yaHayDatos) {
            LOG.info("Seed de desarrollo: ya hay usuarios, no se siembra de nuevo");
            return;
        }

        try {
            Usuario lucia = entrenador("lucia.ferreyra@sherpa.dev", "Lucia", "Ferreyra", 1986, 11,
                    "Fuerza y acondicionamiento general. Preparacion para medias maratones.");
            Usuario marcos = entrenador("marcos.quiroga@sherpa.dev", "Marcos", "Quiroga", 1979, 18,
                    "Powerlifting y tecnica de los tres movimientos.");
            Usuario sofia = entrenador("sofia.benitez@sherpa.dev", "Sofia", "Benitez", 1992, 6,
                    "Entrenamiento funcional y movilidad.");

            Usuario ana = atleta("ana.suarez@sherpa.dev", "Ana", "Suarez", 1998, 64.0,
                    "Corro 10k y quiero mejorar fuerza de piernas.");
            Usuario bruno = atleta("bruno.medina@sherpa.dev", "Bruno", "Medina", 1995, 82.5,
                    "Vuelvo despues de una lesion de hombro.");
            Usuario carla = atleta("carla.rios@sherpa.dev", "Carla", "Rios", 2001, 57.0,
                    "Primera vez en un gimnasio.");
            Usuario diego = atleta("diego.alvarez@sherpa.dev", "Diego", "Alvarez", 1990, 90.0,
                    "Busco ganar masa muscular.");
            Usuario elena = atleta("elena.cabrera@sherpa.dev", "Elena", "Cabrera", 2003, 61.0,
                    "Entreno para jugar al hockey.");

            // --- Vinculos activos ---
            Vinculo vinculoAna = vinculos.save(new Vinculo(lucia, ana));
            vinculos.save(new Vinculo(lucia, bruno));
            vinculos.save(new Vinculo(marcos, diego));

            // --- Vinculo finalizado: queda en el historial de exatletas de Marcos ---
            Vinculo terminado = vinculos.save(new Vinculo(marcos, elena));
            terminado.cerrar(LocalDate.now().minusDays(20));

            // --- Solicitudes: Carla tiene dos pendientes, a Sofia y a Marcos ---
            solicitudes.save(new SolicitudVinculacion(carla, sofia));
            solicitudes.save(new SolicitudVinculacion(carla, marcos));

            // --- Rutinas asignadas ---
            Rutina fuerzaAna = rutinas.save(rutinaAsignada(lucia, ana, "Fuerza de piernas",
                    new String[] {"Sentadilla", "Prensa", "Zancadas"},
                    new int[] {4, 3, 3}, new int[] {8, 12, 12}));
            Rutina tironBruno = rutinas.save(rutinaAsignada(lucia, bruno, "Tren superior suave",
                    new String[] {"Remo con mancuerna", "Press militar"},
                    new int[] {3, 3}, new int[] {12, 10}));
            rutinas.save(rutinaAsignada(marcos, diego, "Banco y espalda",
                    new String[] {"Banco plano", "Dominadas", "Remo con barra"},
                    new int[] {5, 4, 3}, new int[] {5, 6, 8}));

            // Rutina que Elena conserva de cuando la entrenaba Marcos.
            rutinas.save(rutinaAsignada(marcos, elena, "Base de fuerza",
                    new String[] {"Peso muerto", "Sentadilla"},
                    new int[] {3, 3}, new int[] {6, 8}));

            // --- Rutinas propias: el entrenador NO las ve ---
            Rutina cardioAna = rutinas.save(rutinaPropia(ana, "Cardio de los martes",
                    new String[] {"Cinta", "Eliptica"},
                    new int[] {1, 1}, new int[] {30, 20}));
            rutinas.save(rutinaPropia(diego, "Abdominales en casa",
                    new String[] {"Plancha", "Crunch"},
                    new int[] {3, 3}, new int[] {45, 20}));

            // --- Ejecuciones: varias de la misma rutina, con progresion de cargas ---
            registrar(fuerzaAna, LocalDate.now().minusDays(21), 40);
            registrar(fuerzaAna, LocalDate.now().minusDays(14), 45);
            registrar(fuerzaAna, LocalDate.now().minusDays(7), 47.5);
            registrar(fuerzaAna, LocalDate.now().minusDays(1), 50);
            registrar(tironBruno, LocalDate.now().minusDays(5), 12);
            registrar(tironBruno, LocalDate.now().minusDays(2), 14);
            // Ejecucion de una rutina propia: no tiene que aparecerle a Lucia.
            registrar(cardioAna, LocalDate.now().minusDays(3), 0);

            LOG.info("Seed de desarrollo cargado: {} usuarios, {} rutinas, {} ejecuciones. "
                            + "Vinculo de ejemplo desde {}",
                    usuarios.count(), rutinas.count(), ejecuciones.count(),
                    vinculoAna.getFechaInicio());
        } finally {
            // La contrasena compartida ya no hace falta despues de sembrar: se borra del
            // array en vez de dejarla viva como referencia String hasta que pase el GC.
            Arrays.fill(contrasena, '\0');
        }
    }

    private Usuario entrenador(String email, String nombre, String apellido, int anioNacimiento,
                               int aniosServicio, String descripcion) {
        Usuario usuario = new Usuario(email, passwordEncoder.encode(CharBuffer.wrap(contrasena)),
                Rol.ENTRENADOR, nombre, apellido, LocalDate.of(anioNacimiento, 4, 15));
        usuario.setAniosServicio(aniosServicio);
        usuario.setDescripcion(descripcion);
        return usuarios.save(usuario);
    }

    private Usuario atleta(String email, String nombre, String apellido, int anioNacimiento,
                           double pesoKg, String descripcion) {
        Usuario usuario = new Usuario(email, passwordEncoder.encode(CharBuffer.wrap(contrasena)),
                Rol.ATLETA, nombre, apellido, LocalDate.of(anioNacimiento, 9, 3));
        usuario.setPesoKg(pesoKg);
        usuario.setDescripcion(descripcion);
        return usuarios.save(usuario);
    }

    private Rutina rutinaAsignada(Usuario entrenador, Usuario atleta, String nombre,
                                  String[] ejercicios, int[] series, int[] repeticiones) {
        Rutina rutina = Rutina.asignada(nombre, atleta, entrenador);
        for (int i = 0; i < ejercicios.length; i++) {
            rutina.agregarEjercicio(ejercicios[i], series[i], repeticiones[i]);
        }
        return rutina;
    }

    private Rutina rutinaPropia(Usuario atleta, String nombre, String[] ejercicios,
                                int[] series, int[] repeticiones) {
        Rutina rutina = Rutina.propia(nombre, atleta);
        for (int i = 0; i < ejercicios.length; i++) {
            rutina.agregarEjercicio(ejercicios[i], series[i], repeticiones[i]);
        }
        return rutina;
    }

    private void registrar(Rutina rutina, LocalDate fecha, double cargaBase) {
        Ejecucion ejecucion = new Ejecucion(rutina, fecha);
        rutina.getEjercicios().forEach(ejercicio -> {
            for (int nro = 1; nro <= ejercicio.getSeries(); nro++) {
                ejecucion.registrarSerie(ejercicio, nro, cargaBase + (nro - 1) * 2.5);
            }
        });
        ejecuciones.save(ejecucion);
    }
}
