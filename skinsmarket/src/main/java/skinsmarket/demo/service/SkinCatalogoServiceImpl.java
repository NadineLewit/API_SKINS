package skinsmarket.demo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import skinsmarket.demo.controller.skincatalogo.SkinCatalogoRequest;
import skinsmarket.demo.entity.SkinCatalogo;
import skinsmarket.demo.repository.SkinCatalogoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del servicio de catálogo maestro de skins.
 *
 * Cliente de la API pública de ByMykel/CSGO-API:
 *   https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/api/en/skins.json
 *
 * Sin API key. La API tiene ~22.000 skins de CS2 con metadata completa
 * (nombre, descripción, imagen, rareza, exterior soportado, etc.).
 *
 * IMPORTANTE: usamos raw.githubusercontent.com (no bymykel.github.io) porque
 * GitHub Pages sirve el JSON con Content-Type incorrecto (text/html), lo que
 * rompe la deserialización automática de Spring.
 */
@Service
public class SkinCatalogoServiceImpl implements SkinCatalogoService {

    private static final String API_URL =
            "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/api/en/skins.json";

    @Autowired
    private SkinCatalogoRepository skinCatalogoRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    @Transactional
    public int sincronizarDesdeApi(int limit) {
        // 1. Pedimos como String porque GitHub Raw devuelve text/plain
        String json;
        try {
            json = restTemplate.getForObject(API_URL, String.class);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error al consultar la API de ByMykel: " + e.getMessage());
        }

        if (json == null || json.isBlank()) {
            throw new RuntimeException("La API de ByMykel devolvió respuesta vacía");
        }

        // 2. Parseamos manualmente con ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        List<ByMykelSkin> skins;
        try {
            skins = mapper.readValue(json,
                    mapper.getTypeFactory().constructCollectionType(List.class, ByMykelSkin.class));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error al parsear respuesta de ByMykel: " + e.getMessage());
        }

        // 3. Determinar cuántas procesar (limit ≤ 0 → todas)
        boolean sinLimite = limit <= 0;
        int total = sinLimite ? skins.size() : Math.min(limit, skins.size());

        System.out.println("[SkinCatalogoService] Sincronizando " + total +
                " skins (de " + skins.size() + " disponibles en la API)...");

        // 4. Upsert por externalId
        int insertadas = 0;
        int actualizadas = 0;
        int errores = 0;

        for (int i = 0; i < total; i++) {
            ByMykelSkin s = skins.get(i);
            try {
                if (s.getId() == null || s.getName() == null) {
                    errores++;
                    continue;
                }

                Optional<SkinCatalogo> existente =
                        skinCatalogoRepository.findByExternalId(s.getId());

                SkinCatalogo cat;
                boolean esNueva = false;
                if (existente.isPresent()) {
                    cat = existente.get();
                } else {
                    cat = new SkinCatalogo();
                    cat.setExternalId(s.getId());
                    esNueva = true;
                }

                cat.setName(s.getName());
                cat.setDescription(s.getDescription());
                cat.setImageUrl(s.getImage());
                cat.setMinFloat(s.getMin_float());
                cat.setMaxFloat(s.getMax_float());
                cat.setSupportsStattrak(Boolean.TRUE.equals(s.getStattrak()));
                cat.setSupportsSouvenir(Boolean.TRUE.equals(s.getSouvenir()));

                if (s.getWeapon() != null && s.getWeapon().getName() != null) {
                    cat.setWeaponName(s.getWeapon().getName());
                }
                if (s.getCategory() != null && s.getCategory().getName() != null) {
                    cat.setCategoryName(s.getCategory().getName());
                }
                if (s.getRarity() != null) {
                    cat.setRarezaName(s.getRarity().getName());
                    cat.setRarezaColor(s.getRarity().getColor());
                }

                skinCatalogoRepository.save(cat);
                if (esNueva) insertadas++;
                else actualizadas++;

                if ((i + 1) % 1000 == 0) {
                    System.out.println("[SkinCatalogoService] Procesadas " +
                            (i + 1) + "/" + total + "...");
                }
            } catch (Exception e) {
                errores++;
            }
        }

        System.out.println("[SkinCatalogoService] ✓ Sync terminado. " +
                "Insertadas: " + insertadas +
                " | Actualizadas: " + actualizadas +
                " | Errores: " + errores);

        return insertadas;
    }

    @Override
    public List<SkinCatalogo> listarTodos() {
        return skinCatalogoRepository.findAll();
    }

    @Override
    public SkinCatalogo obtenerPorId(Long id) {
        return skinCatalogoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Item del catálogo no encontrado: " + id));
    }

    @Override
    public List<SkinCatalogo> buscarPorNombre(String nombre) {
        return skinCatalogoRepository.findByNameContainingIgnoreCase(nombre);
    }

    @Override
    public List<SkinCatalogo> filtrar(String arma, String categoria) {
        boolean conArma = arma != null && !arma.isBlank();
        boolean conCategoria = categoria != null && !categoria.isBlank();

        if (conArma && conCategoria) {
            return skinCatalogoRepository
                    .findByWeaponNameContainingIgnoreCaseAndCategoryNameContainingIgnoreCase(
                            arma, categoria);
        }
        if (conArma) {
            return skinCatalogoRepository.findByWeaponNameContainingIgnoreCase(arma);
        }
        if (conCategoria) {
            return skinCatalogoRepository.findByCategoryNameContainingIgnoreCase(categoria);
        }
        return new ArrayList<>();
    }

    @Override
    @Transactional
    public SkinCatalogo crear(SkinCatalogoRequest req) {
        if (req.getExternalId() == null || req.getName() == null) {
            throw new RuntimeException("externalId y name son obligatorios");
        }
        skinCatalogoRepository.findByExternalId(req.getExternalId()).ifPresent(c -> {
            throw new RuntimeException(
                    "Ya existe un item con externalId: " + req.getExternalId());
        });

        SkinCatalogo cat = new SkinCatalogo();
        cat.setExternalId(req.getExternalId());
        cat.setName(req.getName());
        cat.setDescription(req.getDescription());
        cat.setImageUrl(req.getImageUrl());
        cat.setWeaponName(req.getWeaponName());
        cat.setCategoryName(req.getCategoryName());
        cat.setRarezaName(req.getRarezaName());
        cat.setRarezaColor(req.getRarezaColor());
        cat.setMinFloat(req.getMinFloat());
        cat.setMaxFloat(req.getMaxFloat());
        cat.setSupportsStattrak(Boolean.TRUE.equals(req.getSupportsStattrak()));
        cat.setSupportsSouvenir(Boolean.TRUE.equals(req.getSupportsSouvenir()));

        return skinCatalogoRepository.save(cat);
    }

    @Override
    @Transactional
    public boolean eliminar(Long id) {
        if (!skinCatalogoRepository.existsById(id)) return false;
        skinCatalogoRepository.deleteById(id);
        return true;
    }

    // =========================================================================
    // DTOs internos para deserializar la respuesta de ByMykel
    // =========================================================================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ByMykelSkin {
        private String id;
        private String name;
        private String description;
        private String image;
        private Double min_float;
        private Double max_float;
        private Boolean stattrak;
        private Boolean souvenir;
        private ByMykelNamed weapon;
        private ByMykelNamed category;
        private ByMykelRarity rarity;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ByMykelNamed {
        private String id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ByMykelRarity {
        private String id;
        private String name;
        private String color;
    }
}
