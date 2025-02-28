package zw.co.afc.orbit.outpost.troop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.afc.orbit.outpost.troop.model.Application;

public interface ApplicationRepository extends JpaRepository<Application, String> {
}
