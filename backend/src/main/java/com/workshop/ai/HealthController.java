package com.workshop.ai;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "Plataforma de Consumo IA",
                "message", "El backend está activo. Las rutas del API comienzan con /api.",
                "endpoints", List.of(
                        "GET /api/quota?userId=...",
                        "GET /api/quota/history?userId=...",
                        "POST /api/ai/generate",
                        "POST /api/quota/upgrade"));
    }
}
