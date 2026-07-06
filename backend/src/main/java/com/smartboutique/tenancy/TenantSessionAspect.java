package com.smartboutique.tenancy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Defense en profondeur : pose la variable de session PostgreSQL {@code app.current_boutique} au
 * debut de chaque transaction, a partir du {@link TenantContext} de la requete. La RLS s'appuie
 * dessus pour filtrer/verrouiller chaque table tenant.
 *
 * <p>{@code SET LOCAL} est scope a la transaction : auto-reinitialise a la fin (sur avec le pool de
 * connexions). L'aspect est le PLUS INTERNE (Order LOWEST) et {@link com.smartboutique.tenancy
 * .TenancyConfig} met l'advisor transactionnel a l'exterieur (order 0) -> quand cet aspect
 * s'execute, la transaction est deja ouverte et le SET LOCAL s'y applique.</p>
 *
 * <p>Si aucun tenant (contexte plateforme/SUPER_ADMIN, ou tache hors requete), on ne pose rien :
 * la policy RLS voit alors NULL -> aucune ligne (fail-closed).</p>
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class TenantSessionAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Around("@within(org.springframework.transaction.annotation.Transactional) "
            + "|| @annotation(org.springframework.transaction.annotation.Transactional)")
    public Object bindTenant(ProceedingJoinPoint pjp) throws Throwable {
        Long boutiqueId = TenantContext.get();
        if (boutiqueId != null && TransactionSynchronizationManager.isActualTransactionActive()) {
            // SET LOCAL n'accepte pas de parametre lie ; boutiqueId est un Long controle serveur.
            entityManager.unwrap(Session.class).doWork(conn -> {
                try (var st = conn.createStatement()) {
                    st.execute("SET LOCAL app.current_boutique = " + boutiqueId);
                }
            });
        }
        return pjp.proceed();
    }
}
