package com.lossherpa.support;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Engancha un appender en memoria al logger AUDITORIA para poder afirmar sobre los eventos
 * de RS12 sin leer el archivo de log.
 */
public final class CapturadorDeAuditoria implements AutoCloseable {

    private final Logger logger;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    public CapturadorDeAuditoria() {
        this.logger = (Logger) LoggerFactory.getLogger("AUDITORIA");
        appender.start();
        logger.addAppender(appender);
    }

    /** Los eventos ya formateados, tal como quedarian escritos en auditoria.log. */
    public List<String> lineas() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    public String ultimaLinea() {
        List<String> todas = lineas();
        return todas.isEmpty() ? "" : todas.get(todas.size() - 1);
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        appender.stop();
    }
}
