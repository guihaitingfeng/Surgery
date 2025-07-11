package com.surgery.enums;

public enum AppointmentStatus {
    SCHEDULED("已安排"),
    PENDING_CONFIRMATION("等待医疗团队确认"),
    TEAM_CONFIRMED("医疗团队已确认"),
    DOCTOR_FINAL_CONFIRMED("医生最终确认"),
    NOTIFIED("已通知病人"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    POSTPONED("已延期");
    
    private final String displayName;
    
    AppointmentStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
} 