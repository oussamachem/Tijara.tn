package com.smartboutique.controller;

import com.smartboutique.dto.DashboardResponse;
import com.smartboutique.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tableau de bord (reserve a l'ADMIN — restriction definie dans SecurityConfig).
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse dashboard() {
        return dashboardService.getDashboard();
    }
}
