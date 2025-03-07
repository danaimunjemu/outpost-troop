package zw.co.afc.orbit.outpost.troop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import zw.co.afc.orbit.outpost.troop.model.Alert;

import java.time.LocalDateTime;

public interface AlertRepository extends JpaRepository<Alert, String> {
    @Query("SELECT MAX(a.alertTime) FROM Alert a WHERE a.alertMessage = :alertMessage")
    LocalDateTime findLatestAlertTimeByAlertMessage(@Param("alertMessage") String alertMessage);


}
