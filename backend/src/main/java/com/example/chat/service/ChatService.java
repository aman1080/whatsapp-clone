package com.example.chat.service;

import com.example.chat.dto.ChatDtos.*; import com.example.chat.model.*; import com.example.chat.repo.*;
import org.springframework.data.domain.PageRequest; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@Service public class ChatService {
 private final UserRepository users; private final ChatRepository chats; private final ChatMemberRepository members; private final MessageRepository messages;
 public ChatService(UserRepository users,ChatRepository chats,ChatMemberRepository members,MessageRepository messages){this.users=users;this.chats=chats;this.members=members;this.messages=messages;}
 public AppUser current(String email){return users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
 public boolean isMember(Long chatId,Long userId){return members.existsByChatIdAndUserId(chatId,userId);}
 @Transactional public ChatSummary createPrivate(String email,Long otherId){AppUser me=current(email),other=users.findById(otherId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found")); if(me.getId().equals(other.getId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cannot chat with yourself");
  for(ChatMember cm:members.findByUserId(me.getId())){Chat c=cm.getChat();if(c.getType()==Chat.Type.PRIVATE && members.existsByChatIdAndUserId(c.getId(),other.getId()))return summary(c);}
  Chat c=chats.save(new Chat(Chat.Type.PRIVATE,null,me));members.save(new ChatMember(c,me));members.save(new ChatMember(c,other));return summary(c);}
 @Transactional public ChatSummary createGroup(String email,GroupChatRequest r){AppUser me=current(email);Chat c=chats.save(new Chat(Chat.Type.GROUP,r.name().trim(),me));Set<Long> ids=new LinkedHashSet<>(r.memberIds());ids.add(me.getId());for(Long id:ids){AppUser u=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown member: "+id));members.save(new ChatMember(c,u));}return summary(c);}
 public List<ChatSummary> list(String email){AppUser me=current(email);return members.findByUserId(me.getId()).stream().map(ChatMember::getChat).distinct().map(this::summary).toList();}
 public List<MessageResponse> history(String email,Long chatId,int limit){AppUser me=current(email);requireMember(chatId,me.getId());int safe=Math.max(1,Math.min(limit,200));List<ChatMessage> list=messages.findByChatIdOrderBySentAtDesc(chatId,PageRequest.of(0,safe));Collections.reverse(list);return list.stream().map(this::messageDto).toList();}
 @Transactional public MessageResponse saveMessage(String email,MessageRequest r){AppUser me=current(email);Chat c=chats.findById(r.chatId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Chat not found"));requireMember(c.getId(),me.getId());String content=r.content().trim();if(content.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Empty message");return messageDto(messages.save(new ChatMessage(c,me,content)));}
 public List<Long> memberIds(Long chatId){return members.findByChatId(chatId).stream().map(m->m.getUser().getId()).toList();}
 public Long userId(String email){return current(email).getId();}
 private void requireMember(Long chatId,Long userId){if(!isMember(chatId,userId))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not a member of this chat");}
 private ChatSummary summary(Chat c){List<Member> ms=members.findByChatId(c.getId()).stream().map(m->new Member(m.getUser().getId(),m.getUser().getDisplayName(),m.getUser().getEmail())).toList();return new ChatSummary(c.getId(),c.getType().name(),c.getName(),ms,c.getCreatedAt());}
 private MessageResponse messageDto(ChatMessage m){return new MessageResponse(m.getId(),m.getChat().getId(),m.getSender().getId(),m.getSender().getDisplayName(),m.getContent(),m.getSentAt());}
}
