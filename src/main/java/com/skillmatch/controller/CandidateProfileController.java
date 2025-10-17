package com.skillmatch.controller;

import com.skillmatch.dto.request.CandidateProfileRequest;
import com.skillmatch.dto.response.CandidateProfileResponse;
import com.skillmatch.service.CandidateProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/candidate/profile")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateProfileService candidateProfileService;

    @GetMapping
    public ResponseEntity<CandidateProfileResponse> getProfile(Authentication authentication) {
        Long userId = ((com.skillmatch.entity.User) authentication.getPrincipal()).getUserId();
        CandidateProfileResponse response = candidateProfileService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CandidateProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody CandidateProfileRequest request) {
        Long userId = ((com.skillmatch.entity.User) authentication.getPrincipal()).getUserId();
        CandidateProfileResponse response = candidateProfileService.createOrUpdateProfile(userId, request);
        return ResponseEntity.ok(response);
    }
}
