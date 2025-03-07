package zw.co.afc.orbit.outpost.troop.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import zw.co.afc.orbit.outpost.troop.enums.IncidentStatus;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String incidentType;
    private String description;

    @Enumerated(EnumType.STRING)
    private IncidentStatus status; // OPEN, IN_PROGRESS, CLOSED

    private LocalDateTime createdAt;
    private LocalDateTime closedAt;

    private String priority; // LOW, MEDIUM, HIGH, CRITICAL

    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy; // User ID of the person who acknowledged

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL)
    private List<IncidentAlert> incidentAlerts;
}
