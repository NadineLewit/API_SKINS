package skinsmarket.demo.controller.ranking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skinsmarket.demo.controller.common.ApiResponse;
import skinsmarket.demo.service.EventoService;

import java.util.List;

/**
 * Controlador REST para los rankings del marketplace.
 *
 * Todos los endpoints son públicos (sin token):
 *   - GET /ranking/vendedores    → top sellers
 *   - GET /ranking/mas-vendidas  → skins más vendidas
 *   - GET /ranking/mas-vistas    → skins más populares (vistas)
 *
 * Los rankings se calculan en tiempo real desde la tabla `eventos`. Si la
 * cantidad de eventos crece mucho podríamos cachear los resultados, pero
 * por ahora consultamos directo (los índices sobre `eventos.tipo` y los
 * GROUP BY mantienen las queries rápidas).
 */
@RestController
@RequestMapping("ranking")
public class RankingController {

    @Autowired
    private EventoService eventoService;

    /**
     * Top vendedores ordenados por cantidad de ventas.
     * GET /ranking/vendedores?limit=10
     */
    @GetMapping("/vendedores")
    public ResponseEntity<?> topVendedores(
            @RequestParam(defaultValue = "10") int limit) {
        List<RankingVendedorDto> ranking = eventoService.topVendedores(limit);
        return ResponseEntity.ok(
                ApiResponse.of("Top " + ranking.size() + " vendedores", ranking));
    }

    /**
     * Top skins más vendidas (agrupadas por catálogo).
     * GET /ranking/mas-vendidas?limit=10
     */
    @GetMapping("/mas-vendidas")
    public ResponseEntity<?> topVendidas(
            @RequestParam(defaultValue = "10") int limit) {
        List<RankingSkinDto> ranking = eventoService.topSkinsVendidas(limit);
        return ResponseEntity.ok(
                ApiResponse.of("Top " + ranking.size() + " skins más vendidas", ranking));
    }

    /**
     * Top skins más vistas (más populares).
     * GET /ranking/mas-vistas?limit=10
     */
    @GetMapping("/mas-vistas")
    public ResponseEntity<?> topVistas(
            @RequestParam(defaultValue = "10") int limit) {
        List<RankingSkinDto> ranking = eventoService.topSkinsVistas(limit);
        return ResponseEntity.ok(
                ApiResponse.of("Top " + ranking.size() + " skins más vistas", ranking));
    }
}
