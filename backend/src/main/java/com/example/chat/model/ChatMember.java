package com.example.chat.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="chat_members", uniqueConstraints=@UniqueConstraint(columnNames={"chat_id","user_id"}))
public class ChatMember {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) private Chat chat;
    @ManyToOne(optional=false) private AppUser user;
    @Column(nullable=false) private Instant joinedAt=Instant.now();
    protected ChatMember() {}
    public ChatMember(Chat chat, AppUser user){this.chat=chat;this.user=user;}
    public Long getId(){return id;} public Chat getChat(){return chat;} public AppUser getUser(){return user;} public Instant getJoinedAt(){return joinedAt;}
}
