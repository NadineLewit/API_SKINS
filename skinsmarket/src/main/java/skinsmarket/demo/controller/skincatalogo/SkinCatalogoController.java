package skinsmarket.demo.controller.skincatalogo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skinsmarket.demo.controller.common.ApiResponse;
import skinsmarket.demo.entity.SkinCatalogo;
import skinsmarket.demo.service.SkinCatalogoService;

import java.util.List;

/**
 * Controlador REST para el catálogo maestro de skins.
 *
 * Funcionalidad:
 *   - GET (público): cualquiera puede consultar el catálogo
 *   - POST/DELETE (admin): solo el ADMIN puede modificar el catálogo
 *   - POST /sincronizar (admin): importa skins desde la API de ByMykel/CSGO-API
 *
 * Las skins en venta (entidad Skin) referencian a este catálogo a través de
 * la columna catalogo_id. Los USERS solo pueden publicar skins basadas en items
 * de este catálogo.
 */
@RestController
@RequestMapping("catalogo")
public class SkinCatalogoController {

    @Autowired
    private SkinCatalogoService skinCatalogoService;

    // =========================================================================
    // Endpoints públicos (lectura)
    // =========================================================================

    /** GET /catalogo — listar todo el catálogo */
    @GetMapping
    public ResponseEntity<?> listar() {
        List<SkinCatalogo> catalogo = skinCatalogoService.listar();
        return ResponseEntity.ok(
                ApiResponse.of("Catálogo de skins (" + catalogo.size() + ")", catalogo));
    }

    /** GET /catalogo/{id} — obtener una skin del catálogo por id */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            SkinCatalogo skin = skinCatalogoService.obtenerPorId(id);
            return ResponseEntity.ok(ApiResponse.of("Skin del catálogo encontrada", skin));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.of(e.getMessage()));
        }
    }

    /** GET /catalogo/buscar?nombre=AK — búsqueda parcial por nombre */
    @GetMapping("/buscar")
    public ResponseEntity<?> buscarPorNombre(@RequestParam String nombre) {
        List<SkinCatalogo> resultados = skinCatalogoService.buscarPorNombre(nombre);
        return ResponseEntity.ok(
                ApiResponse.of("Resultados (" + resultados.size() + ") para: " + nombre, resultados));
    }

    /** GET /catalogo/filtrar?arma=AK-47 o ?categoria=Rifles */
    @GetMapping("/filtrar")
    public ResponseEntity<?> filtrar(
            @RequestParam(required = false) String arma,
            @RequestParam(required = false) String categoria) {

        if (arma != null && !arma.isBlank()) {
            List<SkinCatalogo> resultados = skinCatalogoService.filtrarPorArma(arma);
            return ResponseEntity.ok(
                    ApiResponse.of("Skins de " + arma + " (" + resultados.size() + ")", resultados));
        }
        if (categoria != null && !categoria.isBlank()) {
            List<SkinCatalogo> resultados = skinCatalogoService.filtrarPorCategoria(categoria);
            return ResponseEntity.ok(
                    ApiResponse.of("Skins de categoría " + categoria + " (" + resultados.size() + ")",
                            resultados));
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.of("Especificá 'arma' o 'categoria' como query param"));
    }

    // =========================================================================
    // Endpoints de ADMIN
    // =========================================================================

    /**
     * POST /catalogo/sincronizar?limit=100
     *
     * Llama a la API pública de ByMykel/CSGO-API y persiste todas las skins
     * que aún no estén en la base. Los duplicados se omiten automáticamente
     * gracias al matching por externalId.
     *
     * El parámetro 'limit' es opcional. Si no se especifica, se importan TODAS
     * las skins de la API (varios miles). Recomendado en la primera corrida:
     * usar limit=100 o 200 para hacer una prueba sin saturar la BD.
     */
    @PostMapping("/sincronizar")
    public ResponseEntity<?> sincronizar(
            @RequestParam(required = false) Integer limit) {
        try {
            int insertadas = skinCatalogoService.sincronizarDesdeApi(limit);
            return ResponseEntity.ok(ApiResponse.of(
                    "Sincronización completada. Skins nuevas insertadas: " + insertadas));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.of("Error al sincronizar: " + e.getMessage()));
        }
    }

    /**
     * POST /catalogo
     *
     * Crea manualmente un item del catálogo. Útil cuando se quiere registrar
     * una skin que no está en la API pública (skins custom, internas, etc.).
     */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody SkinCatalogoRequest request) {
        SkinCatalogo creada = skinCatalogoService.crear(request);
        return ResponseEntity.status(201)
                .body(ApiResponse.of("Item de catálogo creado exitosamente", creada));
    }

    /** DELETE /catalogo/{id} — eliminar item del catálogo */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            skinCatalogoService.eliminar(id);
            return ResponseEntity.ok(ApiResponse.of("Item de catálogo eliminado exitosamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.of(e.getMessage()));
        }
    }
}
