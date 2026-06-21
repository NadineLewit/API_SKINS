package skinsmarket.demo.controller.inventario;

import lombok.Data;
import skinsmarket.demo.entity.InventarioItem;

import java.time.LocalDateTime;

@Data
public class InventarioItemResponse {

    private Long id;
    private String assetId;
    private String classId;
    private String instanceId;
    private String marketHashName;
    private String name;
    private String iconUrl;
    private String type;
    private Boolean tradable;
    private Boolean marketable;
    private InventarioCatalogoResponse catalogo;
    private Boolean publicado;
    private LocalDateTime fechaSync;
    private String inventoryStatus;
    private Long pendingOrderId;
    private Long pendingSkinId;
    private LocalDateTime pendingUntil;
    private LocalDateTime deliveredAt;
    private boolean pending;
    private long secondsUntilUnlock;
    private Double estimatedPrice;

    public static InventarioItemResponse from(InventarioItem item) {
        return from(item, null);
    }

    public static InventarioItemResponse from(InventarioItem item, Double estimatedPrice) {
        InventarioItemResponse response = new InventarioItemResponse();
        response.setId(item.getId());
        response.setAssetId(item.getAssetId());
        response.setClassId(item.getClassId());
        response.setInstanceId(item.getInstanceId());
        response.setMarketHashName(item.getMarketHashName());
        response.setName(item.getName());
        response.setIconUrl(item.getIconUrl());
        response.setType(item.getType());
        response.setTradable(item.getTradable());
        response.setMarketable(item.getMarketable());
        response.setCatalogo(InventarioCatalogoResponse.from(item.getCatalogo()));
        response.setPublicado(item.getPublicado());
        response.setFechaSync(item.getFechaSync());
        response.setInventoryStatus(item.getInventoryStatus());
        response.setPendingOrderId(item.getPendingOrderId());
        response.setPendingSkinId(item.getPendingSkinId());
        response.setPendingUntil(item.getPendingUntil());
        response.setDeliveredAt(item.getDeliveredAt());
        response.setPending(item.isPending());
        response.setSecondsUntilUnlock(item.getSecondsUntilUnlock());
        response.setEstimatedPrice(estimatedPrice);
        return response;
    }
}
