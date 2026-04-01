package skinsmarket.demo.service;

import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.Usuario;
import skinsmarket.demo.exception.SkinNoDisponibleException;
import skinsmarket.demo.repository.SkinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkinService implements ISkinService {

    private final SkinRepository skinRepository;
    private final UsuarioService usuarioService;

    public List<Skin> listarActivas() {
        return skinRepository.findByActivaTrue();
    }

    public List<Skin> listarConFiltros(String nombre, Skin.Rareza rareza, Skin.Exterior exterior,
                                       BigDecimal precioMin, BigDecimal precioMax, Long categoriaId) {
        return skinRepository.findByActivaTrue().stream()
                .filter(s -> nombre == null || s.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                .filter(s -> rareza == null || rareza.equals(s.getRareza()))
                .filter(s -> exterior == null || exterior.equals(s.getExterior()))
                .filter(s -> precioMin == null || s.getPrecio().compareTo(precioMin) >= 0)
                .filter(s -> precioMax == null || s.getPrecio().compareTo(precioMax) <= 0)
                .filter(s -> categoriaId == null || (s.getCategoria() != null && categoriaId.equals(s.getCategoria().getId())))
                .collect(Collectors.toList());
    }

    public Skin obtenerPorId(Long id) {
        return skinRepository.findById(id)
                .orElseThrow(() -> new SkinNoDisponibleException());
    }

    public Skin crear(Skin skin, String username) {
        Usuario vendedor = usuarioService.obtenerPorUsername(username);
        skin.setVendedor(vendedor);
        return skinRepository.save(skin);
    }

    public Skin actualizar(Long id, Skin datos, String username) {
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