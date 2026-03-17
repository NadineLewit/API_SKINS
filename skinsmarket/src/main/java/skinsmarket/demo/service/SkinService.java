package skinsmarket.demo.service;

import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.exception.SkinNoDisponibleException;
import skinsmarket.demo.repository.SkinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SkinService {

    private final SkinRepository skinRepository;

    public List<Skin> listarActivas() {
        return skinRepository.findByActivaTrue();
    }

    public Skin obtenerPorId(Long id) {
        return skinRepository.findById(id)
                .orElseThrow(() -> new SkinNoDisponibleException());
    }

    public Skin crear(Skin skin) {
        if (skin.getPrecio() == null || skin.getPrecio().doubleValue() <= 0)
            throw new RuntimeException("El precio debe ser mayor a 0");
        if (skin.getStock() == null || skin.getStock() < 0)
            throw new RuntimeException("El stock no puede ser negativo");
        if (skin.getNombre() == null || skin.getNombre().isBlank())
            throw new RuntimeException("El nombre no puede estar vacío");
        return skinRepository.save(skin);
    }

    public Skin actualizar(Long id, Skin datos) {
        if (datos.getPrecio() != null && datos.getPrecio().doubleValue() <= 0)
            throw new RuntimeException("El precio debe ser mayor a 0");
        if (datos.getStock() != null && datos.getStock() < 0)
            throw new RuntimeException("El stock no puede ser negativo");
        Skin skin = obtenerPorId(id);
        skin.setNombre(datos.getNombre());
        skin.setDescripcion(datos.getDescripcion());
        skin.setPrecio(datos.getPrecio());
        skin.setStock(datos.getStock());
        skin.setImagenUrl(datos.getImagenUrl());
        skin.setCategoria(datos.getCategoria());
        skin.setRareza(datos.getRareza());
        skin.setExterior(datos.getExterior());
        skin.setColeccion(datos.getColeccion());
        skin.setStattrak(datos.getStattrak());
        skin.setDescuento(datos.getDescuento());
        return skinRepository.save(skin);
    }

    public void desactivar(Long id) {
        Skin skin = obtenerPorId(id);
        skin.setActiva(false);
        skinRepository.save(skin);
    }
}