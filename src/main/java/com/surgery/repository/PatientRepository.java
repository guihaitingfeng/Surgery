package com.surgery.repository;

import com.surgery.entity.Patient;
import com.surgery.entity.User;
import com.surgery.enums.SeverityLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    
    Optional<Patient> findByMedicalRecordNumber(String medicalRecordNumber);
    
    Optional<Patient> findByUserId(Long userId);
    
    // 重写 findById 方法，确保关联对象被加载
    @Query("SELECT p FROM Patient p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.assignedDoctor WHERE p.id = :id")
    Optional<Patient> findByIdWithUser(@Param("id") Long id);
    
    List<Patient> findByAssignedDoctor(User doctor);
    
    List<Patient> findByAssignedDoctorAndStatus(User doctor, String status);
    
    List<Patient> findByStatus(String status);
    
    List<Patient> findBySeverityLevel(SeverityLevel severityLevel);
    
    @Query("SELECT p FROM Patient p WHERE p.assignedDoctor IS NULL AND p.status = 'WAITING'")
    List<Patient> findUnassignedPatients();
    
    @Query("SELECT p FROM Patient p WHERE p.assignedDoctor = :doctor AND p.status IN ('WAITING', 'SCHEDULED')")
    List<Patient> findActivePatientsForDoctor(@Param("doctor") User doctor);
    
    @Query("SELECT p FROM Patient p WHERE p.status = 'WAITING' ORDER BY p.severityLevel DESC, p.createdAt ASC")
    List<Patient> findWaitingPatientsByPriority();
    
    Page<Patient> findByAssignedDoctor(User doctor, Pageable pageable);
    
    @Query("SELECT p FROM Patient p WHERE " +
           "p.user.realName LIKE %:keyword% OR " +
           "p.medicalRecordNumber LIKE %:keyword% OR " +
           "p.diseaseDescription LIKE %:keyword%")
    Page<Patient> searchPatients(@Param("keyword") String keyword, Pageable pageable);
    
    // 获取用户最新的病情信息
    @Query("SELECT p FROM Patient p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    List<Patient> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    // 获取用户最新的一条病情信息
    @Query("SELECT p FROM Patient p WHERE p.user.id = :userId ORDER BY p.createdAt DESC LIMIT 1")
    Optional<Patient> findLatestByUserId(@Param("userId") Long userId);
} 