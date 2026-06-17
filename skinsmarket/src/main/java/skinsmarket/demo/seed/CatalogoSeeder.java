package skinsmarket.demo.seed;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import skinsmarket.demo.repository.SkinCatalogoRepository;
import skinsmarket.demo.service.SkinCatalogoService;

/**
 * Seed automático del catálogo al arrancar la aplicación.
 *
 * COMPORTAMIENTO:
 *   - Si la tabla skin_catalogo está VACÍA → sincroniza TODAS las skins
 *     desde la API de ByMykel/CSGO-API (~22.000 entries).
 *   - Si ya tiene datos → no hace nada (no re-sincroniza para no sobrecargar).
 *
 * ⚠️ ATENCIÓN: el primer arranque va a tardar 2-3 minutos porque trae todo
 * el catálogo de skins de CS2. Esto es solo en el PRIMER arranque (cuando
 * la BD está vacía). Los arranques siguientes son instantáneos.
 *
 * Para resincronizar manualmente después, usar:
 *   POST /catalogo/sincronizar           ← todas
 *   POST /catalogo/sincronizar?limit=N   ← limitado
 */
@Component
@Order(1)
public class CatalogoSeeder implements CommandLineRunner {

    @Autowired
    private SkinCatalogoRepository skinCatalogoRepository;

    @Autowired
    private SkinCatalogoService skinCatalogoService;

    @Override
    public void run(String... args) {
        long total = skinCatalogoRepository.count();

        if (total > 0) {
            System.out.println("[CatalogoSeeder] El catálogo ya tiene " + total +
                    " skins. Skip del seed inicial.");
            return;
        }

        System.out.println("[CatalogoSeeder] Catálogo vacío. Iniciando sync COMPLETO " +
                "desde la API de ByMykel/CSGO-API...");
        System.out.println("[CatalogoSeeder] ⚠️ Esto puede tardar 2-3 minutos. " +
                "Es solo la primera vez.");

        long inicio = System.currentTimeMillis();
        try {
            // -1 = sin límite, sincronizamos todas
            int insertadas = skinCatalogoService.sincronizarDesdeApi(-1);
            long duracion = (System.currentTimeMillis() - inicio) / 1000;
            System.out.println("[CatalogoSeeder] ✓ Seed completado en " + duracion +
                    " segundos. Skins insertadas: " + insertadas);
        } catch (Exception e) {
            System.err.println("[CatalogoSeeder] ✗ Error en el seed inicial: " +
                    e.getMessage());
            System.err.println("[CatalogoSeeder] La app arranca igual. Podés " +
                    "sincronizar manualmente con POST /catalogo/sincronizar como ADMIN.");
        }
    }
}
