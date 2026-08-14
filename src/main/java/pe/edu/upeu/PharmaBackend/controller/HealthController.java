package pe.edu.upeu.PharmaBackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health(){
        Map<String, String> response =
        Map.of("STATUS", "UP",
                "mesagge", "BackEnd funcionando correctamente");

        return ResponseEntity.ok(response);
    }
}
