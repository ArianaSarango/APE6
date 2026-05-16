package com.fleet.monitor.repository;

import com.fleet.monitor.model.AlertMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertMessageRepository extends JpaRepository<AlertMessage, Long> {
    List<AlertMessage> findByAlertTrueOrderByReceivedAtDesc();
    List<AlertMessage> findByVehicleIdOrderByReceivedAtDesc(String vehicleId);
    long countByAlertTrue();
}
