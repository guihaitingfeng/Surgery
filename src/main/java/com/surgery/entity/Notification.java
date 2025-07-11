package com.surgery.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;
    
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_appointment_id")
    private SurgeryAppointment relatedAppointment;
    
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    
    public enum NotificationType {
        SURGERY_SCHEDULED("手术安排"),
        SURGERY_CONFIRMED("手术确认"),
        SURGERY_CANCELLED("手术取消"),
        SURGERY_UPDATED("手术更新"),
        SURGERY_NOTIFICATION("手术通知"),
        SURGERY_UPDATE("手术状态更新"),
        SURGERY_COMPLETE("手术完成"),
        CONFIRMATION("确认通知"),
        TEAM_CONFIRMATION_REQUEST("医疗团队确认请求"),
        TEAM_CONFIRMATION_COMPLETED("医疗团队确认完成"),
        DOCTOR_FINAL_CONFIRMATION_REQUEST("医生最终确认请求"),
        PATIENT_SURGERY_NOTICE("病人手术通知单"),
        SCHEDULE_ASSIGNED("排班分配");
        
        private final String displayName;
        
        NotificationType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
} 