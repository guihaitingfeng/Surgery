package com.surgery.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.surgery.enums.AppointmentStatus;
import com.surgery.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "surgery_appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SurgeryAppointment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Patient patient;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User doctor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anesthesiologist_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User anesthesiologist;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User nurse;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "operatingBeds"})
    private OperatingRoom room;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bed_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private OperatingBed bed;
    
    @Column(name = "surgery_name", nullable = false, length = 200)
    private String surgeryName;
    
    @Column(name = "surgery_type", length = 100)
    private String surgeryType;
    
    @Column(name = "planned_date", nullable = false)
    private LocalDate plannedDate;
    
    @Column(name = "planned_start_time", nullable = false)
    private LocalTime plannedStartTime;
    
    @Column(name = "planned_end_time", nullable = false)
    private LocalTime plannedEndTime;
    
    @Column(name = "estimated_duration", nullable = false)
    private Integer estimatedDuration; // 预计时长(分钟)
    
    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;
    
    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;
    
    @Column(name = "surgery_description", columnDefinition = "TEXT")
    private String surgeryDescription;
    
    @Column(name = "pre_surgery_notes", columnDefinition = "TEXT")
    private String preSurgeryNotes;
    
    @Column(name = "post_surgery_notes", columnDefinition = "TEXT")
    private String postSurgeryNotes;
    
    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;
    
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level")
    private SeverityLevel priorityLevel = SeverityLevel.NORMAL;
    
    // 确认状态跟踪
    @Column(name = "anesthesiologist_confirmed")
    private Boolean anesthesiologistConfirmed = false;
    
    @Column(name = "nurse_confirmed")
    private Boolean nurseConfirmed = false;
    
    @Column(name = "anesthesiologist_confirmed_at")
    private LocalDateTime anesthesiologistConfirmedAt;
    
    @Column(name = "nurse_confirmed_at")
    private LocalDateTime nurseConfirmedAt;
    
    @Column(name = "doctor_final_confirmed")
    private Boolean doctorFinalConfirmed = false;
    
    @Column(name = "doctor_final_confirmed_at")
    private LocalDateTime doctorFinalConfirmedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User createdBy;
    
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