package zw.co.afc.orbit.outpost.troop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.afc.orbit.outpost.troop.model.LogEntry;

public interface LogEntryRepository extends JpaRepository<LogEntry, String> {
}
