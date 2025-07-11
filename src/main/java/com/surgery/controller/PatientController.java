package com.surgery.controller;

import com.surgery.entity.Patient;
import com.surgery.entity.User;
import com.surgery.enums.SeverityLevel;
import com.surgery.service.IPatientService;
import com.surgery.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/patients")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PatientController {

    private final IPatientService patientService;
    private final IUserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPatient(@Valid @RequestBody Patient patient, Authentication authentication) {
        try {
            Patient savedPatient = patientService.createPatient(patient);
            return ResponseEntity.ok(savedPatient);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getPatient(@PathVariable Long id) {
        Optional<Patient> patient = patientService.findById(id);
        if (patient.isPresent()) {
            return ResponseEntity.ok(patient.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> updatePatient(@PathVariable Long id, @Valid @RequestBody Patient patientDetails) {
        try {
            Patient updatedPatient = patientService.updatePatient(id, patientDetails);
            return ResponseEntity.ok(updatedPatient);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletePatient(@PathVariable Long id) {
        try {
            patientService.deletePatient(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "病人信息删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getAllPatients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String severity) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Patient> patients;
        
        // 如果有严重程度筛选
        if (severity != null && !severity.trim().isEmpty()) {
            try {
                SeverityLevel severityLevel = SeverityLevel.valueOf(severity.toUpperCase());
                List<Patient> allPatients = patientService.findPatientsBySeverity(severityLevel);
                
                // 如果还有搜索关键词，进一步过滤
                if (search != null && !search.trim().isEmpty()) {
                    String keyword = search.toLowerCase();
                    allPatients = allPatients.stream()
                        .filter(patient -> 
                            (patient.getUser() != null && patient.getUser().getRealName() != null && 
                             patient.getUser().getRealName().toLowerCase().contains(keyword)) ||
                            (patient.getMedicalRecordNumber() != null && 
                             patient.getMedicalRecordNumber().toLowerCase().contains(keyword)) ||
                            (patient.getDiseaseDescription() != null && 
                             patient.getDiseaseDescription().toLowerCase().contains(keyword))
                        )
                        .toList();
                }
                
                // 手动分页
                int start = page * size;
                int end = Math.min(start + size, allPatients.size());
                List<Patient> pagedPatients = allPatients.subList(start, end);
                
                // 创建分页响应
                Map<String, Object> response = new HashMap<>();
                response.put("content", pagedPatients);
                response.put("totalElements", allPatients.size());
                response.put("totalPages", (int) Math.ceil((double) allPatients.size() / size));
                response.put("size", size);
                response.put("number", page);
                
                return ResponseEntity.ok(response);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的严重程度参数"));
            }
        } else {
            // 原有的搜索逻辑
            if (search != null && !search.trim().isEmpty()) {
                patients = patientService.searchPatients(search, pageable);
            } else {
                patients = patientService.searchPatients("", pageable);
            }
            return ResponseEntity.ok(patients);
        }
    }

    @GetMapping("/my-patients")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getMyPatients(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        String username = authentication.getName();
        User doctor = userService.findByUsername(username).orElse(null);
        
        if (doctor == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Patient> patients = patientService.findPatientsForDoctor(doctor, pageable);
        
        return ResponseEntity.ok(patients);
    }

    /**
     * 医生查看分配给自己的病人的最新病情信息（去重显示）
     */
    @GetMapping("/my-patients-latest")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getMyPatientsLatest(Authentication authentication) {
        try {
            String username = authentication.getName();
            User doctor = userService.findByUsername(username).orElse(null);
            
            if (doctor == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "医生信息不存在"));
            }
            
            // 获取分配给该医生的所有病人记录
            List<Patient> allPatients = patientService.findPatientsForDoctor(doctor);
            
            // 按用户分组，每个用户只显示最新的病情记录
            Map<Long, Patient> latestPatients = new HashMap<>();
            for (Patient patient : allPatients) {
                Long userId = patient.getUser().getId();
                if (!latestPatients.containsKey(userId) || 
                    patient.getCreatedAt().isAfter(latestPatients.get(userId).getCreatedAt())) {
                    latestPatients.put(userId, patient);
                }
            }
            
            // 按创建时间倒序排列
            List<Patient> result = latestPatients.values().stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .toList();
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 医生查看特定病人的所有病情历史记录
     */
    @GetMapping("/patient-history/{userId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getPatientHistory(@PathVariable Long userId, Authentication authentication) {
        try {
            String username = authentication.getName();
            User doctor = userService.findByUsername(username).orElse(null);
            
            if (doctor == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "医生信息不存在"));
            }
            
            // 获取该用户的所有病情记录
            List<Patient> patientHistory = patientService.findPatientHistoryByUserId(userId);
            
            // 验证是否有记录分配给当前医生
            boolean hasAssignedRecord = patientHistory.stream()
                .anyMatch(p -> p.getAssignedDoctor() != null && 
                              p.getAssignedDoctor().getId().equals(doctor.getId()));
            
            if (!hasAssignedRecord) {
                return ResponseEntity.badRequest().body(Map.of("error", "您无权查看该病人的信息"));
            }
            
            return ResponseEntity.ok(patientHistory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/waiting")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getWaitingPatients() {
        List<Patient> waitingPatients = patientService.findWaitingPatientsByPriority();
        return ResponseEntity.ok(waitingPatients);
    }

    @GetMapping("/unassigned")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUnassignedPatients() {
        List<Patient> unassignedPatients = patientService.findUnassignedPatients();
        return ResponseEntity.ok(unassignedPatients);
    }

    @GetMapping("/by-severity/{severity}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getPatientsBySeverity(@PathVariable SeverityLevel severity) {
        List<Patient> patients = patientService.findPatientsBySeverity(severity);
        return ResponseEntity.ok(patients);
    }

    @PostMapping("/{patientId}/assign-doctor/{doctorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignDoctor(@PathVariable Long patientId, @PathVariable Long doctorId) {
        try {
            patientService.assignDoctor(patientId, doctorId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "医生分配成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PutMapping("/{patientId}/status")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> updatePatientStatus(@PathVariable Long patientId, @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            patientService.updatePatientStatus(patientId, status);
            Map<String, String> response = new HashMap<>();
            response.put("message", "状态更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 医生仪表盘 - 获取统计信息
     */
    @GetMapping("/doctor-dashboard")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getDoctorDashboard(Authentication authentication) {
        try {
            String username = authentication.getName();
            User doctor = userService.findByUsername(username).orElse(null);
            
            if (doctor == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "医生信息不存在"));
            }
            
            // 获取分配给该医生的所有病人记录
            List<Patient> allPatients = patientService.findPatientsForDoctor(doctor);
            
            // 统计信息
            Map<Long, Patient> uniquePatients = new HashMap<>();
            int totalSubmissions = allPatients.size();
            int waitingCount = 0;
            int scheduledCount = 0;
            int criticalCount = 0;
            int severeCount = 0;
            
            for (Patient patient : allPatients) {
                Long userId = patient.getUser().getId();
                // 统计每个用户的最新记录
                if (!uniquePatients.containsKey(userId) || 
                    patient.getCreatedAt().isAfter(uniquePatients.get(userId).getCreatedAt())) {
                    uniquePatients.put(userId, patient);
                }
                
                // 状态统计
                if ("WAITING".equals(patient.getStatus())) {
                    waitingCount++;
                } else if ("SCHEDULED".equals(patient.getStatus())) {
                    scheduledCount++;
                }
                
                // 严重程度统计
                if (SeverityLevel.EMERGENCY.equals(patient.getSeverityLevel())) {
                    criticalCount++;
                } else if (SeverityLevel.URGENT.equals(patient.getSeverityLevel())) {
                    severeCount++;
                }
            }
            
            // 获取最近的病情提交（最新5条）
            List<Patient> recentSubmissions = allPatients.stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .limit(5)
                .toList();
            
            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("totalPatients", uniquePatients.size());
            dashboard.put("totalSubmissions", totalSubmissions);
            dashboard.put("waitingCount", waitingCount);
            dashboard.put("scheduledCount", scheduledCount);
            dashboard.put("criticalCount", criticalCount);
            dashboard.put("severeCount", severeCount);
            dashboard.put("recentSubmissions", recentSubmissions);
            dashboard.put("needsAttention", criticalCount + severeCount);
            
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

} 