package skinsmarket.demo.service;

import skinsmarket.demo.controller.inventario.PublicarDesdeInventarioRequest;
import skinsmarket.demo.entity.InventarioItem;
import skinsmarket.demo.entity.Skin;

import java.util.List;

/**
 * Servicio para gestionar el inventario de Steam de cada usuario.
 *
 * El inventario se obtiene desde la API pública de steamcommunity.com,
 * sin necesidad de API key. El usuario debe tener su inventario público
 * en la configuración de Steam para que pueda ser leído.
 */
public interface InventarioService {

    /**
     * Sincroniza el inventario del usuario contra la API pública de Steam.
     *
     * @param email email del user autenticado (del JWT)
     * @return cantidad de items finales en el inventario tras el sync
     */
    int sincronizar(String email);

    /**
     * Variante "aislada" del sincronizar — corre en una transacción NUEVA
     * (REQUIRES_NEW). Se usa cuando se llama desde dentro de otra transacción
     * que no debe abortarse si el sync falla.
     *
     * Caso de uso: UserServiceImpl.actualizarUser() la llama después de cambiar
     * el SteamID. Si Steam falla (rate limit, inventario privado, etc.), el
     * rollback queda contenido en esta transacción y no contamina la de afuera
     * que actualiza el perfil.
     */
    int sincronizarAislado(String email);

    /** Lista el inventario completo de un usuario. */
    List<InventarioItem> listarInventario(String email);

    /**
     * Publica un item del inventario como Skin en venta.
     * Marca el InventarioItem como publicado=true.
     */
    Skin publicarDesdeInventario(String email, Long inventarioItemId,
                                 PublicarDesdeInventarioRequest request);
}
