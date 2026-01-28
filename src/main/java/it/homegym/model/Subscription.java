package it.homegym.model;

import java.time.LocalDate;

public class Subscription {
    private Integer id;
    private Integer userId;
    private Integer planId;
    private String status; // PENDING, ACTIVE, CANCELLED, EXPIRED
    private Long priceCents;
    private String currency;
    private LocalDate startDate;
    private LocalDate endDate;
    private String paymentProvider;
    private String paymentProviderSubscriptionId;

    // getters / setters
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public Integer getUserId() {
        return userId;
    }
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    public Integer getPlanId() {
        return planId;
    }
    public void setPlanId(Integer planId) {
        this.planId = planId;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
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
    public LocalDate getStartDate() {
        return startDate;
    }
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    public LocalDate getEndDate() {
        return endDate;
    }
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    public String getPaymentProvider() {
        return paymentProvider;
    }
    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }
    public String getPaymentProviderSubscriptionId() {
        return paymentProviderSubscriptionId;
    }
    public void setPaymentProviderSubscriptionId(String paymentProviderSubscriptionId) {
        this.paymentProviderSubscriptionId = paymentProviderSubscriptionId;
    }
    
}
