package com.example.chat.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="chats")
public class Chat {
    public enum Type { PRIVATE, GROUP }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=12) private Type type;
    @Column(length=100) private String name;
    @ManyToOne(optional=false) private AppUser createdBy;
    @Column(nullable=false) private Instant createdAt=Instant.now();
    protected Chat() {}
    public Chat(Type type, String name, AppUser createdBy){this.type=type;this.name=name;this.createdBy=createdBy;}
    public Long getId(){return id;} public Type getType(){return type;} public String getName(){return name;} public AppUser getCreatedBy(){return createdBy;} public Instant getCreatedAt(){return createdAt;}
}
