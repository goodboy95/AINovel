package com.ainovel.app.admin.dto;

public record AdminDashboardStatsResponse(
        long totalUsers,
        long todayNewUsers,
        double totalCreditsConsumed,
        double todayCreditsConsumed,
        double apiErrorRate,
        long pendingReviews
) {}

