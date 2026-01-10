package it.homegym.model;

import java.sql.Timestamp;

public class TrainingSession {
    private int id;
    private Integer userId;    // nullable
    private String userName;
    private String trainer;
    private Timestamp when;
    private int durationMinutes;
    private String notes;

    public TrainingSession() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getTrainer() { return trainer; }
    public void setTrainer(String trainer) { this.trainer = trainer; }

    public Timestamp getWhen() { return when; }
    public void setWhen(Timestamp when) { this.when = when; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
