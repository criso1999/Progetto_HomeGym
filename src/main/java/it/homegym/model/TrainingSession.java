package it.homegym.model;

import java.util.Date;

public class TrainingSession {

    private int id;
    private Integer userId;
    private String userName; // optional: populated via JOIN
    private String trainer;
    private Date when; // scheduled_at (java.util.Date)
    private Integer durationMinutes; // nullable
    private String notes;
    private Date createdAt;

    public TrainingSession() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getTrainer() { return trainer; }
    public void setTrainer(String trainer) { this.trainer = trainer; }

    public Date getWhen() { return when; }
    public void setWhen(Date when) { this.when = when; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "TrainingSession{" +
                "id=" + id +
                ", userId=" + userId +
                ", userName='" + userName + '\'' +
                ", trainer='" + trainer + '\'' +
                ", when=" + when +
                ", durationMinutes=" + durationMinutes +
                ", notes='" + notes + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
