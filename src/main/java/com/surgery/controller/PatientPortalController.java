package com.surgery.controller;

import com.surgery.entity.*;
import com.surgery.repository.CancellationRecordRepository;
import com.surgery.repository.BlacklistRepository;
import com.surgery.service.*;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/patient-portal")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PATIENT')")
public class PatientPortalController {

    private final IPatientService patientService;
    private final IUserService userService;
    private final ISurgeryAppointmentService appointmentService;
    private final INotificationService notificationService;
    private final CancellationRecordRepository cancellationRecordRepository;
    private final BlacklistRepository blacklistRepository;

    /**
     * 病人提交自身基本病情信息
     */
    @PostMapping("/submit-info")
    public ResponseEntity<?> submitPatientInfo(@Valid @RequestBody Patient patient, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElseThrow();
            
            // 检查是否在黑名单中
            if (blacklistRepository.isUserBlacklisted(user)) {
                return ResponseEntity.badRequest().body(Map.of("error", "您已被列入黑名单，无法提交病情信息"));
            }
            
            patient.setUser(user);
            Patient savedPatient = patientService.createPatient(patient);
            
            return ResponseEntity.ok(Map.of(
                "message", "病情信息提交成功，已为您随机分配医生"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 查看自己的病情信息（最新的一次）
     */
    @GetMapping("/my-info")
    public ResponseEntity<?> getMyPatientInfo(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElseThrow();
            
            Optional<Patient> patient = patientService.findLatestPatientByUserId(user.getId());
            if (patient.isPresent()) {
                return ResponseEntity.ok(patient.get());
            } else {
                return ResponseEntity.ok(Map.of("message", "您还未提交病情信息"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 查看自己的病情历史记录
     */
    @GetMapping("/my-history")
    public ResponseEntity<?> getMyPatientHistory(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElseThrow();
            
            List<Patient> patientHistory = patientService.findPatientHistoryByUserId(user.getId());
            return ResponseEntity.ok(patientHistory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 查看自己的手术预约
     */
    @GetMapping("/my-appointments")
    public ResponseEntity<?> getMyAppointments(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElseThrow();
            
            // 获取用户所有的病人记录
            List<Patient> patientHistory = patientService.findPatientHistoryByUserId(user.getId());
            if (patientHistory.isEmpty()) {
                // 如果用户还没有提交病情信息，返回空的预约列表而不是错误
                return ResponseEntity.ok(List.of());
            }
            
            // 获取所有病人记录对应的预约
            List<SurgeryAppointment> allAppointments = new ArrayList<>();
            for (Patient patient : patientHistory) {
                List<SurgeryAppointment> appointments = appointmentService.findAppointmentsForPatient(patient.getId());
                allAppointments.addAll(appointments);
            }
            
            // 按预约日期和时间排序（最新的在前）
            allAppointments.sort((a, b) -> {
                int dateCompare = b.getPlannedDate().compareTo(a.getPlannedDate());
                if (dateCompare != 0) {
                    return dateCompare;
                }
                return b.getPlannedStartTime().compareTo(a.getPlannedStartTime());
            });
            
            return ResponseEntity.ok(allAppointments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 取消手术预约
     */
    @PostMapping("/cancel-appointment/{appointmentId}")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long appointmentId, 
                                               @RequestBody Map<String, String> request, 
                                               Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElseThrow();
            
            String reason = request.get("reason");
            appointmentService.cancelAppointment(appointmentId, user.getId(), reason);
            
            return ResponseEntity.ok(Map.of("message", "预约取消成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 查看通知消息
     */
    @GetMapping("/notifications")
    public ResponseEntity<?> getMyNotifications(Authentication authentication,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElseThrow();
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Notification> notifications = notificationService.findNotificationsForUser(user, pageable);
            
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/notifications/{notificationId}/read")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Long notificationId, 
                                                    Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElseThrow();
            
            notificationService.markAsRead(notificationId, user.getId());
            return ResponseEntity.ok(Map.of("message", "通知已标记为已读"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 查看未读通知数量
     */
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<?> getUnreadNotificationCount(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElseThrow();
            
            long unreadCount = notificationService.countUnreadNotifications(user);
            return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 查看本月取消次数和黑名单状态
     */
    @GetMapping("/cancellation-status")
    public ResponseEntity<?> getCancellationStatus(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElseThrow();
            
            LocalDate now = LocalDate.now();
            Long thisMonthCancellations = cancellationRecordRepository.countByUserAndMonth(
                    user, now.getYear(), now.getMonthValue());
            
            boolean isBlacklisted = blacklistRepository.isUserBlacklisted(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("thisMonthCancellations", thisMonthCancellations);
            response.put("remainingCancellations", Math.max(0, 1 - thisMonthCancellations));
            response.put("isBlacklisted", isBlacklisted);
            
            if (isBlacklisted) {
                Optional<Blacklist> blacklist = blacklistRepository.findActiveBlacklistByUser(user, now);
                if (blacklist.isPresent()) {
                    response.put("blacklistReason", blacklist.get().getReason());
                    response.put("blacklistEndDate", blacklist.get().getBlacklistEndDate());
                }
                response.put("message", "您已被列入黑名单，无法进行预约操作");
            } else {
                response.put("message", thisMonthCancellations >= 1 ? 
                    "本月已取消1次，无法再次取消" : "本月还可取消1次预约");
                response.put("note", "请注意：取消预约需提前48小时，否则将被列入黑名单1个月");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
} 