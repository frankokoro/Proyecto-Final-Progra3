package com.metalurgica1.metalurgica1.service;

import com.metalurgica1.metalurgica1.modelo.enums.EProceso;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Service
public class NotificacionClienteService {

    private final EmailService emailService;
    private final String frontendUrl;

    public NotificacionClienteService(
            EmailService emailService,
            @Value("${app.frontend.url}") String frontendUrl) {
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    @Async
    public void notificarCambioEstado(DatosCambioEstado datos) {
        if (datos.emailCliente() == null || datos.emailCliente().isBlank()) {
            log.warn(
                    "Registro {} sin email de cliente; no se envía notificación",
                    datos.idRegistro()
            );
            return;
        }

        String enlace = frontendUrl + "/panel/clientes/" + datos.idCliente();

        String html = """
                <div style="font-family: Arial, sans-serif; line-height: 1.5;">
                  <h2>Hola %s</h2>
                  <p>El estado de tu encargo <strong>%s</strong> cambió.</p>
                  <p style="font-size: 1.1rem;">
                    <strong>%s</strong> &rarr; <strong>%s</strong>
                  </p>
                  <p><strong>Tarea:</strong> %s</p>
                  <p>
                    <a href="%s">Ver el detalle en MetaGestor</a>
                  </p>
                </div>
                """.formatted(
                HtmlUtils.htmlEscape(texto(datos.nombreCliente(), "cliente")),
                HtmlUtils.htmlEscape(texto(datos.titulo(), "tu encargo")),
                datos.anterior().getProceso(),
                datos.nuevo().getProceso(),
                HtmlUtils.htmlEscape(texto(datos.tituloTarea(), "Sin tarea")),
                enlace
        );

        emailService.sendEmail(
                datos.emailCliente(),
                "Actualización de tu encargo: " + datos.titulo(),
                html
        );
    }

    private String texto(String valor, String defecto) {
        return valor == null || valor.isBlank() ? defecto : valor;
    }

    public record DatosCambioEstado(
            Long idRegistro,
            String titulo,
            Long idCliente,
            String emailCliente,
            String nombreCliente,
            String tituloTarea,
            EProceso anterior,
            EProceso nuevo
    ) {
    }
}