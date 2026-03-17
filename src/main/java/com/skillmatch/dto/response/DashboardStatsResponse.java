package com.skillmatch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardStatsResponse {
    private int totalTests;
    private double avgScorePercent;
    private int loginStreakDays;
    private int rank;
}

