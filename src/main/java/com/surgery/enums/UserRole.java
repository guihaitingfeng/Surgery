package com.surgery.enums;

public enum UserRole {
    PATIENT("病人"),
    DOCTOR("医生"),
    NURSE("护士"),
    ANESTHESIOLOGIST("麻醉师"),
    ADMIN("管理员");
    
    private final String displayName;
    
    UserRole(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
} 