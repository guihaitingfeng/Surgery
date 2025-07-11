package com.surgery.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.surgery.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;
    
    @Column(name = "medical_record_number", unique = true, nullable = false, length = 50)
    private String medicalRecordNumber;
    
    @Column(name = "id_card", length = 18)
    private String idCard;
    
    @Column(name = "emergency_contact", length = 100)
    private String emergencyContact;
    
    @Column(name = "emergency_phone", length = 20)
    private String emergencyPhone;
    
    @Column(name = "medical_history", columnDefinition = "TEXT")
    private String medicalHistory;
    
    @Column(columnDefinition = "TEXT")
    private String allergies;
    
    @Column(name = "current_medications", columnDefinition = "TEXT")
    private String currentMedications;
    
    @Column(name = "admission_date")
    private LocalDate admissionDate;
    
    @Column(name = "ward_number", length = 50)
    private String wardNumber;
    
    @Column(name = "bed_number", length = 20)
    private String bedNumber;
    
    @Column(name = "disease_description", nullable = false, columnDefinition = "TEXT")
    private String diseaseDescription;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity_level")
    private SeverityLevel severityLevel = SeverityLevel.NORMAL;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_doctor_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User assignedDoctor;
    
    @Column(length = 20)
    private String status = "WAITING"; // WAITING, SCHEDULED, COMPLETED, CANCELLED
    
    @Column(name = "last_visit_date")
    private LocalDate lastVisitDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 