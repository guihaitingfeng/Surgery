package com.surgery.service;

import com.surgery.entity.Patient;
import com.surgery.entity.User;
import com.surgery.enums.SeverityLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface IPatientService {
    
    Patient createPatient(Patient patient);
    
    Patient updatePatient(Long id, Patient patientDetails);
    
    void assignRandomDoctor(Patient patient);
    
    void assignDoctor(Long patientId, Long doctorId);
    
    Optional<Patient> findById(Long id);
    
    Optional<Patient> findByMedicalRecordNumber(String medicalRecordNumber);
    
    Optional<Patient> findByUserId(Long userId);
    
    List<Patient> findPatientsForDoctor(User doctor);
    
    List<Patient> findActivePatientsForDoctor(User doctor);
    
    List<Patient> findWaitingPatients();
    
    List<Patient> findWaitingPatientsByPriority();
    
    List<Patient> findUnassignedPatients();
    
    List<Patient> findPatientsBySeverity(SeverityLevel severityLevel);
    
    Page<Patient> searchPatients(String keyword, Pageable pageable);
    
    Page<Patient> findPatientsForDoctor(User doctor, Pageable pageable);
    
    void updatePatientStatus(Long patientId, String status);
    
    void deletePatient(Long id);
    
    // 获取用户的所有病情记录（按时间倒序）
    List<Patient> findPatientHistoryByUserId(Long userId);
    
    // 获取用户最新的病情信息
    Optional<Patient> findLatestPatientByUserId(Long userId);
} 