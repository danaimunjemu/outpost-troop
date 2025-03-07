package zw.co.afc.orbit.outpost.troop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.afc.orbit.outpost.troop.model.Garrison;

import java.util.List;

public interface GarrisonRepository extends JpaRepository<Garrison, String> {
    List<Garrison> findAllByActiveTrue();
}
