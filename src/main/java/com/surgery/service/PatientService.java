package com.surgery.service;

import com.surgery.entity.Patient;
import com.surgery.entity.User;
import com.surgery.enums.SeverityLevel;
import com.surgery.enums.UserRole;
import com.surgery.repository.PatientRepository;
import com.surgery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientService implements IPatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    @Override
    public Patient createPatient(Patient patient) {
        // 检查病历号是否已存在
        if (patientRepository.findByMedicalRecordNumber(patient.getMedicalRecordNumber()).isPresent()) {
            throw new RuntimeException("病历号已存在");
        }
        
        // 自动分配随机医生
        assignRandomDoctor(patient);
        
        return patientRepository.save(patient);
    }

    @Override
    public Patient updatePatient(Long id, Patient patientDetails) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("病人不存在"));
        
        patient.setMedicalRecordNumber(patientDetails.getMedicalRecordNumber());
        patient.setIdCard(patientDetails.getIdCard());
        patient.setEmergencyContact(patientDetails.getEmergencyContact());
        patient.setEmergencyPhone(patientDetails.getEmergencyPhone());
        patient.setMedicalHistory(patientDetails.getMedicalHistory());
        patient.setAllergies(patientDetails.getAllergies());
        patient.setCurrentMedications(patientDetails.getCurrentMedications());
        patient.setAdmissionDate(patientDetails.getAdmissionDate());
        patient.setWardNumber(patientDetails.getWardNumber());
        patient.setBedNumber(patientDetails.getBedNumber());
        patient.setDiseaseDescription(patientDetails.getDiseaseDescription());
        patient.setSeverityLevel(patientDetails.getSeverityLevel());
        
        return patientRepository.save(patient);
    }

    @Override
    public void assignRandomDoctor(Patient patient) {
        // 先检查该用户是否已经有分配过的医生
        List<Patient> existingPatients = patientRepository.findByUserIdOrderByCreatedAtDesc(patient.getUser().getId());
        
        if (!existingPatients.isEmpty()) {
            // 如果已经有病情记录，使用同一个医生
            User existingDoctor = existingPatients.get(0).getAssignedDoctor();
            if (existingDoctor != null) {
                patient.setAssignedDoctor(existingDoctor);
                return;
            }
        }
        
        // 如果是第一次或者之前没有分配医生，则随机分配
        List<User> doctors = userRepository.findByRoleAndIsActiveTrue(UserRole.DOCTOR);
        if (!doctors.isEmpty()) {
            User randomDoctor = doctors.get(random.nextInt(doctors.size()));
            patient.setAssignedDoctor(randomDoctor);
        }
    }

    @Override
    public void assignDoctor(Long patientId, Long doctorId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("病人不存在"));
        
        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("医生不存在"));
        
        if (doctor.getRole() != UserRole.DOCTOR) {
            throw new RuntimeException("指定用户不是医生");
        }
        
        patient.setAssignedDoctor(doctor);
        patientRepository.save(patient);
    }

    @Override
    public Optional<Patient> findById(Long id) {
        return patientRepository.findByIdWithUser(id);
    }

    @Override
    public Optional<Patient> findByMedicalRecordNumber(String medicalRecordNumber) {
        return patientRepository.findByMedicalRecordNumber(medicalRecordNumber);
    }

    @Override
    public Optional<Patient> findByUserId(Long userId) {
        return patientRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findPatientsForDoctor(User doctor) {
        return patientRepository.findByAssignedDoctor(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findActivePatientsForDoctor(User doctor) {
        return patientRepository.findActivePatientsForDoctor(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findWaitingPatients() {
        return patientRepository.findByStatus("WAITING");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findWaitingPatientsByPriority() {
        return patientRepository.findWaitingPatientsByPriority();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findUnassignedPatients() {
        return patientRepository.findUnassignedPatients();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findPatientsBySeverity(SeverityLevel severityLevel) {
        return patientRepository.findBySeverityLevel(severityLevel);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Patient> searchPatients(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return patientRepository.findAll(pageable);
        }
        return patientRepository.searchPatients(keyword.trim(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Patient> findPatientsForDoctor(User doctor, Pageable pageable) {
        return patientRepository.findByAssignedDoctor(doctor, pageable);
    }

    @Override
    public void updatePatientStatus(Long patientId, String status) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("病人不存在"));
        patient.setStatus(status);
        patientRepository.save(patient);
    }

    @Override
    public void deletePatient(Long id) {
        if (!patientRepository.existsById(id)) {
            throw new RuntimeException("病人不存在");
        }
        patientRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findPatientHistoryByUserId(Long userId) {
        return patientRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Patient> findLatestPatientByUserId(Long userId) {
        return patientRepository.findLatestByUserId(userId);
    }
} 