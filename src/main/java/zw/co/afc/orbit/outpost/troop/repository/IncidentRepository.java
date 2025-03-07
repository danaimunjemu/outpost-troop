package zw.co.afc.orbit.outpost.troop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.afc.orbit.outpost.troop.enums.IncidentStatus;
import zw.co.afc.orbit.outpost.troop.model.Incident;

public interface IncidentRepository extends JpaRepository<Incident, String> {
    Incident findByIncidentTypeAndStatus(String type, IncidentStatus status);

}
