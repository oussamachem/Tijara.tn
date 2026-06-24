package com.smartboutique;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

/**
 * Point d'entree de l'API Smart Boutique.
 */
@SpringBootApplication
public class SmartBoutiqueApplication {

    /**
     * Fuseau metier de la boutique : la frontiere de journee ("ventes du jour", filtres de
     * periode) est calculee en heure de Tunis, independamment du fuseau de la machine/conteneur
     * (souvent UTC). Fixe tot, au demarrage du contexte.
     */
    @PostConstruct
    void initTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Tunis"));
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Tunis"));
        SpringApplication.run(SmartBoutiqueApplication.class, args);
    }
}
