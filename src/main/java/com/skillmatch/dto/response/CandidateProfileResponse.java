package com.skillmatch.dto.response;

import com.skillmatch.entity.CandidateProfile.Availability;
import com.skillmatch.entity.CandidateProfile.Gender;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CandidateProfileResponse {

    private Long profileId;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String address;

    private String city;

    private String state;

    private String country;

    private String postalCode;

    private int experienceYears;

    private Double currentSalary;

    private Double expectedSalary;

    private String bio;

    private String linkedinUrl;

    private String githubUrl;

    private String portfolioUrl;

    private Availability availability;

}
