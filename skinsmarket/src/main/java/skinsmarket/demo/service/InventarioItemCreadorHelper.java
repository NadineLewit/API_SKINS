package skinsmarket.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
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
 * Usa REQUIRES_NEW para que la creación corra en su propia transacción.
 * Si dos requests llegan al mismo tiempo e intentan insertar el mismo item
 * (unique constraint: user_id + asset_id), uno de los dos va a fallar con
 * DataIntegrityViolationException. Con REQUIRES_NEW, ese fallo solo afecta
 * la sub-transacción interna; la transacción principal (que guarda el
 * paymentStatus = "PAID") no se ve afectada y puede commitear normalmente.
 */
@Component
public class InventarioItemCreadorHelper {

    @Autowired
    private InventarioItemRepository inventarioItemRepository;

    /**
     * Crea el InventarioItem de la compra si aún no existe.
     * Si ya existe (otro hilo o request lo creó antes), no hace nada.
     *
     * Corre en su PROPIA transacción (REQUIRES_NEW) para que un eventual
     * DataIntegrityViolationException no contamine la transacción del llamador.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
