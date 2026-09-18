package com.taskmanager.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.taskmanager.repository.UserRepository;
import com.taskmanager.security.JwtService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
  @Mock private UserRepository users;
  @Mock private PasswordEncoder encoder;
  @Mock private JwtService jwt;
  @InjectMocks private AuthController controller;

  @Test
  void registersEmailInLowercaseAndReturnsToken() {
    when(users.existsByEmail("user@example.com")).thenReturn(false);
    when(encoder.encode("password123")).thenReturn("hashed-password");
    when(jwt.generateToken("user@example.com")).thenReturn("token");

    ResponseEntity<?> response = controller.register(
      new AuthController.Registration("Ahmed", "User@Example.com", "password123"));

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(Map.of("token", "token", "email", "user@example.com", "name", "Ahmed"), response.getBody());
    verify(encoder).encode("password123");
  }

  @Test
  void rejectsDuplicateEmail() {
    when(users.existsByEmail("user@example.com")).thenReturn(true);

    ResponseEntity<?> response = controller.register(
      new AuthController.Registration("Ahmed", "User@Example.com", "password123"));

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
  }

  @Test
  void rejectsInvalidLogin() {
    when(users.findByEmail("user@example.com")).thenReturn(Optional.empty());

    ResponseEntity<?> response = controller.login(
        new AuthController.Credentials("User@Example.com", "password123"));

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    verifyNoInteractions(encoder, jwt);
  }
}
