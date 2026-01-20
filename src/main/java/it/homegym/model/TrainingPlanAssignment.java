package it.homegym.model;

import java.util.Date;

public class TrainingPlanAssignment {
    private int id;
    private int planId;
    private int userId;
    private int trainerId;
    private Date assignedAt;
    private boolean active;
    private String notes;
    private Integer paymentId;
    // getters/setters
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public int getPlanId() {
        return planId;
    }
    public void setPlanId(int planId) {
        this.planId = planId;
    }
    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }
    public int getTrainerId() {
        return trainerId;
    }
    public void setTrainerId(int trainerId) {
        this.trainerId = trainerId;
    }
    public Date getAssignedAt() {
        return assignedAt;
    }
    public void setAssignedAt(Date assignedAt) {
        this.assignedAt = assignedAt;
    }
    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }
    public Integer getPaymentId() {
        return paymentId;
    }
    public void setPaymentId(Integer paymentId) {
        this.paymentId = paymentId;
    }

}
