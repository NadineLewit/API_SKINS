package skinsmarket.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import skinsmarket.demo.entity.InventarioItem;
import skinsmarket.demo.entity.Order;
import skinsmarket.demo.entity.Skin;
import skinsmarket.demo.entity.User;
import skinsmarket.demo.repository.InventarioItemRepository;

import java.time.LocalDateTime;

/**
 * Helper para crear InventarioItems de compra de forma idempotente.
 *
 * Participa de la transacción del pago. La orden se bloquea antes de invocar
 * este helper, por lo que dos confirmaciones no pueden crear el mismo item.
 * Mantener una transacción REQUIRES_NEW aquí genera un lock wait: el insert
 * necesita validar las claves foráneas de la orden que la transacción padre
 * todavía mantiene bloqueada.
 */
@Component
public class InventarioItemCreadorHelper {

    @Autowired
    private InventarioItemRepository inventarioItemRepository;

    /**
     * Crea el InventarioItem de la compra si aún no existe.
     * Si ya existe (otro hilo o request lo creó antes), no hace nada.
     *
     * Corre dentro de la transacción del llamador para que el pago y el item de
     * inventario se confirmen de forma atómica.
     */
    @Transactional
    public void crearSiNoExiste(User comprador, Order order, Skin skin, String assetId) {
        boolean yaExiste = inventarioItemRepository
                .findByUserAndPendingOrderIdAndPendingSkinId(comprador, order.getId(), skin.getId())
                .isPresent();

        if (yaExiste) return;

        InventarioItem item = InventarioItem.builder()
                .user(comprador)
                .assetId(assetId)
                .publicado(false)
                .build();
        item.setName(skin.getName());
        item.setMarketHashName(skin.getName());
        item.setIconUrl(skin.getImageUrl());
        item.setType(skin.getCatalogo() != null ? skin.getCatalogo().getCategoryName() : null);
        item.setTradable(false);
        item.setMarketable(false);
        item.setCatalogo(skin.getCatalogo());
        item.setFechaSync(LocalDateTime.now());
        item.setInventoryStatus(InventarioItem.STATUS_PENDING_DELIVERY);
        item.setPendingOrderId(order.getId());
        item.setPendingSkinId(skin.getId());
        item.setPendingUntil(skin.getLockedUntil());

        inventarioItemRepository.save(item);
    }
}
