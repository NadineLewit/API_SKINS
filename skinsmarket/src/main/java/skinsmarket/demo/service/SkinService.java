package skinsmarket.demo.service;

import skinsmarket.demo.entity.Skin;
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
                .orElseThrow(() -> new RuntimeException("Skin no encontrada"));
    }

    public Skin crear(Skin skin) {
        return skinRepository.save(skin);
    }

    public Skin actualizar(Long id, Skin datos) {
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