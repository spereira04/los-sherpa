package com.lossherpa.web.dto.out;

import com.lossherpa.domain.Ejecucion;
import com.lossherpa.domain.SerieEjecutada;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Una entrada del historial, con la carga de cada serie. */
public record EjecucionResponse(
        UUID id,
        LocalDate fecha,
        UUID idRutina,
        String nombreRutina,
        List<SerieResponse> series
) {

    public record SerieResponse(
            UUID idEjercicio,
            String nombreEjercicio,
            int nroSerie,
            double cargaKg
    ) {

        static SerieResponse de(SerieEjecutada serie) {
            return new SerieResponse(
                    serie.getEjercicio().getId(),
                    serie.getEjercicio().getNombre(),
                    serie.getNroSerie(),
                    serie.getCargaKg());
        }
    }

    public static EjecucionResponse de(Ejecucion ejecucion) {
        List<SerieResponse> series = ejecucion.getSeries().stream()
                .sorted(Comparator
                        .comparingInt((SerieEjecutada s) -> s.getEjercicio().getOrden())
                        .thenComparingInt(SerieEjecutada::getNroSerie))
                .map(SerieResponse::de)
                .toList();

        return new EjecucionResponse(
                ejecucion.getId(),
                ejecucion.getFecha(),
                ejecucion.getRutina().getId(),
                ejecucion.getRutina().getNombre(),
                series);
    }
}
