package com.example.chat.config;

import com.example.chat.repo.*; import com.example.chat.security.JwtService;
import org.springframework.context.annotation.Configuration; import org.springframework.messaging.*; import org.springframework.messaging.simp.config.*;
import org.springframework.messaging.simp.stomp.*; import org.springframework.messaging.support.ChannelInterceptor; import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.*; import java.security.Principal;

@Configuration @EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
 private final JwtService jwt; private final UserRepository users; private final ChatMemberRepository members;
 public WebSocketConfig(JwtService jwt,UserRepository users,ChatMemberRepository members){this.jwt=jwt;this.users=users;this.members=members;}
 @Override public void registerStompEndpoints(StompEndpointRegistry registry){registry.addEndpoint("/ws").setAllowedOrigins("http://localhost:4200").withSockJS();}
 @Override public void configureMessageBroker(MessageBrokerRegistry registry){registry.setApplicationDestinationPrefixes("/app");registry.enableSimpleBroker("/topic","/queue");registry.setUserDestinationPrefix("/user");}
 @Override public void configureClientInboundChannel(ChannelRegistration registration){registration.interceptors(new ChannelInterceptor(){
  @Override public Message<?> preSend(Message<?> message,MessageChannel channel){
   StompHeaderAccessor a=MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class); if(a==null)return message;
   if(StompCommand.CONNECT.equals(a.getCommand())){String auth=a.getFirstNativeHeader("Authorization"); if(auth==null||!auth.startsWith("Bearer "))throw new IllegalArgumentException("Missing bearer token"); String email=jwt.subject(auth.substring(7)); if(users.findByEmailIgnoreCase(email).isEmpty())throw new IllegalArgumentException("Unknown user"); a.setUser((Principal)()->email);}
   if(StompCommand.SUBSCRIBE.equals(a.getCommand())){String dest=a.getDestination(); Principal p=a.getUser(); if(dest!=null&&dest.startsWith("/topic/chat/")){if(p==null)throw new IllegalArgumentException("Unauthenticated"); try{long id=Long.parseLong(dest.substring("/topic/chat/".length())); var u=users.findByEmailIgnoreCase(p.getName()).orElseThrow(); if(!members.existsByChatIdAndUserId(id,u.getId()))throw new IllegalArgumentException("Not a chat member");}catch(NumberFormatException e){throw new IllegalArgumentException("Invalid chat destination");}}}
   return message;
  }
 });}
}
