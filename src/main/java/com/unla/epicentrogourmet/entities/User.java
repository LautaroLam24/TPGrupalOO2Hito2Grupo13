package com.unla.epicentrogourmet.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true, length = 100)
    private String username; // es el email

    @Column(nullable = false, length = 60) // BCrypt siempre genera 60 caracteres
    private String password;

    private boolean enabled;

    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id")
    private Role role;

    public User(String username, String password, boolean enabled, Role role) {
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.role = role;
    }
}