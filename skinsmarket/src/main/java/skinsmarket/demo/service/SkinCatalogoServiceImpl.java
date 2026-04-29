package skinsmarket.demo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import skinsmarket.demo.controller.skincatalogo.SkinCatalogoRequest;
import skinsmarket.demo.entity.SkinCatalogo;
import skinsmarket.demo.repository.SkinCatalogoRepository;

import java.util.List;

@Service
public class SkinCatalogoServiceImpl implements SkinCatalogoService {

    /**
     * URL del endpoint de skins de la API pública de ByMykel/CSGO-API.
     *
     * Ojo: hay que usar raw.githubusercontent.com (que sirve el archivo crudo).
     * La URL bymykel.github.io es la landing page del proyecto y devuelve HTML.
     *
     * No requiere API key — es JSON estático servido desde el repo de GitHub.
     * Documentación: https://github.com/ByMykel/CSGO-API
     */
    private static final String STEAM_SKINS_URL =
            "https://raw.githubusercontent.com/ByMykel/CSGO-API/main/public/api/en/skins.json";

    @Autowired
    private SkinCatalogoRepository skinCatalogoRepository;

    @Autowired
    private RestTemplate restTemplate;

    // =========================================================================
    // Sincronización con la API externa
    // =========================================================================

    @Override
    @Transactional
    public int sincronizarDesdeApi(Integer limit) {
        // 1. Traemos la respuesta como String crudo.
        //    Hacemos esto en lugar de pedir directamente List<ApiSkinDto> porque
        //    GitHub sirve el JSON con Content-Type "text/plain", lo que hace
        //    que el RestTemplate no encuentre un MessageConverter de Jackson
        //    automáticamente. Pidiendo String se usa StringHttpMessageConverter
        //    que acepta cualquier text/*, y después parseamos manualmente.
        String json = restTemplate.getForObject(STEAM_SKINS_URL, String.class);

        if (json == null || json.isBlank()) {
            throw new RuntimeException("La API de skins no devolvió datos");
        }

        // 2. Deserializamos el JSON con un ObjectMapper local
        ObjectMapper mapper = new ObjectMapper();
        // Por si la API agrega campos nuevos en el futuro, evitamos que el
        // deserializer falle por propiedades desconocidas.
        mapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);

        List<ApiSkinDto> skinsDeLaApi;
        try {
            skinsDeLaApi = mapper.readValue(json, new TypeReference<List<ApiSkinDto>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear JSON de la API: " + e.getMessage());
        }

        if (skinsDeLaApi == null || skinsDeLaApi.isEmpty()) {
            throw new RuntimeException("La API de skins no devolvió datos");
        }

        // 3. Aplicamos el límite si fue provisto (default: todas las que vienen)
        int max = (limit != null && limit > 0)
                ? Math.min(limit, skinsDeLaApi.size())
                : skinsDeLaApi.size();

        // 4. Recorremos y persistimos solo las que no existen en la BD
        int insertadas = 0;
        for (int i = 0; i < max; i++) {
            ApiSkinDto dto = skinsDeLaApi.get(i);

            // Si ya existe esta skin (matcheada por externalId), la salteamos
            if (dto.getId() == null ||
                    skinCatalogoRepository.findByExternalId(dto.getId()).isPresent()) {
                continue;
            }

            SkinCatalogo entidad = SkinCatalogo.builder()
                    .externalId(dto.getId())
                    .name(dto.getName())
                    .description(dto.getDescription())
                    .weaponName(dto.getWeapon() != null ? dto.getWeapon().getName() : null)
                    .categoryName(dto.getCategory() != null ? dto.getCategory().getName() : null)
                    .rarezaName(dto.getRarity() != null ? dto.getRarity().getName() : null)
                    .rarezaColor(dto.getRarity() != null ? dto.getRarity().getColor() : null)
                    .imageUrl(dto.getImage())
                    .minFloat(dto.getMin_float())
                    .maxFloat(dto.getMax_float())
                    .supportsStattrak(Boolean.TRUE.equals(dto.getStattrak()))
                    .supportsSouvenir(Boolean.TRUE.equals(dto.getSouvenir()))
                    .build();

            skinCatalogoRepository.save(entidad);
            insertadas++;
        }

        return insertadas;
    }

    // =========================================================================
    // CRUD manual del catálogo
    // =========================================================================

    @Override
    @Transactional
    public SkinCatalogo crear(SkinCatalogoRequest request) {
        SkinCatalogo entidad = SkinCatalogo.builder()
                .externalId(request.getExternalId())
                .name(request.getName())
                .description(request.getDescription())
                .weaponName(request.getWeaponName())
                .categoryName(request.getCategoryName())
                .rarezaName(request.getRarezaName())
                .rarezaColor(request.getRarezaColor())
                .imageUrl(request.getImageUrl())
                .minFloat(request.getMinFloat())
                .maxFloat(request.getMaxFloat())
                .supportsStattrak(Boolean.TRUE.equals(request.getSupportsStattrak()))
                .supportsSouvenir(Boolean.TRUE.equals(request.getSupportsSouvenir()))
                .build();
        return skinCatalogoRepository.save(entidad);
    }

    @Override
    public List<SkinCatalogo> listar() {
        return skinCatalogoRepository.findAll();
    }

    @Override
    public SkinCatalogo obtenerPorId(Long id) {
        return skinCatalogoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Catálogo no encontrado con id: " + id));
    }

    @Override
    public List<SkinCatalogo> buscarPorNombre(String nombre) {
        return skinCatalogoRepository.findByNameContainingIgnoreCase(nombre);
    }

    @Override
    public List<SkinCatalogo> filtrarPorArma(String weapon) {
        return skinCatalogoRepository.findByWeaponNameIgnoreCase(weapon);
    }

    @Override
    public List<SkinCatalogo> filtrarPorCategoria(String categoria) {
        return skinCatalogoRepository.findByCategoryNameIgnoreCase(categoria);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!skinCatalogoRepository.existsById(id)) {
            throw new IllegalArgumentException("Catálogo no encontrado con id: " + id);
        }
        skinCatalogoRepository.deleteById(id);
    }

    // =========================================================================
    // DTOs internos para deserializar la respuesta de la API externa
    // Solo se usan dentro de este service (no se exponen al cliente).
    // =========================================================================

    /**
     * Estructura mínima de la respuesta de la API.
     * Solo mapeamos los campos que necesitamos. @JsonIgnoreProperties evita
     * que el deserializer falle cuando vienen campos extra.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiSkinDto {
        private String id;
        private String name;
        private String description;
        private String image;
        private Double min_float;
        private Double max_float;
        private Boolean stattrak;
        private Boolean souvenir;
        private NamedRef weapon;
        private NamedRef category;
        private RarityRef rarity;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NamedRef {
        private String id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RarityRef {
        private String id;
        private String name;
        private String color;
    }
}
