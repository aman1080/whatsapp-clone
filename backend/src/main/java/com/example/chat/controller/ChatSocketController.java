package com.example.chat.controller;
import com.example.chat.dto.ChatDtos.*; import com.example.chat.service.ChatService; import jakarta.validation.Valid; import org.springframework.messaging.handler.annotation.MessageMapping; import org.springframework.messaging.simp.SimpMessagingTemplate; import org.springframework.security.access.AccessDeniedException; import org.springframework.stereotype.Controller; import java.security.Principal;
@Controller public class ChatSocketController {private final ChatService service;private final SimpMessagingTemplate broker;public ChatSocketController(ChatService service,SimpMessagingTemplate broker){this.service=service;this.broker=broker;}
 @MessageMapping("chat.send") public void send(Principal principal,@Valid MessageRequest request){if(principal==null)throw new AccessDeniedException("Unauthenticated");MessageResponse saved=service.saveMessage(principal.getName(),request);if("GROUP".equalsIgnoreCase(service.list(principal.getName()).stream().filter(c->c.id().equals(saved.chatId())).findFirst().orElseThrow().type())){broker.convertAndSend("/topic/chat/"+saved.chatId(),saved);}else{for(Long id:service.memberIds(saved.chatId())){ // private chat members receive through their authenticated user destination
   // Resolve the member's email through the summary returned by chat list.
   service.list(principal.getName()).stream().filter(c->c.id().equals(saved.chatId())).flatMap(c->c.members().stream()).filter(m->m.id().equals(id)).findFirst().ifPresent(m->broker.convertAndSendToUser(m.email(),"/queue/messages",saved));
  }} }
}
