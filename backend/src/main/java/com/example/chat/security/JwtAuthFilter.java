package com.example.chat.security;

import com.example.chat.repo.UserRepository;
import jakarta.servlet.*; import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component; import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException; import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
 private final JwtService jwt; private final UserRepository users;
 public JwtAuthFilter(JwtService jwt, UserRepository users){this.jwt=jwt;this.users=users;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException {
  String h=req.getHeader("Authorization");
  if(h!=null && h.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication()==null){
   try { String email=jwt.subject(h.substring(7)); if(users.findByEmailIgnoreCase(email).isPresent()){
    var principal=User.withUsername(email).password("").authorities(List.of("ROLE_USER")).build();
    var auth=new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()); auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req)); SecurityContextHolder.getContext().setAuthentication(auth);
   }} catch(Exception ignored) { SecurityContextHolder.clearContext(); }
  }
  chain.doFilter(req,res);
 }
}
