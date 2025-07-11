package com.surgery.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancellation_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancellationRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private SurgeryAppointment appointment;
    
    @Column(name = "cancellation_date", nullable = false)
    private LocalDate cancellationDate;
    
    @Column(name = "cancellation_time", nullable = false)
    private LocalDateTime cancellationTime;
    
    @Column(name = "hours_before_surgery", nullable = false)
    private Integer hoursBeforeSurgery;
    
    @Column(name = "reason")
    private String reason;
    
    @PrePersist
    protected void onCreate() {
        this.cancellationTime = LocalDateTime.now();
        this.cancellationDate = LocalDate.now();
    }
} 