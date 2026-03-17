package com.skillmatch.controller;

import com.skillmatch.dto.request.RecordTestRequest;
import com.skillmatch.dto.response.DashboardStatsResponse;
import com.skillmatch.entity.User;
import com.skillmatch.repository.UserRepository;
import com.skillmatch.service.StatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class StatsController {

    private final UserRepository userRepository;
    private final StatsService statsService;

    @GetMapping("/me")
    public ResponseEntity<DashboardStatsResponse> me(Authentication authentication) {
        User me = loadUser(authentication);
        return ResponseEntity.ok(statsService.getMyStatsAndRank(me));
    }

    @PostMapping("/record-test")
    public ResponseEntity<DashboardStatsResponse> recordTest(@Valid @RequestBody RecordTestRequest req,
                                                             Authentication authentication) {
        User me = loadUser(authentication);
        if (me == null) {
            return ResponseEntity.status(401).build();
        }
        statsService.recordTest(me, req.getScorePercent());
        // Return updated stats for instant UI update
        return ResponseEntity.ok(statsService.getMyStatsAndRank(me));
    }

    private User loadUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) return null;
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }
}

