package com.fleet.monitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gps_telemetria")
public class GpsMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vehicleId;
    private String timestamp;
    private double lat;
    private double lng;
    private double speed;

    @Column(name = "received_at")
    private LocalDateTime receivedAt = LocalDateTime.now();

    // ─── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }

    @Override
    public String toString() {
        return "GpsMessage{vehicleId='" + vehicleId + "', lat=" + lat + ", lng=" + lng + ", speed=" + speed + "}";
    }
}
