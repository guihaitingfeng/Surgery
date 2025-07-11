package com.surgery.enums;

public enum SeverityLevel {
    EMERGENCY("紧急"),
    URGENT("急诊"),
    NORMAL("普通"),
    LOW("低级");
    
    private final String displayName;
    
    SeverityLevel(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
} 