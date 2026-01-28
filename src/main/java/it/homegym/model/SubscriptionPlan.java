package it.homegym.model;

public class SubscriptionPlan {
    private Integer id;
    private String code;
    private String name;
    private String description;
    private Integer durationDays;
    private Long priceCents;
    private String currency;
    private Boolean active;

    // getters / setters
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;   
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Integer getDurationDays() {
        return durationDays;
    }
    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }
    public Long getPriceCents() {
        return priceCents;
    }
    public void setPriceCents(Long priceCents) {
        this.priceCents = priceCents;
    }
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    public Boolean getActive() {
        return active;
    }
    public void setActive(Boolean active) {
        this.active = active;
    }
    
}
