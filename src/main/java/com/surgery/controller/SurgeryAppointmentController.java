package com.surgery.controller;

import com.surgery.entity.SurgeryAppointment;
import com.surgery.entity.User;
import com.surgery.entity.Patient;
import com.surgery.entity.OperatingRoom;
import com.surgery.entity.OperatingBed;
import com.surgery.enums.AppointmentStatus;
import com.surgery.enums.UserRole;
import com.surgery.service.ISurgeryAppointmentService;
import com.surgery.service.IUserService;
import com.surgery.service.IPatientService;
import com.surgery.repository.OperatingRoomRepository;
import com.surgery.repository.OperatingBedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SurgeryAppointmentController {

    private final ISurgeryAppointmentService appointmentService;
    private final IUserService userService;
    private final IPatientService patientService;
    private final OperatingRoomRepository roomRepository;
    private final OperatingBedRepository bedRepository;

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> createAppointment(@Valid @RequestBody SurgeryAppointment appointment, Authentication authentication) {
        try {
            // 设置创建者
            String username = authentication.getName();
            User creator = userService.findByUsername(username).orElse(null);
            if (creator != null) {
                appointment.setCreatedBy(creator);
            }
            
            SurgeryAppointment savedAppointment = appointmentService.createAppointment(appointment);
            return ResponseEntity.ok(savedAppointment);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE') or hasRole('ANESTHESIOLOGIST') or hasRole('ADMIN')")
    public ResponseEntity<?> getAppointment(@PathVariable Long id) {
        try {
            Optional<SurgeryAppointment> appointmentOpt = appointmentService.findById(id);
            if (appointmentOpt.isPresent()) {
                SurgeryAppointment appointment = appointmentOpt.get();
                
                // 添加调试日志
                System.out.println("获取预约详情 - ID: " + id);
                System.out.println("预约信息: " + appointment.getSurgeryName());
                System.out.println("病人信息: " + (appointment.getPatient() != null && appointment.getPatient().getUser() != null ? 
                                 appointment.getPatient().getUser().getRealName() : "未知"));
                System.out.println("状态: " + appointment.getStatus());
                
                // 添加缓存控制头，防止304问题
                return ResponseEntity.ok()
                        .header("Cache-Control", "no-cache, no-store, must-revalidate")
                        .header("Pragma", "no-cache")
                        .header("Expires", "0")
                        .body(appointment);
            } else {
                System.out.println("未找到预约 - ID: " + id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("获取预约详情失败 - ID: " + id + ", 错误: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取预约详情失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> updateAppointment(@PathVariable Long id, @Valid @RequestBody SurgeryAppointment appointmentDetails) {
        try {
            SurgeryAppointment updatedAppointment = appointmentService.updateAppointment(id, appointmentDetails);
            return ResponseEntity.ok(updatedAppointment);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE') or hasRole('ANESTHESIOLOGIST')")
    public ResponseEntity<?> confirmAppointment(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().build();
            }
            
            appointmentService.confirmAppointment(id, user.getId());
            Map<String, String> response = new HashMap<>();
            response.put("message", "预约确认成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/{id}/send-confirmation")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> sendConfirmationToTeam(@PathVariable Long id) {
        try {
            appointmentService.sendConfirmationToTeam(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/notify-patient")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> notifyPatient(@PathVariable Long id) {
        try {
            appointmentService.notifyPatient(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 医疗团队成员确认手术预约
     */
    @PostMapping("/{id}/confirm-by-team")
    @PreAuthorize("hasRole('NURSE') or hasRole('ANESTHESIOLOGIST')")
    public ResponseEntity<?> confirmByTeamMember(@PathVariable Long id, Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            appointmentService.confirmByTeamMember(id, currentUser.getId());
            return ResponseEntity.ok(Map.of("message", "确认成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 医生最终确认手术预约
     */
    @PostMapping("/{id}/doctor-final-confirm")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> doctorFinalConfirm(@PathVariable Long id, Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            appointmentService.doctorFinalConfirm(id, currentUser.getId());
            return ResponseEntity.ok(Map.of("message", "最终确认成功，已发送手术通知单给病人"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> startSurgery(@PathVariable Long id) {
        try {
            appointmentService.startSurgery(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> completeSurgery(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String postSurgeryNotes = request.get("postSurgeryNotes");
            appointmentService.completeSurgery(id, postSurgeryNotes);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long id, @RequestBody Map<String, String> request, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().build();
            }
            
            String reason = request.get("reason");
            appointmentService.cancelAppointment(id, user.getId(), reason);
            Map<String, String> response = new HashMap<>();
            response.put("message", "预约取消成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE') or hasRole('ANESTHESIOLOGIST') or hasRole('ADMIN')")
    public ResponseEntity<?> getAllAppointments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "plannedDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) List<AppointmentStatus> statuses,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<SurgeryAppointment> appointments;
            
            // 如果有搜索条件，使用搜索方法
            if (patientName != null || status != null || startDate != null || endDate != null) {
                appointments = appointmentService.searchAppointments(
                    patientName, status, startDate, endDate, pageable);
            } else if (statuses != null && !statuses.isEmpty()) {
                appointments = appointmentService.findAppointmentsByStatus(statuses, pageable);
            } else {
                appointments = appointmentService.findAppointmentsByStatus(Arrays.asList(AppointmentStatus.values()), pageable);
            }
            
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-appointments")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getMyAppointments(Authentication authentication) {
        String username = authentication.getName();
        User doctor = userService.findByUsername(username).orElse(null);
        
        if (doctor == null) {
            return ResponseEntity.badRequest().build();
        }
        
        List<SurgeryAppointment> appointments = appointmentService.findAppointmentsForDoctor(doctor);
        
        // 添加调试日志
        System.out.println("医生 " + doctor.getRealName() + " 的预约数量: " + appointments.size());
        for (SurgeryAppointment apt : appointments) {
            System.out.println("预约ID: " + apt.getId() + ", 手术名称: " + apt.getSurgeryName() + 
                             ", 病人: " + (apt.getPatient() != null && apt.getPatient().getUser() != null ? 
                                         apt.getPatient().getUser().getRealName() : "未知"));
        }
        
        // 添加缓存控制头，防止304问题
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(appointments);
    }

    @GetMapping("/patient/my-appointments")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> getPatientAppointments(Authentication authentication) {
        String username = authentication.getName();
        User patient = userService.findByUsername(username).orElse(null);
        
        if (patient == null) {
            return ResponseEntity.badRequest().build();
        }
        
        List<SurgeryAppointment> appointments = appointmentService.findAppointmentsForPatient(patient.getId());
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/patient/upcoming")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<?> getPatientUpcomingAppointments(Authentication authentication) {
        String username = authentication.getName();
        User patient = userService.findByUsername(username).orElse(null);
        
        if (patient == null) {
            return ResponseEntity.badRequest().build();
        }
        
        List<SurgeryAppointment> appointments = appointmentService.findUpcomingAppointmentsForPatient(patient.getId());
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE') or hasRole('ANESTHESIOLOGIST') or hasRole('ADMIN')")
    public ResponseEntity<?> getAppointmentsForDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<SurgeryAppointment> appointments = appointmentService.findAppointmentsForDate(date);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/week/{startDate}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE') or hasRole('ANESTHESIOLOGIST') or hasRole('ADMIN')")
    public ResponseEntity<?> getAppointmentsForWeek(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        List<SurgeryAppointment> appointments = appointmentService.findAppointmentsForWeek(startDate);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/today")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE') or hasRole('ANESTHESIOLOGIST') or hasRole('ADMIN')")
    public ResponseEntity<?> getTodayAppointments(Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username).orElse(null);
            
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户信息不存在"));
            }
            
            LocalDate today = LocalDate.now();
            List<SurgeryAppointment> appointments;
            
            // 根据用户角色返回不同的数据
            if (currentUser.getRole() == UserRole.ADMIN) {
                // 管理员看到所有今日手术（包括已完成的）
                appointments = appointmentService.findAppointmentsForDate(today);
            } else if (currentUser.getRole() == UserRole.DOCTOR) {
                // 医生只看到自己负责的今日手术（包括已完成的）
                appointments = appointmentService.findAppointmentsForDate(today).stream()
                        .filter(appointment -> appointment.getDoctor() != null && 
                                             appointment.getDoctor().getId().equals(currentUser.getId()))
                        .collect(Collectors.toList());
            } else if (currentUser.getRole() == UserRole.NURSE) {
                // 护士只看到分配给自己的今日手术（包括已完成的）
                appointments = appointmentService.findAppointmentsForDate(today).stream()
                        .filter(appointment -> appointment.getNurse() != null && 
                                             appointment.getNurse().getId().equals(currentUser.getId()))
                        .collect(Collectors.toList());
            } else if (currentUser.getRole() == UserRole.ANESTHESIOLOGIST) {
                // 麻醉师只看到分配给自己的今日手术（包括已完成的）
                appointments = appointmentService.findAppointmentsForDate(today).stream()
                        .filter(appointment -> appointment.getAnesthesiologist() != null && 
                                             appointment.getAnesthesiologist().getId().equals(currentUser.getId()))
                        .collect(Collectors.toList());
            } else {
                appointments = appointmentService.findAppointmentsForDate(today);
            }
            
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pending-confirmations")
    @PreAuthorize("hasRole('NURSE') or hasRole('ANESTHESIOLOGIST')")
    public ResponseEntity<?> getPendingConfirmations(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username).orElse(null);
        
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        
        LocalDate today = LocalDate.now();
        List<SurgeryAppointment> appointments = appointmentService.findPendingConfirmationsForStaff(user, today);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/room/{roomId}/date/{date}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getRoomSchedule(@PathVariable Long roomId, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<SurgeryAppointment> appointments = appointmentService.findRoomScheduleForDate(roomId, date);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/bed/{bedId}/date/{date}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getBedSchedule(@PathVariable Long bedId, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<SurgeryAppointment> appointments = appointmentService.findBedScheduleForDate(bedId, date);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/room/{roomId}/beds/availability/{date}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getRoomBedsAvailability(@PathVariable Long roomId, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            // 获取手术室的所有床位
            List<OperatingBed> beds = bedRepository.findByRoomId(roomId);
            
            // 为每个床位获取预约信息
            List<Map<String, Object>> bedAvailabilities = new ArrayList<>();
            for (OperatingBed bed : beds) {
                List<SurgeryAppointment> appointments = appointmentService.findBedScheduleForDate(bed.getId(), date);
                
                Map<String, Object> bedAvailability = new HashMap<>();
                bedAvailability.put("bed", bed);
                bedAvailability.put("appointments", appointments);
                bedAvailabilities.add(bedAvailability);
            }
            
            return ResponseEntity.ok(bedAvailabilities);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 这里可以添加统计数据的计算逻辑
        // 例如：今日手术数量、本周手术数量、各状态手术数量等
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * 获取创建预约所需的选项数据
     */
    @GetMapping("/create-options")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getCreateOptions(Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username).orElse(null);
            
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户信息不存在"));
            }
            
            Map<String, Object> options = new HashMap<>();
            
            // 如果是医生，获取分配给自己的病人
            if (currentUser.getRole() == UserRole.DOCTOR) {
                List<Patient> myPatients = patientService.findPatientsForDoctor(currentUser);
                // 按用户分组，每个用户只显示最新的病情记录
                Map<Long, Patient> latestPatients = new HashMap<>();
                for (Patient patient : myPatients) {
                    Long userId = patient.getUser().getId();
                    if (!latestPatients.containsKey(userId) || 
                        patient.getCreatedAt().isAfter(latestPatients.get(userId).getCreatedAt())) {
                        latestPatients.put(userId, patient);
                    }
                }
                options.put("patients", new ArrayList<>(latestPatients.values()));
            } else {
                // 管理员可以看到所有病人
                options.put("patients", patientService.findWaitingPatients());
            }
            
            // 获取麻醉师和护士
            List<User> anesthesiologists = userService.findByRole(UserRole.ANESTHESIOLOGIST);
            List<User> nurses = userService.findByRole(UserRole.NURSE);
            
            options.put("anesthesiologists", anesthesiologists);
            options.put("nurses", nurses);
            
            // 获取手术室信息
            List<OperatingRoom> rooms = roomRepository.findAvailableRooms();
            options.put("rooms", rooms);
            
            return ResponseEntity.ok(options);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取手术室的可用床位
     */
    @GetMapping("/rooms/{roomId}/beds")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getRoomBeds(@PathVariable Long roomId) {
        try {
            List<OperatingBed> beds = bedRepository.findAvailableBedsByRoom(roomId);
            return ResponseEntity.ok(beds);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取管理员统计数据
     */
    @GetMapping("/admin-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAdminStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 获取用户统计
            long totalUsers = userService.countAllUsers();
            long doctorCount = userService.countUsersByRole(UserRole.DOCTOR);
            long patientCount = userService.countUsersByRole(UserRole.PATIENT);
            long nurseCount = userService.countUsersByRole(UserRole.NURSE);
            long anesthesiologistCount = userService.countUsersByRole(UserRole.ANESTHESIOLOGIST);
            long adminCount = userService.countUsersByRole(UserRole.ADMIN);
            
            // 获取预约统计
            long totalAppointments = appointmentService.countAllAppointments();
            long todayAppointments = appointmentService.countTodayAppointments();
            
            // 获取手术室统计
            long totalRooms = roomRepository.count();
            
            stats.put("totalUsers", totalUsers);
            stats.put("todayAppointments", todayAppointments);
            stats.put("totalAppointments", totalAppointments);
            stats.put("totalRooms", totalRooms);
            stats.put("doctorCount", doctorCount);
            stats.put("patientCount", patientCount);
            stats.put("nurseCount", nurseCount);
            stats.put("anesthesiologistCount", anesthesiologistCount);
            stats.put("adminCount", adminCount);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取医护人员仪表盘统计数据
     */
    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE') or hasRole('ANESTHESIOLOGIST')")
    public ResponseEntity<?> getDashboardStats(Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username).orElse(null);
            
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户信息不存在"));
            }
            
            Map<String, Object> stats = new HashMap<>();
            LocalDate today = LocalDate.now();
            
            // 获取病人总数
            long totalPatients = userService.countUsersByRole(UserRole.PATIENT);
            
            // 根据角色获取不同的统计数据
            if (currentUser.getRole() == UserRole.DOCTOR) {
                // 医生：获取今日手术、已完成、待确认的统计
                long todayAppointments = appointmentService.countTodayAppointmentsForDoctor(currentUser.getId());
                long completedAppointments = appointmentService.countCompletedAppointmentsForDoctor(currentUser.getId(), today);
                long pendingAppointments = appointmentService.countPendingAppointmentsForDoctor(currentUser.getId());
                
                stats.put("todayAppointments", todayAppointments);
                stats.put("completedAppointments", completedAppointments);
                stats.put("pendingAppointments", pendingAppointments);
                stats.put("totalPatients", totalPatients);
            } else if (currentUser.getRole() == UserRole.NURSE || currentUser.getRole() == UserRole.ANESTHESIOLOGIST) {
                // 护士/麻醉师：获取今日参与的手术、已完成、待确认的统计
                long todayAppointments = appointmentService.countTodayAppointmentsForStaff(currentUser.getId(), currentUser.getRole());
                long completedAppointments = appointmentService.countCompletedAppointmentsForStaff(currentUser.getId(), currentUser.getRole(), today);
                long pendingAppointments = appointmentService.countPendingAppointmentsForStaff(currentUser.getId(), currentUser.getRole());
                
                stats.put("todayAppointments", todayAppointments);
                stats.put("completedAppointments", completedAppointments);
                stats.put("pendingAppointments", pendingAppointments);
                stats.put("totalPatients", totalPatients);
            }
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取护士和麻醉师参与的所有手术预约
     */
    @GetMapping("/my-staff-appointments")
    @PreAuthorize("hasRole('NURSE') or hasRole('ANESTHESIOLOGIST')")
    public ResponseEntity<?> getMyStaffAppointments(Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username).orElse(null);
            
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户信息不存在"));
            }
            
            List<SurgeryAppointment> appointments;
            
            if (currentUser.getRole() == UserRole.NURSE) {
                // 护士：获取分配给自己的所有手术
                appointments = appointmentService.findAppointmentsForNurse(currentUser);
            } else if (currentUser.getRole() == UserRole.ANESTHESIOLOGIST) {
                // 麻醉师：获取分配给自己的所有手术
                appointments = appointmentService.findAppointmentsForAnesthesiologist(currentUser);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的用户角色"));
            }
            
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除已取消的预约（仅限医生）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteAppointment(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username).orElse(null);
            
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户信息不存在"));
            }
            
            // 获取预约信息
            SurgeryAppointment appointment = appointmentService.findById(id)
                    .orElseThrow(() -> new RuntimeException("预约不存在"));
            
            // 检查权限：医生只能删除自己的预约，管理员可以删除任何预约
            if (currentUser.getRole() == UserRole.DOCTOR) {
                if (!appointment.getDoctor().getId().equals(currentUser.getId())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "您只能删除自己创建的预约"));
                }
            }
            
            // 只能删除已取消的预约
            if (appointment.getStatus() != AppointmentStatus.CANCELLED) {
                return ResponseEntity.badRequest().body(Map.of("error", "只能删除已取消的预约"));
            }
            
            // 执行删除
            appointmentService.deleteAppointment(id);
            
            return ResponseEntity.ok(Map.of("message", "预约删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
} 