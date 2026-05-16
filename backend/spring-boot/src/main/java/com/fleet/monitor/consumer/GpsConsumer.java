package com.fleet.monitor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleet.monitor.config.RabbitMQConfig;
import com.fleet.monitor.model.GpsMessage;
import com.fleet.monitor.repository.GpsMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class GpsConsumer {

    private static final Logger log = LoggerFactory.getLogger(GpsConsumer.class);

    private final GpsMessageRepository gpsRepo;
    private final ObjectMapper objectMapper;

    public GpsConsumer(GpsMessageRepository gpsRepo, ObjectMapper objectMapper) {
        this.gpsRepo = gpsRepo;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitMQConfig.GPS_QUEUE)
    public void consumeGps(String message) {
        try {
            log.info("[GPS] Mensaje recibido: {}", message);
            GpsMessage gps = objectMapper.readValue(message, GpsMessage.class);
            gpsRepo.save(gps);
            log.info("[GPS] Almacenado: vehicleId={}, lat={}, lng={}, speed={} km/h",
                    gps.getVehicleId(), gps.getLat(), gps.getLng(), gps.getSpeed());
        } catch (Exception e) {
            log.error("[GPS] Error procesando mensaje: {}", e.getMessage());
        }
    }
}
