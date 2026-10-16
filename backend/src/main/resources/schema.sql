-- RF3 / SH-37: la invariante de vinculacion se garantiza en la base, no solo con un exists
-- previo en el servicio (dos requests simultaneas podrian pasar ambas el chequeo).
-- Corre despues de que Hibernate crea o actualiza las tablas (defer-datasource-initialization).

-- Un atleta tiene como maximo un vinculo activo (activo = fecha_fin nula).
CREATE UNIQUE INDEX IF NOT EXISTS ux_vinculos_atleta_activo
    ON vinculos (atleta_id) WHERE fecha_fin IS NULL;

-- Una sola solicitud pendiente por par atleta-entrenador.
CREATE UNIQUE INDEX IF NOT EXISTS ux_solicitudes_pendiente_par
    ON solicitudes_vinculacion (atleta_id, entrenador_id) WHERE estado = 'PENDIENTE';
