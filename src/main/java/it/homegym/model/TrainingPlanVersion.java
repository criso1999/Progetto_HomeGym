package it.homegym.model;

import java.util.Date;

public class TrainingPlanVersion {
    private int id;
    private int planId;
    private int versionNumber;
    private String title;
    private String description;
    private String content;
    private Integer createdBy;
    private Date createdAt;
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
    public int getVersionNumber() {
        return versionNumber;
    }
    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public Integer getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }
    public Date getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
}
