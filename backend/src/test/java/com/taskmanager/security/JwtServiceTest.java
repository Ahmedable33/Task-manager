package com.taskmanager.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JwtServiceTest {
  private static final String SECRET = "test-secret-that-is-at-least-32-characters-long";

  @Test
  void generatesAndExtractsEmail() {
    JwtService service = new JwtService(SECRET, 86_400_000L);

    String token = service.generateToken("user@example.com");

    assertEquals("user@example.com", service.extractEmail(token));
    assertTrue(service.isValid(token));
  }

  @Test
  void rejectsMalformedToken() {
    JwtService service = new JwtService(SECRET, 86_400_000L);

    assertFalse(service.isValid("not-a-jwt"));
  }
}
