package com.smartboutique.support;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Contexte de securite de test pour le modele Phase A : un user REEL (charge par email) avec une
 * autorite de BOUTIQUE contextuelle (ROLE_SHOP_OWNER / ROLE_SHOP_VENDOR) — l'equivalent d'une
 * requete authentifiee portant un X-Shop-Id valide. N'affaiblit rien : le principal est le vrai
 * compte (avec son id), et l'autorite est exactement celle que poserait le filtre X-Shop-Id.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithShopMemberSecurityContextFactory.class)
public @interface WithShopMember {

    /** Email du compte reel a incarner (doit exister : seede par DataSeeder). */
    String email() default "admin@smartboutique.com";

    /** Role contextuel dans la boutique active : OWNER | VENDOR. */
    String role() default "OWNER";

    /** Ajoute aussi l'autorite plateforme (ROLE_PLATFORM_ADMIN). */
    boolean platformAdmin() default false;
}
