package com.fleet.monitor.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.monitor.config.RabbitMQConfig;
import com.fleet.monitor.model.AlertMessage;
import com.fleet.monitor.repository.AlertMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class AlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertConsumer.class);

    private final RabbitTemplate rabbitTemplate;
    private final AlertMessageRepository alertRepo;
    private final ObjectMapper objectMapper;

    // Umbrales de alerta
    private static final double TEMP_MAX  = 8.0;   // °C
    private static final double FUEL_MIN  = 20.0;  // %

    public AlertConsumer(RabbitTemplate rabbitTemplate,
                         AlertMessageRepository alertRepo,
                         ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.alertRepo = alertRepo;
        this.objectMapper = objectMapper;
    }

    // ─── Temperatura ──────────────────────────────────────────────────────────

    @RabbitListener(queues = RabbitMQConfig.TEMP_ALERT_QUEUE)
    public void consumeTempAlert(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            double temp = node.path("temperature").asDouble();
            String vehicleId = node.path("vehicleId").asText();
            boolean isAlert = temp > TEMP_MAX;

            AlertMessage alert = buildAlert(vehicleId, node.path("timestamp").asText(),
                    "TEMPERATURA", message, isAlert);
            alertRepo.save(alert);

            if (isAlert) {
                log.warn("[ALERTA TEMPERATURA] Vehículo {} → {}°C (máx {}°C)", vehicleId, temp, TEMP_MAX);
                // Reenviar a cola de notificaciones
                String notification = String.format(
                        "{\"type\":\"TEMP_ALERT\",\"vehicleId\":\"%s\",\"value\":%.2f,\"threshold\":%.1f}",
                        vehicleId, temp, TEMP_MAX);
                rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_QUEUE, notification);
            } else {
                log.info("[TEMP] Vehículo {} → {}°C - OK", vehicleId, temp);
            }
        } catch (Exception e) {
            log.error("[TEMP] Error procesando alerta: {}", e.getMessage());
        }
    }

    // ─── Combustible ──────────────────────────────────────────────────────────

    @RabbitListener(queues = RabbitMQConfig.FUEL_QUEUE)
    public void consumeFuelAlert(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            double fuelLevel = node.path("fuelLevel").asDouble();
            String vehicleId = node.path("vehicleId").asText();
            boolean isAlert = fuelLevel < FUEL_MIN;

            AlertMessage alert = buildAlert(vehicleId, node.path("timestamp").asText(),
                    "COMBUSTIBLE", message, isAlert);
            alertRepo.save(alert);

            if (isAlert) {
                log.warn("[ALERTA COMBUSTIBLE] Vehículo {} → {}% (mín {}%)", vehicleId, fuelLevel, FUEL_MIN);
                // Reenviar a cola de notificaciones
                String notification = String.format(
                        "{\"type\":\"FUEL_ALERT\",\"vehicleId\":\"%s\",\"value\":%.2f,\"threshold\":%.1f}",
                        vehicleId, fuelLevel, FUEL_MIN);
                rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATION_QUEUE, notification);
            } else {
                log.info("[FUEL] Vehículo {} → {}% - OK", vehicleId, fuelLevel);
            }
        } catch (Exception e) {
            log.error("[FUEL] Error procesando alerta: {}", e.getMessage());
        }
    }

    // ─── Notificaciones (competing consumers demo) ────────────────────────────

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void consumeNotification(String message) {
        log.warn("[NOTIFICACION] → Enviando alerta al operador: {}", message);
        // Aquí se integraría con email/SMS/webhook
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private AlertMessage buildAlert(String vehicleId, String timestamp,
                                    String type, String raw, boolean isAlert) {
        AlertMessage a = new AlertMessage();
        a.setVehicleId(vehicleId);
        a.setTimestamp(timestamp);
        a.setType(type);
        a.setRawPayload(raw);
        a.setAlert(isAlert);
        return a;
    }
}
