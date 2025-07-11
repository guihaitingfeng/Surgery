package com.surgery.repository;

import com.surgery.entity.OperatingBed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatingBedRepository extends JpaRepository<OperatingBed, Long> {
    
    List<OperatingBed> findByRoomId(Long roomId);
    
    List<OperatingBed> findByStatus(String status);
    
    List<OperatingBed> findByRoomIdAndStatus(Long roomId, String status);
    
    Optional<OperatingBed> findByRoomIdAndBedNumber(Long roomId, String bedNumber);
    
    boolean existsByRoomIdAndBedNumber(Long roomId, String bedNumber);
    
    long countByRoomId(Long roomId);
    
    long countByStatus(String status);
    
    @Query("SELECT ob FROM OperatingBed ob WHERE ob.room.id = :roomId AND ob.status = 'AVAILABLE'")
    List<OperatingBed> findAvailableBedsByRoom(@Param("roomId") Long roomId);
} 