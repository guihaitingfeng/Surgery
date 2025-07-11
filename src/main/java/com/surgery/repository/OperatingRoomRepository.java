package com.surgery.repository;

import com.surgery.entity.OperatingRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OperatingRoomRepository extends JpaRepository<OperatingRoom, Long> {
    
    Optional<OperatingRoom> findByRoomNumber(String roomNumber);
    
    boolean existsByRoomNumber(String roomNumber);
    
    List<OperatingRoom> findByStatus(String status);
    
    long countByStatus(String status);
    
    List<OperatingRoom> findByFloorNumber(Integer floorNumber);
    
    @Query("SELECT or FROM OperatingRoom or WHERE or.status = 'AVAILABLE'")
    List<OperatingRoom> findAvailableRooms();
    
    @Query("SELECT DISTINCT or FROM OperatingRoom or " +
           "LEFT JOIN SurgeryAppointment sa ON or.id = sa.room.id " +
           "WHERE or.status = 'AVAILABLE' AND " +
           "(sa.id IS NULL OR sa.plannedDate != :date OR sa.status = 'CANCELLED')")
    List<OperatingRoom> findAvailableRoomsForDate(@Param("date") LocalDate date);
    
    @Query("SELECT or FROM OperatingRoom or WHERE " +
           "or.roomNumber LIKE %:keyword% OR or.roomName LIKE %:keyword%")
    List<OperatingRoom> searchRooms(@Param("keyword") String keyword);
} 