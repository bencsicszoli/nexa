package com.nexa.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Egy felhasználó előfizetésének tükre a DB-ben (1:1 a userhez). A Paddle az
 * igazság forrása; ide a webhookok írják az állapotot (lásd {@link SubscriptionService}).
 * A kártyaadat sosem kerül ide — azt kizárólag a Paddle kezeli.
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue
    private UUID id;

    /** A tulajdonos felhasználó azonosítója (egyedi — felhasználónként egy sor). */
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.NONE;

    /** A választott csomag; null, amíg nincs checkout. */
    @Enumerated(EnumType.STRING)
    @Column(name = "plan")
    private Plan plan;

    /** Paddle customer azonosító (ctm_...) — a számlázási portálhoz. */
    @Column(name = "paddle_customer_id")
    private String paddleCustomerId;

    /** Paddle subscription azonosító (sub_...). */
    @Column(name = "paddle_subscription_id", unique = true)
    private String paddleSubscriptionId;

    /** A próbaidő vége (TRIALING állapotban). */
    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    /** A következő megújulás/terhelés időpontja (ACTIVE állapotban). */
    @Column(name = "renews_at")
    private Instant renewsAt;

    /** A lemondás időpontja (CANCELED állapotban). */
    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Subscription() {
        // JPA
    }

    public Subscription(UUID userId) {
        this.userId = userId;
    }

    /** Frissíti az {@code updatedAt} bélyeget — minden módosító művelet után hívandó. */
    public void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public String getPaddleCustomerId() {
        return paddleCustomerId;
    }

    public void setPaddleCustomerId(String paddleCustomerId) {
        this.paddleCustomerId = paddleCustomerId;
    }

    public String getPaddleSubscriptionId() {
        return paddleSubscriptionId;
    }

    public void setPaddleSubscriptionId(String paddleSubscriptionId) {
        this.paddleSubscriptionId = paddleSubscriptionId;
    }

    public Instant getTrialEndsAt() {
        return trialEndsAt;
    }

    public void setTrialEndsAt(Instant trialEndsAt) {
        this.trialEndsAt = trialEndsAt;
    }

    public Instant getRenewsAt() {
        return renewsAt;
    }

    public void setRenewsAt(Instant renewsAt) {
        this.renewsAt = renewsAt;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(Instant canceledAt) {
        this.canceledAt = canceledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
