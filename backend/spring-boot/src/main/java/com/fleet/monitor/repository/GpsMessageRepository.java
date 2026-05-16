package com.fleet.monitor.repository;

import com.fleet.monitor.model.GpsMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GpsMessageRepository extends JpaRepository<GpsMessage, Long> {
    List<GpsMessage> findByVehicleIdOrderByReceivedAtDesc(String vehicleId);
    List<GpsMessage> findTop10ByVehicleIdOrderByReceivedAtDesc(String vehicleId);
}
