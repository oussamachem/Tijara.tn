package com.smartboutique.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Active l'ordonnancement des taches (@Scheduled) : ex. expiration quotidienne des reservations. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
