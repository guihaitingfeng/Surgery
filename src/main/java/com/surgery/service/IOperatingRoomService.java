package com.surgery.service;

import com.surgery.entity.OperatingRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface IOperatingRoomService {
    
    Page<OperatingRoom> findAllRooms(Pageable pageable);
    
    List<OperatingRoom> findAvailableRooms();
    
    Optional<OperatingRoom> findById(Long id);
    
    OperatingRoom createRoom(OperatingRoom room);
    
    OperatingRoom updateRoom(OperatingRoom room);
    
    void deleteRoom(Long id);
    
    OperatingRoom updateRoomStatus(Long id, String status);
    
    List<OperatingRoom> findByStatus(String status);
    
    long countAllRooms();
    
    long countByStatus(String status);
} 