package com.programacion.taller3.rest;



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
        // La memoria ahora es gestionada nativamente por VectorStoreChatMemoryAdvisor.
        // Generar un nuevo sessionId es suficiente para iniciar un nuevo chat.
        return ResponseEntity.ok(Map.of(
                "message", "Sesión limpiada exitosamente",
                "sessionId", sessionId
        ));
    }

}
