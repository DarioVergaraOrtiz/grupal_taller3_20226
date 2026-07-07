package com.programacion.taller3.rest;

import com.programacion.taller3.services.ConversationMemoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Gestión de sesiones de usuario.
 * Cada sesión tiene un UUID único que aísla la memoria conversacional.
 */
@RestController
@RequestMapping("/api/session")
public class SessionController {

    @Autowired
    private ConversationMemoryService memoryService;

    /**
     * Genera un nuevo ID de sesión.
     */
    @PostMapping("/new")
    public ResponseEntity<Map<String, String>> createSession() {
        String sessionId = UUID.randomUUID().toString();
        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }

    /**
     * Limpia la memoria de una sesión específica.
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, String>> clearSession(@PathVariable String sessionId) {
        memoryService.clearSession(sessionId);
        return ResponseEntity.ok(Map.of(
                "message", "Sesión limpiada exitosamente",
                "sessionId", sessionId
        ));
    }

}
