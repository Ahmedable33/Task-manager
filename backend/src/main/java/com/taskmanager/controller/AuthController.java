package com.taskmanager.controller;

import com.taskmanager.model.User;
import com.taskmanager.repository.UserRepository;
import com.taskmanager.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
  public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) { this.users = users; this.encoder = encoder; this.jwt = jwt; }
  public record Credentials(@Email @NotBlank String email, @Size(min=8) String password) {}
  public record Registration(@NotBlank @Size(min=2, max=80) String name, @Email @NotBlank String email, @Size(min=8) String password) {}
  @PostMapping("/register") public ResponseEntity<?> register(@Valid @RequestBody Registration body) {
    if (users.existsByEmail(body.email().toLowerCase())) return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email déjà utilisé"));
    User user = new User(); user.setName(body.name().trim()); user.setEmail(body.email().toLowerCase()); user.setPassword(encoder.encode(body.password())); users.save(user);
    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("token", jwt.generateToken(user.getEmail()), "email", user.getEmail(), "name", user.getName()));
  }
  @PostMapping("/login") public ResponseEntity<?> login(@Valid @RequestBody Credentials body) {
    return users.findByEmail(body.email().toLowerCase()).filter(user -> encoder.matches(body.password(), user.getPassword())).<ResponseEntity<?>>map(user -> ResponseEntity.ok(Map.of("token", jwt.generateToken(user.getEmail()), "email", user.getEmail(), "name", user.getName() == null ? user.getEmail() : user.getName()))).orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Identifiants invalides")));
  }
}
