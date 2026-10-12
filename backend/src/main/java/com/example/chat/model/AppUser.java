package com.example.chat.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="app_users", uniqueConstraints=@UniqueConstraint(columnNames="email"))
public class AppUser {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=80) private String displayName;
    @Column(nullable=false, length=190) private String email;
    @Column(nullable=false) private String passwordHash;
    @Column(nullable=false) private Instant createdAt=Instant.now();
    protected AppUser() {}
    public AppUser(String displayName, String email, String passwordHash) { this.displayName=displayName; this.email=email.toLowerCase(); this.passwordHash=passwordHash; }
    public Long getId(){return id;} public String getDisplayName(){return displayName;} public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;} public Instant getCreatedAt(){return createdAt;}
}
