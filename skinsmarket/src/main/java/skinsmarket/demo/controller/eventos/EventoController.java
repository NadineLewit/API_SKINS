package skinsmarket.demo.controller.eventos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skinsmarket.demo.controller.common.ApiResponse;
import skinsmarket.demo.service.EventoService;

import java.util.List;

/**
 * Controlador REST para consulta de eventos e historial de precios.
 *
 * Todos los endpoints son públicos (sin token):
 *   - GET /historial/skin/{id}        → historial de una publicación
 *   - GET /historial/catalogo/{id}    → tendencia agregada por modelo del juego
 *
 * El historial agregado por catálogo es el más útil — muestra cómo evolucionó
 * el precio promedio diario de "AK-47 Redline" sumando todas sus publicaciones.
 */
@RestController
@RequestMapping("historial")
public class EventoController {

    @Autowired
    private EventoService eventoService;

    /**
     * Historial de precios de una publicación específica (cambios + ventas).
     * GET /historial/skin/{skinId}
     */
    @GetMapping("/skin/{skinId}")
    public ResponseEntity<?> historialDeSkin(@PathVariable Long skinId) {
        try {
            List<PrecioPuntoDto> puntos = eventoService.historialDeSkin(skinId);
            return ResponseEntity.ok(
                    ApiResponse.of("Historial de la skin (" + puntos.size() + " puntos)", puntos));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.of(e.getMessage()));
        }
    }

    /**
     * Historial agregado por catálogo (tendencia diaria).
     * GET /historial/catalogo/{catalogoId}?dias=30
     *
     * Devuelve, por día, el precio promedio/min/max y cuántas muestras hubo.
     * Ideal para gráficos de tendencia.
     */
    @GetMapping("/catalogo/{catalogoId}")
    public ResponseEntity<?> historialDeCatalogo(
            @PathVariable Long catalogoId,
            @RequestParam(defaultValue = "30") int dias) {
        try {
            List<PrecioPuntoDto> puntos = eventoService.historialDeCatalogo(catalogoId, dias);
            return ResponseEntity.ok(
                    ApiResponse.of("Tendencia de los últimos " + dias + " días (" +
                            puntos.size() + " días con datos)", puntos));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.of(e.getMessage()));
        }
    }
}
