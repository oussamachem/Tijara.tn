package com.smartboutique.tenancy;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Place l'advisor transactionnel a l'EXTERIEUR (order le plus prioritaire) pour que
 * {@link TenantSessionAspect} (le plus interne) s'execute une fois la transaction deja ouverte
 * et pose {@code SET LOCAL app.current_boutique} dessus. Remplace l'activation par defaut de
 * Spring Boot (qui met l'advisor a LOWEST_PRECEDENCE).
 */
@Configuration
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
public class TenancyConfig {
}
