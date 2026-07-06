package com.smartboutique.tenancy;

/**
 * Contexte tenant de la requete courante (ThreadLocal). Rempli par le filtre JWT a partir de la
 * boutique de l'utilisateur authentifie, lu par {@link TenantSessionAspect} pour poser la variable
 * de session PostgreSQL {@code app.current_boutique} (base de la RLS). TOUJOURS nettoye en fin de
 * requete pour eviter toute fuite entre requetes reutilisant le meme thread du pool.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long boutiqueId) {
        CURRENT.set(boutiqueId);
    }

    public static Long get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
