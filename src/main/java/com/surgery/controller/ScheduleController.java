package com.surgery.controller;

import com.surgery.entity.User;
import com.surgery.entity.SurgeryAppointment;
import com.surgery.enums.UserRole;
import com.surgery.service.IUserService;
import com.surgery.service.ISurgeryAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schedule")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ScheduleController {

    private final IUserService userService;
    private final ISurgeryAppointmentService appointmentService;

    /**
     * 获取医护人员列表（医生查看麻醉师和护士）
     */
    @GetMapping("/staff")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getStaffList() {
        try {
            List<User> anesthesiologists = userService.findByRole(UserRole.ANESTHESIOLOGIST);
            List<User> nurses = userService.findByRole(UserRole.NURSE);
            
            Map<String, Object> result = new HashMap<>();
            result.put("anesthesiologists", anesthesiologists);
            result.put("nurses", nurses);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取特定医护人员的基本信息
     */
    @GetMapping("/staff/{userId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getStaffInfo(@PathVariable Long userId) {
        try {
            Optional<User> user = userService.findById(userId);
            if (user.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User staff = user.get();
            if (staff.getRole() != UserRole.ANESTHESIOLOGIST && staff.getRole() != UserRole.NURSE) {
                return ResponseEntity.badRequest().body(Map.of("error", "只能查看麻醉师和护士信息"));
            }
            
            return ResponseEntity.ok(staff);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取特定医护人员某天的排班和空闲时段
     */
    @GetMapping("/staff/{userId}/availability/{date}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getStaffAvailability(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            Optional<User> user = userService.findById(userId);
            if (user.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User staff = user.get();
            if (staff.getRole() != UserRole.ANESTHESIOLOGIST && staff.getRole() != UserRole.NURSE) {
                return ResponseEntity.badRequest().body(Map.of("error", "只能查看麻醉师和护士信息"));
            }
            
            // 获取该医护人员在指定日期的所有手术预约
            List<SurgeryAppointment> appointments = getStaffAppointmentsForDate(staff, date);
            
            // 计算空闲时段
            List<Map<String, Object>> freeSlots = calculateFreeSlots(appointments, date);
            
            Map<String, Object> result = new HashMap<>();
            result.put("staff", staff);
            result.put("date", date);
            result.put("appointments", appointments);
            result.put("freeSlots", freeSlots);
            result.put("totalAppointments", appointments.size());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取特定医护人员一周内的空闲时段
     */
    @GetMapping("/staff/{userId}/weekly-availability")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getStaffWeeklyAvailability(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        try {
            Optional<User> user = userService.findById(userId);
            if (user.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User staff = user.get();
            if (staff.getRole() != UserRole.ANESTHESIOLOGIST && staff.getRole() != UserRole.NURSE) {
                return ResponseEntity.badRequest().body(Map.of("error", "只能查看麻醉师和护士信息"));
            }
            
            List<Map<String, Object>> weeklySchedule = new ArrayList<>();
            
            // 计算一周内每天的空闲时段
            for (int i = 0; i < 7; i++) {
                LocalDate date = startDate.plusDays(i);
                List<SurgeryAppointment> appointments = getStaffAppointmentsForDate(staff, date);
                List<Map<String, Object>> freeSlots = calculateFreeSlots(appointments, date);
                
                Map<String, Object> daySchedule = new HashMap<>();
                daySchedule.put("date", date);
                daySchedule.put("dayOfWeek", date.getDayOfWeek().getValue());
                daySchedule.put("appointments", appointments);
                daySchedule.put("freeSlots", freeSlots);
                daySchedule.put("totalAppointments", appointments.size());
                daySchedule.put("availableHours", calculateAvailableHours(freeSlots));
                
                weeklySchedule.add(daySchedule);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("staff", staff);
            result.put("startDate", startDate);
            result.put("endDate", startDate.plusDays(6));
            result.put("weeklySchedule", weeklySchedule);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取医护人员在指定日期的手术预约
     */
    private List<SurgeryAppointment> getStaffAppointmentsForDate(User staff, LocalDate date) {
        List<SurgeryAppointment> allAppointments = appointmentService.findAppointmentsForDate(date);
        
        return allAppointments.stream()
                .filter(appointment -> {
                    if (staff.getRole() == UserRole.ANESTHESIOLOGIST) {
                        return appointment.getAnesthesiologist() != null && 
                               appointment.getAnesthesiologist().getId().equals(staff.getId());
                    } else if (staff.getRole() == UserRole.NURSE) {
                        return appointment.getNurse() != null && 
                               appointment.getNurse().getId().equals(staff.getId());
                    }
                    return false;
                })
                .sorted(Comparator.comparing(SurgeryAppointment::getPlannedStartTime))
                .collect(Collectors.toList());
    }

    /**
     * 计算空闲时段
     */
    private List<Map<String, Object>> calculateFreeSlots(List<SurgeryAppointment> appointments, LocalDate date) {
        List<Map<String, Object>> freeSlots = new ArrayList<>();
        
        // 工作时间：8:00 - 18:00
        LocalTime workStartTime = LocalTime.of(8, 0);
        LocalTime workEndTime = LocalTime.of(18, 0);
        
        if (appointments.isEmpty()) {
            // 没有预约，整天空闲
            Map<String, Object> freeSlot = new HashMap<>();
            freeSlot.put("startTime", workStartTime);
            freeSlot.put("endTime", workEndTime);
            freeSlot.put("duration", 600); // 10小时 = 600分钟
            freeSlot.put("type", "全天空闲");
            freeSlots.add(freeSlot);
            return freeSlots;
        }
        
        LocalTime currentTime = workStartTime;
        
        for (SurgeryAppointment appointment : appointments) {
            LocalTime appointmentStart = appointment.getPlannedStartTime();
            LocalTime appointmentEnd = appointment.getPlannedEndTime();
            
            // 如果当前时间早于预约开始时间，说明有空闲时段
            if (currentTime.isBefore(appointmentStart)) {
                Map<String, Object> freeSlot = new HashMap<>();
                freeSlot.put("startTime", currentTime);
                freeSlot.put("endTime", appointmentStart);
                freeSlot.put("duration", calculateMinutes(currentTime, appointmentStart));
                freeSlot.put("type", "空闲");
                freeSlots.add(freeSlot);
            }
            
            // 更新当前时间为预约结束时间
            currentTime = appointmentEnd.isAfter(currentTime) ? appointmentEnd : currentTime;
        }
        
        // 检查最后一个预约后是否还有空闲时间
        if (currentTime.isBefore(workEndTime)) {
            Map<String, Object> freeSlot = new HashMap<>();
            freeSlot.put("startTime", currentTime);
            freeSlot.put("endTime", workEndTime);
            freeSlot.put("duration", calculateMinutes(currentTime, workEndTime));
            freeSlot.put("type", "空闲");
            freeSlots.add(freeSlot);
        }
        
        return freeSlots;
    }

    /**
     * 计算两个时间之间的分钟数
     */
    private int calculateMinutes(LocalTime start, LocalTime end) {
        return (int) java.time.Duration.between(start, end).toMinutes();
    }

    /**
     * 计算可用小时数
     */
    private double calculateAvailableHours(List<Map<String, Object>> freeSlots) {
        int totalMinutes = freeSlots.stream()
                .mapToInt(slot -> (Integer) slot.get("duration"))
                .sum();
        return totalMinutes / 60.0;
    }
} 