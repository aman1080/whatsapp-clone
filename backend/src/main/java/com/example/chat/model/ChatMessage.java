package com.example.chat.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="chat_messages", indexes=@Index(name="idx_message_chat_time", columnList="chat_id, sentAt"))
public class ChatMessage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false) private Chat chat;
    @ManyToOne(optional=false) private AppUser sender;
    @Column(nullable=false, length=4000) private String content;
    @Column(nullable=false) private Instant sentAt=Instant.now();
    protected ChatMessage() {}
    public ChatMessage(Chat chat, AppUser sender, String content){this.chat=chat;this.sender=sender;this.content=content;}
    public Long getId(){return id;} public Chat getChat(){return chat;} public AppUser getSender(){return sender;} public String getContent(){return content;} public Instant getSentAt(){return sentAt;}
}
