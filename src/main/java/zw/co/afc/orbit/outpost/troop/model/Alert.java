package zw.co.afc.orbit.outpost.troop.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Garrison user; // Link to user (Garrison)

    private String alertMessage;
    private LocalDateTime alertTime;
    private String status; // SENT, FAILED, ACKNOWLEDGED

    private String medium; // EMAIL, WHATSAPP, SMS

    private boolean acknowledged;
    private LocalDateTime acknowledgedAt;
}