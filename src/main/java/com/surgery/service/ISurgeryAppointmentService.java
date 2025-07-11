package com.surgery.service;

import com.surgery.entity.SurgeryAppointment;
import com.surgery.entity.User;
import com.surgery.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ISurgeryAppointmentService {
    
    SurgeryAppointment createAppointment(SurgeryAppointment appointment);
    
    SurgeryAppointment updateAppointment(Long id, SurgeryAppointment appointmentDetails);
    
    void confirmAppointment(Long appointmentId, Long userId);
    
    void startSurgery(Long id);
    
    void completeSurgery(Long id, String postSurgeryNotes);
    
    void cancelAppointment(Long appointmentId, Long userId, String reason);
    
    Optional<SurgeryAppointment> findById(Long id);
    
    List<SurgeryAppointment> findAppointmentsForDate(LocalDate date);
    
    List<SurgeryAppointment> findActiveAppointmentsForDate(LocalDate date);
    
    List<SurgeryAppointment> findAppointmentsForWeek(LocalDate startDate);
    
    List<SurgeryAppointment> findAppointmentsForDoctor(User doctor);
    
    List<SurgeryAppointment> findAppointmentsForNurse(User nurse);
    
    List<SurgeryAppointment> findAppointmentsForAnesthesiologist(User anesthesiologist);
    
    List<SurgeryAppointment> findPendingConfirmationsForStaff(User user, LocalDate date);
    
    List<SurgeryAppointment> findRoomScheduleForDate(Long roomId, LocalDate date);
    
    List<SurgeryAppointment> findBedScheduleForDate(Long bedId, LocalDate date);
    
    Page<SurgeryAppointment> findAppointmentsByStatus(List<AppointmentStatus> statuses, Pageable pageable);
    
    Page<SurgeryAppointment> searchAppointments(String patientName, String status, LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    List<SurgeryAppointment> findAppointmentsForPatient(Long patientId);
    List<SurgeryAppointment> findUpcomingAppointmentsForPatient(Long patientId);
    
    long countAllAppointments();
    long countTodayAppointments();
    
    // 医生统计方法
    long countTodayAppointmentsForDoctor(Long doctorId);
    long countCompletedAppointmentsForDoctor(Long doctorId, LocalDate date);
    long countPendingAppointmentsForDoctor(Long doctorId);
    
    // 医护人员统计方法
    long countTodayAppointmentsForStaff(Long staffId, com.surgery.enums.UserRole role);
    long countCompletedAppointmentsForStaff(Long staffId, com.surgery.enums.UserRole role, LocalDate date);
    long countPendingAppointmentsForStaff(Long staffId, com.surgery.enums.UserRole role);
    
    void sendConfirmationToTeam(Long id);
    void notifyPatient(Long id);
    
    void confirmByTeamMember(Long appointmentId, Long userId);
    void doctorFinalConfirm(Long appointmentId, Long userId);
    
    void deleteAppointment(Long id);
} 