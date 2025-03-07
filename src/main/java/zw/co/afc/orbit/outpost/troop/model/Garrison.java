package zw.co.afc.orbit.outpost.troop.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import zw.co.afc.orbit.outpost.troop.enums.Role;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Garrison {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;
    private String email;
    private String phoneNumber;
    private Boolean active;

    @Enumerated(EnumType.STRING)
    private Role role; // ADMIN, TECHNICIAN, MANAGER

    private String alertPreferences; // JSON: {"whatsapp": true, "email": false, "sms": true}
}
