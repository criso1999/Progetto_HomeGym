package it.homegym.model;

import java.util.Date;
import java.util.Objects;

public class TrainingPlan {
    private int id;
    private String title;
    private String description;
    private String content; // json/text with exercises
    private Integer createdBy;
    private Date createdAt;
    private Date updatedAt;
    private boolean deleted;

    // --- attachment metadata ---
    private String attachmentFilename;
    private String attachmentContentType;
    private String attachmentPath;
    private Long attachmentSize;

    // getters/setters + toString/equals/hashCode
    // ...
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
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
    public Date getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    public boolean isDeleted() {
        return deleted;
    }
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    public String getAttachmentFilename() {
        return attachmentFilename;
    }
    public void setAttachmentFilename(String attachmentFilename) {
        this.attachmentFilename = attachmentFilename;
    }
    public String getAttachmentContentType() {
        return attachmentContentType;
    }
    public void setAttachmentContentType(String attachmentContentType) {
        this.attachmentContentType = attachmentContentType;
    }
    public String getAttachmentPath() {
        return attachmentPath;
    }
    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }
    public Long getAttachmentSize() {
        return attachmentSize;
    }
    public void setAttachmentSize(Long attachmentSize) {
        this.attachmentSize = attachmentSize;
    }
    @Override
    public String toString() {
        return "TrainingPlan{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", content='" + content + '\'' +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", deleted=" + deleted +
                ", attachmentFilename='" + attachmentFilename + '\'' +
                ", attachmentContentType='" + attachmentContentType + '\'' +
                ", attachmentPath='" + attachmentPath + '\'' +
                ", attachmentSize=" + attachmentSize +
                '}';
    }
}
