package com.taskmanager.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  private final long expiration;

  public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expiration) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expiration = expiration;
  }
  public String generateToken(String email) {
    Date now = new Date();
    return Jwts.builder().subject(email).issuedAt(now).expiration(new Date(now.getTime() + expiration)).signWith(key).compact();
  }
  public String extractEmail(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject(); }
  public boolean isValid(String token) {
    try { extractEmail(token); return true; } catch (JwtException | IllegalArgumentException ex) { return false; }
  }
}
