package skinsmarket.demo.controller.skincatalogo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import skinsmarket.demo.controller.common.ApiResponse;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.SkinCatalogo;
import skinsmarket.demo.repository.SkinCatalogoRepository;
import skinsmarket.demo.repository.SkinRepository;
import skinsmarket.demo.service.SkinCatalogoService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador del catálogo maestro de skins.
 *
 * IMPORTANTE: GET /catalogo SIEMPRE pagina (default page=0, size=50). El catálogo
 * tiene ~22.000 skins y devolver todo de una sola vez:
 *   - Tarda mucho (varios segundos)
 *   - Genera responses gigantes (~30MB) que crashean a Insomnia
 *   - No tiene sentido para un frontend real
 */
@RestController
@RequestMapping("catalogo")
public class SkinCatalogoController {

    @Autowired
    private SkinCatalogoService skinCatalogoService;

    @Autowired
    private SkinCatalogoRepository skinCatalogoRepository;

    @Autowired
    private SkinRepository skinRepository;

    /**
     * GET /catalogo?page=0&size=50
     * Devuelve una página del catálogo. Default 50 items por página.
     */
    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        if (size > 500) size = 500;
        if (size < 1) size = 50;
        if (page < 0) page = 0;

        Pageable pageable = PageRequest.of(page, size);
        var pageResult = skinCatalogoRepository.findAll(pageable);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("page", page);
        respuesta.put("size", size);
        respuesta.put("totalElements", pageResult.getTotalElements());
        respuesta.put("totalPages", pageResult.getTotalPages());
        respuesta.put("hasNext", pageResult.hasNext());
        respuesta.put("hasPrevious", pageResult.hasPrevious());
        respuesta.put("content", pageResult.getContent());

        return ResponseEntity.ok(ApiResponse.of(
                "Catálogo paginado (página " + (page + 1) + " de " +
                pageResult.getTotalPages() + ")", respuesta));
    }

    /**
     * GET /catalogo/count — total de skins en el catálogo.
     * Útil para verificar que el seeder cargó bien sin traer la lista entera.
     */
    @GetMapping("/count")
    public ResponseEntity<?> count() {
        long total = skinCatalogoRepository.count();
        Map<String, Object> info = new HashMap<>();
        info.put("totalSkinsEnCatalogo", total);
        return ResponseEntity.ok(ApiResponse.of("Conteo del catálogo", info));
    }

    /**
     * GET /catalogo/admin/market-stats?page=0&size=50
     *
     * Vista ADMIN del catálogo con stock/precios calculados desde publicaciones
     * disponibles. El stock NO pertenece a cada publicación visible al usuario:
     * acá representa cuántas publicaciones activas y disponibles existen para
     * ese item del catálogo.
     */
    @GetMapping("/admin/market-stats")
    public ResponseEntity<?> marketStatsAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        if (size > 500) size = 500;
        if (size < 1) size = 50;
        if (page < 0) page = 0;

        Pageable pageable = PageRequest.of(page, size);
        var pageResult = skinCatalogoRepository.findAll(pageable);
        List<SkinCatalogo> catalogoItems = pageResult.getContent();
        List<Long> catalogoIds = catalogoItems.stream()
                .map(SkinCatalogo::getId)
                .toList();

        Map<Long, List<Skin>> publicacionesPorCatalogo = catalogoIds.isEmpty()
                ? Map.of()
                : skinRepository
                        .findByCatalogo_IdInAndActiveTrueAndStockGreaterThan(catalogoIds, 0)
                        .stream()
                        .collect(Collectors.groupingBy(s -> s.getCatalogo().getId()));

        List<SkinCatalogoMarketStatsResponse> content = new ArrayList<>();
        for (SkinCatalogo cat : catalogoItems) {
            content.add(toMarketStats(cat, publicacionesPorCatalogo.getOrDefault(cat.getId(), List.of())));
        }

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("page", page);
        respuesta.put("size", size);
        respuesta.put("totalElements", pageResult.getTotalElements());
        respuesta.put("totalPages", pageResult.getTotalPages());
        respuesta.put("hasNext", pageResult.hasNext());
        respuesta.put("hasPrevious", pageResult.hasPrevious());
        respuesta.put("content", content);

        return ResponseEntity.ok(ApiResponse.of(
                "Catálogo admin con stock/precio promedio calculado", respuesta));
    }

    /** GET /catalogo/{id} — detalle de un item. */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            SkinCatalogo cat = skinCatalogoService.obtenerPorId(id);
            return ResponseEntity.ok(ApiResponse.of("Item del catálogo", cat));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ApiResponse.of(e.getMessage()));
        }
    }

    /**
     * GET /catalogo/buscar?nombre=Redline
     * Búsqueda parcial por nombre. Limita a 200 resultados.
     */
    @GetMapping("/buscar")
    public ResponseEntity<?> buscar(@RequestParam String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.of("Debés especificar 'nombre' como query param"));
        }
        List<SkinCatalogo> resultados = skinCatalogoService.buscarPorNombre(nombre);
        if (resultados.size() > 200) {
            resultados = resultados.subList(0, 200);
        }
        return ResponseEntity.ok(ApiResponse.of(
                "Resultados de búsqueda: " + resultados.size() + " (max 200)",
                resultados));
    }

    /**
     * GET /catalogo/filtrar?arma=AK-47&categoria=Rifle
     * Filtra por arma y/o categoría. Al menos uno es obligatorio.
     */
    @GetMapping("/filtrar")
    public ResponseEntity<?> filtrar(
            @RequestParam(required = false) String arma,
            @RequestParam(required = false) String categoria) {
        boolean conArma = arma != null && !arma.isBlank();
        boolean conCategoria = categoria != null && !categoria.isBlank();
        if (!conArma && !conCategoria) {
            return ResponseEntity.badRequest().body(ApiResponse.of(
                    "Debés especificar al menos uno: ?arma= o ?categoria="));
        }
        List<SkinCatalogo> resultados = skinCatalogoService.filtrar(arma, categoria);
        if (resultados.size() > 200) {
            resultados = resultados.subList(0, 200);
        }
        return ResponseEntity.ok(ApiResponse.of(
                "Resultados del filtro: " + resultados.size() + " (max 200)",
                resultados));
    }

    /**
     * POST /catalogo/sincronizar?limit=N — solo ADMIN.
     * Si no se manda ?limit, sincroniza todas las skins de la API (~22000).
     */
    @PostMapping("/sincronizar")
    public ResponseEntity<?> sincronizar(
            @RequestParam(required = false, defaultValue = "-1") int limit) {
        try {
            int insertadas = skinCatalogoService.sincronizarDesdeApi(limit);
            return ResponseEntity.ok(ApiResponse.of(
                    "Sincronización completada. Insertadas: " + insertadas));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        }
    }

    /** POST /catalogo — crear item manual (ADMIN). */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody SkinCatalogoRequest request) {
        try {
            SkinCatalogo cat = skinCatalogoService.crear(request);
            return ResponseEntity.status(201)
                    .body(ApiResponse.of("Item del catálogo creado", cat));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.of(e.getMessage()));
        }
    }

    /** DELETE /catalogo/{id} — ADMIN. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        boolean borrado = skinCatalogoService.eliminar(id);
        if (!borrado) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.of("Item no encontrado: " + id));
        }
        return ResponseEntity.ok(ApiResponse.of("Item eliminado"));
    }

    private SkinCatalogoMarketStatsResponse toMarketStats(SkinCatalogo cat, List<Skin> publicaciones) {
        SkinCatalogoMarketStatsResponse r = new SkinCatalogoMarketStatsResponse();
        r.setCatalogoId(cat.getId());
        r.setName(cat.getName());
        r.setMarketHashName(cat.getMarketHashName());
        r.setWeaponName(cat.getWeaponName());
        r.setCategoryName(cat.getCategoryName());
        r.setExteriorName(cat.getExteriorName());
        r.setImageUrl(cat.getImageUrl());
        r.setSupportsStattrak(cat.getSupportsStattrak());

        MarketStats normal = calcularStats(publicaciones, false);
        r.setStock(normal.stock());
        r.setPrecioPromedio(normal.promedio());
        r.setPrecioMinimo(normal.minimo());
        r.setPrecioMaximo(normal.maximo());

        MarketStats stattrak = calcularStats(publicaciones, true);
        r.setStockStattrak(stattrak.stock());
        r.setPrecioPromedioStattrak(stattrak.promedio());
        r.setPrecioMinimoStattrak(stattrak.minimo());
        r.setPrecioMaximoStattrak(stattrak.maximo());
        return r;
    }

    private MarketStats calcularStats(List<Skin> publicaciones, boolean stattrak) {
        List<Double> precios = publicaciones.stream()
                .filter(s -> Boolean.TRUE.equals(s.getStattrak()) == stattrak)
                .map(Skin::getFinalPrice)
                .toList();

        if (precios.isEmpty()) {
            return new MarketStats(0, null, null, null);
        }

        double sum = 0.0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (Double precio : precios) {
            sum += precio;
            min = Math.min(min, precio);
            max = Math.max(max, precio);
        }
        return new MarketStats(precios.size(), sum / precios.size(), min, max);
    }

    private record MarketStats(Integer stock, Double promedio, Double minimo, Double maximo) {
    }
}
