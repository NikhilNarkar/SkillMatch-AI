package com.skillmatch.dto.request;

import com.skillmatch.entity.CandidateProfile.Availability;
import com.skillmatch.entity.CandidateProfile.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CandidateProfileRequest {

    @NotNull
    private LocalDate dateOfBirth;

    @NotNull
    private Gender gender;

    private String address;

    private String city;

    private String state;

    private String country;

    private String postalCode;

    @Min(0)
    private int experienceYears;

    @PositiveOrZero
    private Double currentSalary;

    @PositiveOrZero
    private Double expectedSalary;

    private String bio;

    @Pattern(regexp = "^(https?://)?.*$", message = "Invalid LinkedIn URL")
    private String linkedinUrl;

    @Pattern(regexp = "^(https?://)?.*$", message = "Invalid GitHub URL")
    private String githubUrl;

    @Pattern(regexp = "^(https?://)?.*$", message = "Invalid Portfolio URL")
    private String portfolioUrl;

    @NotNull
    private Availability availability;
}
