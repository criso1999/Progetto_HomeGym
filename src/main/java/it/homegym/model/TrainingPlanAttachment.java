package it.homegym.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class TrainingPlanAttachment implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int planId;
    private String filename;
    private String path;
    private String contentType;
    private Long size;
    private Date uploadedAt;
    private Integer uploadedBy; // opzionale

    public TrainingPlanAttachment() {}

    public TrainingPlanAttachment(int planId, String filename, String path) {
        this.planId = planId;
        this.filename = filename;
        this.path = path;
    }

    // getters / setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public Date getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Date uploadedAt) { this.uploadedAt = uploadedAt; }

    public Integer getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Integer uploadedBy) { this.uploadedBy = uploadedBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrainingPlanAttachment)) return false;
        TrainingPlanAttachment that = (TrainingPlanAttachment) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "TrainingPlanAttachment{" +
                "id=" + id +
                ", planId=" + planId +
                ", filename='" + filename + '\'' +
                ", path='" + path + '\'' +
                ", contentType='" + contentType + '\'' +
                ", size=" + size +
                ", uploadedAt=" + uploadedAt +
                ", uploadedBy=" + uploadedBy +
                '}';
    }
}
