package cn.seecoder.campushelp.dto;

import java.time.LocalDate;

public class DailyCheckinResponse {

    private int pointsAwarded;
    private int streak;
    private LocalDate checkinDate;

    public DailyCheckinResponse(int pointsAwarded, int streak, LocalDate checkinDate) {
        this.pointsAwarded = pointsAwarded;
        this.streak = streak;
        this.checkinDate = checkinDate;
    }

    public int getPointsAwarded() { return pointsAwarded; }
    public int getStreak() { return streak; }
    public LocalDate getCheckinDate() { return checkinDate; }
}
