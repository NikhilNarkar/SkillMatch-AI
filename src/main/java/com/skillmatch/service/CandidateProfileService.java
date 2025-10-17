package com.skillmatch.service;

import com.skillmatch.dto.request.CandidateProfileRequest;
import com.skillmatch.dto.response.CandidateProfileResponse;
import com.skillmatch.entity.CandidateProfile;
import com.skillmatch.entity.User;
import com.skillmatch.repository.CandidateProfileRepository;
import com.skillmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CandidateProfileService {

    private final CandidateProfileRepository profileRepository;
    private final UserRepository userRepository;

    public CandidateProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        CandidateProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        return mapToResponse(profile);
    }

    public CandidateProfileResponse createOrUpdateProfile(Long userId, CandidateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CandidateProfile profile = profileRepository.findByUser(user)
                .orElse(new CandidateProfile());

        profile.setUser(user);
        profile.setDateOfBirth(request.getDateOfBirth().atStartOfDay());
        profile.setGender(request.getGender());
        profile.setAddress(request.getAddress());
        profile.setCity(request.getCity());
        profile.setState(request.getState());
        profile.setCountry(request.getCountry());
        profile.setPostalCode(request.getPostalCode());
        profile.setExperienceYears(request.getExperienceYears());
        profile.setCurrentSalary(request.getCurrentSalary());
        profile.setExpectedSalary(request.getExpectedSalary());
        profile.setBio(request.getBio());
        profile.setLinkedinUrl(request.getLinkedinUrl());
        profile.setGithubUrl(request.getGithubUrl());
        profile.setPortfolioUrl(request.getPortfolioUrl());
        profile.setAvailability(request.getAvailability());

        CandidateProfile savedProfile = profileRepository.save(profile);
        return mapToResponse(savedProfile);
    }

    private CandidateProfileResponse mapToResponse(CandidateProfile profile) {
        CandidateProfileResponse response = new CandidateProfileResponse();
        response.setProfileId(profile.getProfileId());
        response.setDateOfBirth(profile.getDateOfBirth().toLocalDate());
        response.setGender(profile.getGender());
        response.setAddress(profile.getAddress());
        response.setCity(profile.getCity());
        response.setState(profile.getState());
        response.setCountry(profile.getCountry());
        response.setPostalCode(profile.getPostalCode());
        response.setExperienceYears(profile.getExperienceYears());
        response.setCurrentSalary(profile.getCurrentSalary());
        response.setExpectedSalary(profile.getExpectedSalary());
        response.setBio(profile.getBio());
        response.setLinkedinUrl(profile.getLinkedinUrl());
        response.setGithubUrl(profile.getGithubUrl());
        response.setPortfolioUrl(profile.getPortfolioUrl());
        response.setAvailability(profile.getAvailability());
        return response;
    }
}
