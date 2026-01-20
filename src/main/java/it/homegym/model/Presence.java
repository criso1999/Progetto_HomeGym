package it.homegym.model;

import java.util.Date;

public class Presence {
    private int id;
    private Integer sessionId;
    private Integer userId;
    private String token;
    private boolean used;
    private Integer scannedBy;
    private Date checkinAt;
    private Date expiresAt;
    private Date createdAt;

    // getters / setters ...
    public int getId() { return id; }
    public void setId(int id) { this.id = id; } 
    public Integer getSessionId() { return sessionId; }
    public void setSessionId(Integer sessionId) { this.sessionId = sessionId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
    public Integer getScannedBy() { return scannedBy; }
    public void setScannedBy(Integer scannedBy) { this.scannedBy = scannedBy; }
    public Date getCheckinAt() { return checkinAt; }
    public void setCheckinAt(Date checkinAt) { this.checkinAt = checkinAt; }
    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
}
