package com.surgery.repository;

import com.surgery.entity.Patient;
import com.surgery.entity.SurgeryAppointment;
import com.surgery.entity.User;
import com.surgery.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SurgeryAppointmentRepository extends JpaRepository<SurgeryAppointment, Long> {
    
    List<SurgeryAppointment> findByPlannedDate(LocalDate date);
    
    List<SurgeryAppointment> findByPlannedDateAndStatus(LocalDate date, AppointmentStatus status);
    
    List<SurgeryAppointment> findByDoctor(User doctor);
    
    List<SurgeryAppointment> findByDoctorAndStatus(User doctor, AppointmentStatus status);
    
    List<SurgeryAppointment> findByAnesthesiologist(User anesthesiologist);
    
    List<SurgeryAppointment> findByNurse(User nurse);
    
    @Query("SELECT sa FROM SurgeryAppointment sa " +
           "LEFT JOIN FETCH sa.patient p " +
           "LEFT JOIN FETCH p.user " +
           "LEFT JOIN FETCH sa.doctor " +
           "LEFT JOIN FETCH sa.anesthesiologist " +
           "LEFT JOIN FETCH sa.nurse " +
           "LEFT JOIN FETCH sa.room " +
           "LEFT JOIN FETCH sa.bed " +
           "WHERE sa.id = :id")
    Optional<SurgeryAppointment> findByIdWithDetails(@Param("id") Long id);
    
    @Query("SELECT sa FROM SurgeryAppointment sa " +
           "LEFT JOIN FETCH sa.patient p " +
           "LEFT JOIN FETCH p.user " +
           "LEFT JOIN FETCH sa.anesthesiologist " +
           "LEFT JOIN FETCH sa.nurse " +
           "LEFT JOIN FETCH sa.room " +
           "LEFT JOIN FETCH sa.bed " +
           "WHERE sa.doctor = :doctor")
    List<SurgeryAppointment> findByDoctorWithDetails(@Param("doctor") User doctor);
    
    @Query("SELECT sa FROM SurgeryAppointment sa " +
           "LEFT JOIN FETCH sa.patient p " +
           "LEFT JOIN FETCH p.user " +
           "LEFT JOIN FETCH sa.doctor " +
           "LEFT JOIN FETCH sa.anesthesiologist " +
           "LEFT JOIN FETCH sa.room " +
           "LEFT JOIN FETCH sa.bed " +
           "WHERE sa.nurse = :nurse " +
           "ORDER BY sa.plannedDate DESC, sa.plannedStartTime DESC")
    List<SurgeryAppointment> findByNurseWithDetails(@Param("nurse") User nurse);
    
    @Query("SELECT sa FROM SurgeryAppointment sa " +
           "LEFT JOIN FETCH sa.patient p " +
           "LEFT JOIN FETCH sa.doctor " +
           "LEFT JOIN FETCH sa.anesthesiologist " +
           "LEFT JOIN FETCH sa.room " +
           "LEFT JOIN FETCH sa.bed " +
           "WHERE sa.anesthesiologist = :anesthesiologist " +
           "ORDER BY sa.plannedDate DESC, sa.plannedStartTime DESC")
    List<SurgeryAppointment> findByAnesthesiologistWithDetails(@Param("anesthesiologist") User anesthesiologist);
    
    @Query("SELECT sa FROM SurgeryAppointment sa WHERE sa.plannedDate = :date AND sa.status IN ('SCHEDULED', 'PENDING_CONFIRMATION', 'TEAM_CONFIRMED', 'DOCTOR_FINAL_CONFIRMED', 'NOTIFIED', 'IN_PROGRESS')")
    List<SurgeryAppointment> findActiveSurgeriesForDate(@Param("date") LocalDate date);
    
    @Query("SELECT sa FROM SurgeryAppointment sa WHERE sa.plannedDate BETWEEN :startDate AND :endDate ORDER BY sa.plannedDate, sa.plannedStartTime")
    List<SurgeryAppointment> findSurgeriesBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT sa FROM SurgeryAppointment sa WHERE sa.room.id = :roomId AND sa.plannedDate = :date AND sa.status IN ('SCHEDULED', 'PENDING_CONFIRMATION', 'TEAM_CONFIRMED', 'DOCTOR_FINAL_CONFIRMED', 'NOTIFIED', 'IN_PROGRESS')")
    List<SurgeryAppointment> findRoomScheduleForDate(@Param("roomId") Long roomId, @Param("date") LocalDate date);
    
    @Query("SELECT sa FROM SurgeryAppointment sa " +
           "LEFT JOIN FETCH sa.patient p " +
           "LEFT JOIN FETCH sa.doctor " +
           "LEFT JOIN FETCH sa.anesthesiologist " +
           "LEFT JOIN FETCH sa.nurse " +
           "LEFT JOIN FETCH sa.room " +
           "LEFT JOIN FETCH sa.bed " +
           "WHERE sa.bed.id = :bedId AND sa.plannedDate = :date AND sa.status IN ('SCHEDULED', 'PENDING_CONFIRMATION', 'TEAM_CONFIRMED', 'DOCTOR_FINAL_CONFIRMED', 'NOTIFIED', 'IN_PROGRESS')")
    List<SurgeryAppointment> findBedScheduleForDate(@Param("bedId") Long bedId, @Param("date") LocalDate date);
    
    @Query("SELECT sa FROM SurgeryAppointment sa WHERE " +
           "(sa.anesthesiologist = :user OR sa.nurse = :user) AND " +
           "sa.plannedDate = :date AND sa.status = 'PENDING_CONFIRMATION'")
    List<SurgeryAppointment> findPendingConfirmationsForStaff(@Param("user") User user, @Param("date") LocalDate date);
    
    @Query("SELECT COUNT(sa) FROM SurgeryAppointment sa WHERE sa.doctor = :doctor AND sa.plannedDate = :date AND sa.status IN ('SCHEDULED', 'PENDING_CONFIRMATION', 'TEAM_CONFIRMED', 'DOCTOR_FINAL_CONFIRMED', 'NOTIFIED', 'IN_PROGRESS')")
    Long countDoctorSurgeriesForDate(@Param("doctor") User doctor, @Param("date") LocalDate date);
    
    Page<SurgeryAppointment> findByStatusIn(List<AppointmentStatus> statuses, Pageable pageable);
    
    @Query("SELECT sa FROM SurgeryAppointment sa " +
           "LEFT JOIN FETCH sa.patient p " +
           "LEFT JOIN FETCH p.user u " +
           "LEFT JOIN FETCH sa.doctor " +
           "LEFT JOIN FETCH sa.anesthesiologist " +
           "LEFT JOIN FETCH sa.nurse " +
           "LEFT JOIN FETCH sa.room " +
           "LEFT JOIN FETCH sa.bed " +
           "WHERE (:patientName IS NULL OR u.realName LIKE CONCAT('%', :patientName, '%')) " +
           "AND (:status IS NULL OR sa.status = :status) " +
           "AND (:startDate IS NULL OR sa.plannedDate >= :startDate) " +
           "AND (:endDate IS NULL OR sa.plannedDate <= :endDate)")
    Page<SurgeryAppointment> searchAppointments(
            @Param("patientName") String patientName,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);
    
    Page<SurgeryAppointment> findByDoctorAndPlannedDateBetween(User doctor, LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    List<SurgeryAppointment> findByPatient(Patient patient);
    
    @Query("SELECT sa FROM SurgeryAppointment sa " +
           "LEFT JOIN FETCH sa.patient p " +
           "LEFT JOIN FETCH p.user " +
           "LEFT JOIN FETCH sa.doctor " +
           "LEFT JOIN FETCH sa.anesthesiologist " +
           "LEFT JOIN FETCH sa.nurse " +
           "LEFT JOIN FETCH sa.room " +
           "LEFT JOIN FETCH sa.bed " +
           "WHERE sa.patient = :patient " +
           "ORDER BY sa.plannedDate DESC, sa.plannedStartTime DESC")
    List<SurgeryAppointment> findByPatientWithDetails(@Param("patient") Patient patient);
    
    List<SurgeryAppointment> findByPatientAndPlannedDateGreaterThanEqualOrderByPlannedDateAsc(Patient patient, LocalDate date);
    
    @Query("SELECT sa FROM SurgeryAppointment sa WHERE sa.plannedDate = :date AND sa.plannedStartTime < :currentTime AND sa.status = 'NOTIFIED'")
    List<SurgeryAppointment> findOverdueAppointments(@Param("date") LocalDate date, @Param("currentTime") java.time.LocalTime currentTime);
} 