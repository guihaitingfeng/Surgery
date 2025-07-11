package com.surgery.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "operating_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OperatingRoom {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "room_number", unique = true, nullable = false, length = 20)
    private String roomNumber;
    
    @Column(name = "room_name", nullable = false, length = 100)
    private String roomName;
    
    @Column(name = "floor_number")
    private Integer floorNumber;
    
    @Column(name = "equipment_list", columnDefinition = "TEXT")
    private String equipmentList;
    
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "room"})
    private List<OperatingBed> operatingBeds;
    
    @Column(length = 20)
    private String status = "AVAILABLE"; // AVAILABLE, OCCUPIED, MAINTENANCE, RESERVED
    
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