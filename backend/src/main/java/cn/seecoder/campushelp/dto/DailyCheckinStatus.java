package cn.seecoder.campushelp.dto;

import java.time.LocalDate;

public class DailyCheckinStatus {

    private boolean checkedIn;
    private int currentStreak;
    private LocalDate lastCheckinDate;

    public DailyCheckinStatus(boolean checkedIn, int currentStreak, LocalDate lastCheckinDate) {
        this.checkedIn = checkedIn;
        this.currentStreak = currentStreak;
        this.lastCheckinDate = lastCheckinDate;
    }

    public boolean isCheckedIn() { return checkedIn; }
    public int getCurrentStreak() { return currentStreak; }
    public LocalDate getLastCheckinDate() { return lastCheckinDate; }
}
