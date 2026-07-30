package com.smartboutique.controller;

import com.smartboutique.dto.NotificationResponse;
import com.smartboutique.security.UserPrincipal;
import com.smartboutique.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Notifications in-app de l'utilisateur connecte. Au grain IDENTITE (route /api/me/**) : accessible
 * a tout user authentifie, y compris un client sans boutique, SANS X-Shop-Id.
 */
@RestController
@RequestMapping("/api/me/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return notificationService.list(principal.getId());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return Map.of("count", notificationService.unreadCount(principal.getId()));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markRead(id, principal.getId());
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        return Map.of("updated", notificationService.markAllRead(principal.getId()));
    }
}
