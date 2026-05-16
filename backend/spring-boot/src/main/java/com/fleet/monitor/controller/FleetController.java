package com.fleet.monitor.controller;

import com.fleet.monitor.model.AlertMessage;
import com.fleet.monitor.model.GpsMessage;
import com.fleet.monitor.repository.AlertMessageRepository;
import com.fleet.monitor.repository.GpsMessageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/fleet")
@CrossOrigin(origins = "*")
public class FleetController {

    private final GpsMessageRepository gpsRepo;
    private final AlertMessageRepository alertRepo;

    public FleetController(GpsMessageRepository gpsRepo,
                           AlertMessageRepository alertRepo) {
        this.gpsRepo = gpsRepo;
        this.alertRepo = alertRepo;
    }

    /**
     * GET /api/fleet/status
     * Resumen general del estado de la flota.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getFleetStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("totalVehicles", 3);
        status.put("totalGpsRecords", gpsRepo.count());
        status.put("activeAlerts", alertRepo.countByAlertTrue());
        status.put("timestamp", new Date());
        return ResponseEntity.ok(status);
    }

    /**
     * GET /api/fleet/vehicle/{id}/telemetria
     * Últimos 10 registros de GPS para un vehículo.
     */
    @GetMapping("/vehicle/{id}/telemetria")
    public ResponseEntity<List<GpsMessage>> getTelemetria(@PathVariable String id) {
        List<GpsMessage> data = gpsRepo.findTop10ByVehicleIdOrderByReceivedAtDesc(id);
        if (data.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(data);
    }

    /**
     * GET /api/fleet/alerts
     * Lista de alertas activas.
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<AlertMessage>> getActiveAlerts() {
        return ResponseEntity.ok(alertRepo.findByAlertTrueOrderByReceivedAtDesc());
    }

    /**
     * GET /api/fleet/vehicle/{id}/alerts
     * Alertas de un vehículo específico.
     */
    @GetMapping("/vehicle/{id}/alerts")
    public ResponseEntity<List<AlertMessage>> getVehicleAlerts(@PathVariable String id) {
        return ResponseEntity.ok(alertRepo.findByVehicleIdOrderByReceivedAtDesc(id));
    }
}
