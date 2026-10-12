package com.example.chat.controller;
import com.example.chat.dto.ChatDtos.*; import com.example.chat.service.ChatService; import jakarta.validation.Valid; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequestMapping("/api/chats") public class ChatController {private final ChatService service;public ChatController(ChatService service){this.service=service;}
 @GetMapping public List<ChatSummary> list(Authentication a){return service.list(a.getName());}
 @PostMapping("/private") public ChatSummary privateChat(Authentication a,@Valid @RequestBody PrivateChatRequest r){return service.createPrivate(a.getName(),r.otherUserId());}
 @PostMapping("/group") public ChatSummary group(Authentication a,@Valid @RequestBody GroupChatRequest r){return service.createGroup(a.getName(),r);}
 @GetMapping("/{chatId}/messages") public List<MessageResponse> history(Authentication a,@PathVariable Long chatId,@RequestParam(defaultValue="100") int limit){return service.history(a.getName(),chatId,limit);}
}
