package com.skillmatch.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecordTestRequest {
    @NotNull
    @Min(0)
    @Max(100)
    private Double scorePercent;

    // Optional: "HR", "CODING", "APTITUDE", etc.
    private String category;
}

