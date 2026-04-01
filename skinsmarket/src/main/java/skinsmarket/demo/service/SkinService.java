package skinsmarket.demo.service;

import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.Usuario;
import skinsmarket.demo.exception.SkinNoDisponibleException;
import skinsmarket.demo.repository.SkinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SkinService implements ISkinService {

    private final SkinRepository skinRepository;
    private final UsuarioService usuarioService;

    public List<Skin> listarActivas() {
        return skinRepository.findByActivaTrue();
    }

    public Skin obtenerPorId(Long id) {
        return skinRepository.findById(id)
                .orElseThrow(() -> new SkinNoDisponibleException());
    }

    public Skin crear(Skin skin, String username) {
        if (skin.getNombre() == null || skin.getNombre().isBlank())
            throw new RuntimeException("El nombre no puede estar vacío");
        if (skin.getPrecio() == null || skin.getPrecio().doubleValue() <= 0)
            throw new RuntimeException("El precio debe ser mayor a 0");
        if (skin.getStock() == null || skin.getStock() < 0)
            throw new RuntimeException("El stock no puede ser negativo");

        Usuario vendedor = usuarioService.obtenerPorUsername(username);
        skin.setVendedor(vendedor);
        return skinRepository.save(skin);
    }

    public Skin actualizar(Long id, Skin datos, String username) {
        if (datos.getPrecio() != null && datos.getPrecio().doubleValue() <= 0)
            throw new RuntimeException("El precio debe ser mayor a 0");
        if (datos.getStock() != null && datos.getStock() < 0)
            throw new RuntimeException("El stock no puede ser negativo");
        Skin skin = obtenerPorId(id);
        if (!skin.getVendedor().getUsername().equals(username))
            throw new RuntimeException("Solo el vendedor puede editar esta skin");
        skin.setNombre(datos.getNombre());
        skin.setDescripcion(datos.getDescripcion());
        skin.setPrecio(datos.getPrecio());
        skin.setStock(datos.getStock());
        skin.setImagenUrl(datos.getImagenUrl());
        skin.setCategoria(datos.getCategoria());
        skin.setRareza(datos.getRareza());
        skin.setExterior(datos.getExterior());
        skin.setStattrak(datos.getStattrak());
        skin.setDescuento(datos.getDescuento());
        return skinRepository.save(skin);
    }

    public void desactivar(Long id, String username) {
        Skin skin = obtenerPorId(id);
        if (!skin.getVendedor().getUsername().equals(username))
            throw new RuntimeException("Solo el vendedor puede desactivar esta skin");
        skin.setActiva(false);
        skinRepository.save(skin);
    }

    public List<Skin> misVentas(String username) {
        Usuario vendedor = usuarioService.obtenerPorUsername(username);
        return skinRepository.findByVendedor(vendedor);
    }

    public List<Skin> misVentasActivas(String username) {
        Usuario vendedor = usuarioService.obtenerPorUsername(username);
        return skinRepository.findByVendedorAndActivaTrue(vendedor);
    }
}