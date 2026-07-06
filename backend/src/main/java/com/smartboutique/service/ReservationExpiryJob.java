package com.smartboutique.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job quotidien d'expiration des reservations (B6). Delegue la logique metier
 * (transactionnelle, testable a la main) a {@link ReservationService#expireOverdue()}.
 * Horaire configurable via {@code app.reservations.expiry-cron} (defaut 02:00 Africa/Tunis).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpiryJob {

    private final ReservationService reservationService;

    @Scheduled(cron = "${app.reservations.expiry-cron:0 0 2 * * *}")
    public void run() {
        try {
            int expired = reservationService.expireOverdue();
            if (expired > 0) log.info("Job expiration reservations : {} expiree(s)", expired);
        } catch (Exception e) {
            log.error("Job expiration reservations : echec", e);
        }
    }
}
