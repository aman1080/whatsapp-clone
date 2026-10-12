package com.example.chat.controller;
import com.example.chat.dto.ChatDtos.Member; import com.example.chat.repo.UserRepository; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequestMapping("/api/users") public class UserController {private final UserRepository users; public UserController(UserRepository users){this.users=users;}
 @GetMapping public List<Member> all(Authentication a){return users.findAll().stream().filter(u->!u.getEmail().equalsIgnoreCase(a.getName())).map(u->new Member(u.getId(),u.getDisplayName(),u.getEmail())).toList();}
}
