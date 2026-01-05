package com.ainovel.app.settings.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "global_settings")
public class GlobalSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private boolean registrationEnabled = true;
    private boolean maintenanceMode = false;

    private int checkInMinPoints = 10;
    private int checkInMaxPoints = 50;

    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    private String smtpPassword;

    private String llmBaseUrl;
    private String llmModelName;
    private String llmApiKeyEncrypted;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public boolean isRegistrationEnabled() { return registrationEnabled; }
    public void setRegistrationEnabled(boolean registrationEnabled) { this.registrationEnabled = registrationEnabled; }
    public boolean isMaintenanceMode() { return maintenanceMode; }
    public void setMaintenanceMode(boolean maintenanceMode) { this.maintenanceMode = maintenanceMode; }
    public int getCheckInMinPoints() { return checkInMinPoints; }
    public void setCheckInMinPoints(int checkInMinPoints) { this.checkInMinPoints = checkInMinPoints; }
    public int getCheckInMaxPoints() { return checkInMaxPoints; }
    public void setCheckInMaxPoints(int checkInMaxPoints) { this.checkInMaxPoints = checkInMaxPoints; }
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }
    public Integer getSmtpPort() { return smtpPort; }
    public void setSmtpPort(Integer smtpPort) { this.smtpPort = smtpPort; }
    public String getSmtpUsername() { return smtpUsername; }
    public void setSmtpUsername(String smtpUsername) { this.smtpUsername = smtpUsername; }
    public String getSmtpPassword() { return smtpPassword; }
    public void setSmtpPassword(String smtpPassword) { this.smtpPassword = smtpPassword; }
    public String getLlmBaseUrl() { return llmBaseUrl; }
    public void setLlmBaseUrl(String llmBaseUrl) { this.llmBaseUrl = llmBaseUrl; }
    public String getLlmModelName() { return llmModelName; }
    public void setLlmModelName(String llmModelName) { this.llmModelName = llmModelName; }
    public String getLlmApiKeyEncrypted() { return llmApiKeyEncrypted; }
    public void setLlmApiKeyEncrypted(String llmApiKeyEncrypted) { this.llmApiKeyEncrypted = llmApiKeyEncrypted; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
