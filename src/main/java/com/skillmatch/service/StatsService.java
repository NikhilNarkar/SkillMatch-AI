package com.skillmatch.service;

import com.skillmatch.dto.response.DashboardStatsResponse;
import com.skillmatch.entity.User;
import com.skillmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserRepository userRepository;

    @Transactional
    public void updateLoginStreak(User user, LocalDate today) {
        if (user == null) return;
        LocalDate last = user.getLastLoginDate();
        int streak = user.getLoginStreakDays() == null ? 0 : user.getLoginStreakDays();

        if (last == null) {
            streak = 1;
        } else if (last.isEqual(today)) {
            // same-day login: keep streak
        } else if (last.plusDays(1).isEqual(today)) {
            streak = Math.max(1, streak + 1);
        } else {
            streak = 1;
        }

        user.setLastLoginDate(today);
        user.setLoginStreakDays(streak);
        userRepository.save(user);
    }

    @Transactional
    public void recordTest(User user, double scorePercent) {
        if (user == null) return;
        double score = Math.max(0, Math.min(100, scorePercent));

        int tests = user.getTestsTaken() == null ? 0 : user.getTestsTaken();
        double total = user.getScoreTotalPercent() == null ? 0.0 : user.getScoreTotalPercent();

        tests += 1;
        total += score;

        user.setTestsTaken(tests);
        user.setScoreTotalPercent(total);
        user.setAvgScorePercent(tests == 0 ? 0.0 : (total / tests));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getMyStatsAndRank(User me) {
        if (me == null) {
            return new DashboardStatsResponse(0, 0.0, 0, 0);
        }

        List<User> users = userRepository.findAllActive();
        users.sort(rankComparator());

        int rank = 0;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUserId() != null && users.get(i).getUserId().equals(me.getUserId())) {
                rank = i + 1;
                break;
            }
        }

        int tests = me.getTestsTaken() == null ? 0 : me.getTestsTaken();
        double avg = me.getAvgScorePercent() == null ? 0.0 : me.getAvgScorePercent();
        int streak = me.getLoginStreakDays() == null ? 0 : me.getLoginStreakDays();
        return new DashboardStatsResponse(tests, round1(avg), streak, rank);
    }

    private Comparator<User> rankComparator() {
        return Comparator
                .comparing((User u) -> safeDouble(u.getAvgScorePercent())).reversed()
                .thenComparing((User u) -> safeInt(u.getLoginStreakDays()), Comparator.reverseOrder())
                .thenComparing((User u) -> safeInt(u.getTestsTaken()), Comparator.reverseOrder())
                .thenComparing((User u) -> safeLong(u.getUserId()));
    }

    private static double safeDouble(Double d) { return d == null ? 0.0 : d; }
    private static int safeInt(Integer i) { return i == null ? 0 : i; }
    private static long safeLong(Long l) { return l == null ? Long.MAX_VALUE : l; }
    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}

