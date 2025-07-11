package com.surgery.service;

import com.surgery.entity.*;
import com.surgery.enums.AppointmentStatus;
import com.surgery.enums.UserRole;
import com.surgery.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SurgeryAppointmentService implements ISurgeryAppointmentService {

    private final SurgeryAppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final OperatingRoomRepository roomRepository;
    private final OperatingBedRepository bedRepository;
    private final INotificationService notificationService;
    private final BlacklistRepository blacklistRepository;
    private final CancellationRecordRepository cancellationRecordRepository;
    private final UserRepository userRepository;

    @Override
    public SurgeryAppointment createAppointment(SurgeryAppointment appointment) {
        // 验证时间冲突
        validateTimeConflict(appointment);
        
        // 验证资源可用性
        validateResourceAvailability(appointment);
        
        // 保存预约
        SurgeryAppointment savedAppointment = appointmentRepository.save(appointment);
        
        // 更新病人状态 - 使用正确的方法获取完整的Patient对象
        Long patientId = savedAppointment.getPatient().getId();
        Optional<Patient> patientOpt = patientRepository.findByIdWithUser(patientId);
        if (patientOpt.isPresent()) {
            Patient patient = patientOpt.get();
            patient.setStatus("SCHEDULED");
            patientRepository.save(patient);
        }
        
        // 发送通知
        sendAppointmentNotifications(savedAppointment);
        
        return savedAppointment;
    }

    @Override
    public SurgeryAppointment updateAppointment(Long id, SurgeryAppointment appointmentDetails) {
        SurgeryAppointment appointment = appointmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("手术预约不存在"));
        
        // 验证是否可以修改
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new RuntimeException("已完成的手术不能修改");
        }
        
        // 更新字段
        appointment.setSurgeryName(appointmentDetails.getSurgeryName());
        appointment.setSurgeryType(appointmentDetails.getSurgeryType());
        appointment.setPlannedDate(appointmentDetails.getPlannedDate());
        appointment.setPlannedStartTime(appointmentDetails.getPlannedStartTime());
        appointment.setPlannedEndTime(appointmentDetails.getPlannedEndTime());
        appointment.setEstimatedDuration(appointmentDetails.getEstimatedDuration());
        appointment.setSurgeryDescription(appointmentDetails.getSurgeryDescription());
        appointment.setPreSurgeryNotes(appointmentDetails.getPreSurgeryNotes());
        appointment.setPriorityLevel(appointmentDetails.getPriorityLevel());
        
        // 如果更新了人员分配
        if (appointmentDetails.getAnesthesiologist() != null) {
            appointment.setAnesthesiologist(appointmentDetails.getAnesthesiologist());
        }
        if (appointmentDetails.getNurse() != null) {
            appointment.setNurse(appointmentDetails.getNurse());
        }
        if (appointmentDetails.getRoom() != null) {
            appointment.setRoom(appointmentDetails.getRoom());
        }
        if (appointmentDetails.getBed() != null) {
            appointment.setBed(appointmentDetails.getBed());
        }
        
        SurgeryAppointment savedAppointment = appointmentRepository.save(appointment);
        
        // 发送更新通知
        notificationService.sendSurgeryUpdatedNotification(savedAppointment);
        
        return savedAppointment;
    }

    @Override
    public void confirmAppointment(Long appointmentId, Long userId) {
        SurgeryAppointment appointment = appointmentRepository.findByIdWithDetails(appointmentId)
                .orElseThrow(() -> new RuntimeException("手术预约不存在"));
        
        // 检查是否有权限确认
        if (!canConfirmAppointment(appointment, userId)) {
            throw new RuntimeException("无权限确认此预约");
        }
        
        appointment.setStatus(AppointmentStatus.TEAM_CONFIRMED);
        appointmentRepository.save(appointment);
        
        // 发送确认通知
        notificationService.sendSurgeryConfirmedNotification(appointment);
    }

    @Override
    @Transactional
    public void sendConfirmationToTeam(Long id) {
        SurgeryAppointment appointment = appointmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("预约不存在"));
        
        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new RuntimeException("只有已安排状态的预约才能发送确认通知");
        }
        
        // 发送通知给麻醉师和护士
        String patientName = "未知患者";
        if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
            patientName = appointment.getPatient().getUser().getRealName() != null ? 
                         appointment.getPatient().getUser().getRealName() : "未知患者";
        }
        
        if (appointment.getAnesthesiologist() != null) {
            notificationService.createNotification(
                appointment.getAnesthesiologist().getId(),
                "手术确认通知",
                String.format("您被安排为病人 %s 的手术麻醉师，手术时间：%s %s，请确认。",
                    patientName,
                    appointment.getPlannedDate(),
                    appointment.getPlannedStartTime()),
                Notification.NotificationType.CONFIRMATION
            );
        }
        
        if (appointment.getNurse() != null) {
            notificationService.createNotification(
                appointment.getNurse().getId(),
                "手术确认通知", 
                String.format("您被安排为病人 %s 的手术护士，手术时间：%s %s，请确认。",
                    patientName,
                    appointment.getPlannedDate(),
                    appointment.getPlannedStartTime()),
                Notification.NotificationType.CONFIRMATION
            );
        }
        
        // 更新状态为等待医疗团队确认
        appointment.setStatus(AppointmentStatus.PENDING_CONFIRMATION);
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public void notifyPatient(Long id) {
        SurgeryAppointment appointment = appointmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("预约不存在"));
        
        if (appointment.getStatus() != AppointmentStatus.DOCTOR_FINAL_CONFIRMED) {
            throw new RuntimeException("只有医生最终确认状态的预约才能通知病人");
        }
        
        // 发送通知给病人
        if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
            String doctorName = appointment.getDoctor() != null ? appointment.getDoctor().getRealName() : "未知医生";
        notificationService.createNotification(
            appointment.getPatient().getUser().getId(),
            "手术通知",
            String.format("您的手术已安排，手术名称：%s，手术时间：%s %s，手术室：%s，主刀医生：%s。请按时到达。",
                appointment.getSurgeryName(),
                appointment.getPlannedDate(),
                appointment.getPlannedStartTime(),
                appointment.getRoom() != null ? appointment.getRoom().getRoomName() : "待定",
                    doctorName),
            Notification.NotificationType.SURGERY_NOTIFICATION
        );
        }
        
        // 更新状态为已通知病人
        appointment.setStatus(AppointmentStatus.NOTIFIED);
        appointmentRepository.save(appointment);
    }

    /**
     * 麻醉师或护士确认手术预约
     */
    @Transactional
    public void confirmByTeamMember(Long appointmentId, Long userId) {
        SurgeryAppointment appointment = appointmentRepository.findByIdWithDetails(appointmentId)
                .orElseThrow(() -> new RuntimeException("手术预约不存在"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        if (appointment.getStatus() != AppointmentStatus.PENDING_CONFIRMATION) {
            throw new RuntimeException("预约状态不正确，无法确认");
        }
        
        boolean isAnesthesiologist = appointment.getAnesthesiologist() != null && 
                                   appointment.getAnesthesiologist().getId().equals(userId);
        boolean isNurse = appointment.getNurse() != null && 
                         appointment.getNurse().getId().equals(userId);
        
        if (!isAnesthesiologist && !isNurse) {
            throw new RuntimeException("您不是此手术的医疗团队成员，无法确认");
        }
        
        // 更新确认状态
        if (isAnesthesiologist) {
            appointment.setAnesthesiologistConfirmed(true);
            appointment.setAnesthesiologistConfirmedAt(LocalDateTime.now());
        }
        
        if (isNurse) {
            appointment.setNurseConfirmed(true);
            appointment.setNurseConfirmedAt(LocalDateTime.now());
        }
        
        // 检查是否所有团队成员都已确认
        boolean allConfirmed = true;
        if (appointment.getAnesthesiologist() != null && !appointment.getAnesthesiologistConfirmed()) {
            allConfirmed = false;
        }
        if (appointment.getNurse() != null && !appointment.getNurseConfirmed()) {
            allConfirmed = false;
        }
        
        if (allConfirmed) {
            // 所有团队成员都已确认，更新状态并通知医生
            appointment.setStatus(AppointmentStatus.TEAM_CONFIRMED);
            appointmentRepository.save(appointment);
            
            // 发送医生最终确认请求
            notificationService.sendDoctorFinalConfirmationRequest(appointment);
        } else {
            appointmentRepository.save(appointment);
        }
        
        log.info("医疗团队成员确认 - 预约ID: {}, 用户: {}, 角色: {}", 
                appointmentId, user.getRealName(), 
                isAnesthesiologist ? "麻醉师" : "护士");
    }

    /**
     * 医生最终确认手术预约
     */
    @Transactional
    public void doctorFinalConfirm(Long appointmentId, Long userId) {
        SurgeryAppointment appointment = appointmentRepository.findByIdWithDetails(appointmentId)
                .orElseThrow(() -> new RuntimeException("手术预约不存在"));
        
        if (!appointment.getDoctor().getId().equals(userId)) {
            throw new RuntimeException("只有主刀医生可以进行最终确认");
        }
        
        if (appointment.getStatus() != AppointmentStatus.TEAM_CONFIRMED) {
            throw new RuntimeException("医疗团队尚未完成确认，无法进行最终确认");
        }
        
        // 医生最终确认
        appointment.setDoctorFinalConfirmed(true);
        appointment.setDoctorFinalConfirmedAt(LocalDateTime.now());
        appointment.setStatus(AppointmentStatus.DOCTOR_FINAL_CONFIRMED);
        appointmentRepository.save(appointment);
        
        // 发送手术通知单给病人
        notificationService.sendPatientSurgeryNotice(appointment);
        
        log.info("医生最终确认 - 预约ID: {}, 医生: {}", 
                appointmentId, appointment.getDoctor().getRealName());
    }

    @Override
    @Transactional
    public void startSurgery(Long id) {
        SurgeryAppointment appointment = appointmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("预约不存在"));
        
        if (appointment.getStatus() != AppointmentStatus.NOTIFIED) {
            throw new RuntimeException("只有已通知病人状态的预约才能开始手术");
        }
        
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        appointment.setActualStartTime(LocalDateTime.now());
        appointmentRepository.save(appointment);
        
        // 通知相关人员手术开始
        if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
        notificationService.createNotification(
            appointment.getPatient().getUser().getId(),
            "手术开始",
            "您的手术已开始，请耐心等待。",
            Notification.NotificationType.SURGERY_UPDATE
        );
        }
    }

    @Override
    @Transactional
    public void completeSurgery(Long id, String postSurgeryNotes) {
        SurgeryAppointment appointment = appointmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("预约不存在"));
        
        if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new RuntimeException("只有进行中状态的预约才能完成手术");
        }
        
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setActualEndTime(LocalDateTime.now());
        if (postSurgeryNotes != null && !postSurgeryNotes.trim().isEmpty()) {
            appointment.setPostSurgeryNotes(postSurgeryNotes);
        }
        appointmentRepository.save(appointment);
        
        // 更新病人状态为已完成手术
        Patient patient = patientRepository.findByIdWithUser(appointment.getPatient().getId())
                .orElseThrow(() -> new RuntimeException("病人不存在"));
        patient.setStatus("已完成手术");
        patient.setLastVisitDate(LocalDate.now());
        patientRepository.save(patient);
        
        // 通知病人手术完成
        if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
        notificationService.createNotification(
            appointment.getPatient().getUser().getId(),
            "手术完成",
            "您的手术已成功完成，请遵医嘱进行后续治疗。",
            Notification.NotificationType.SURGERY_COMPLETE
        );
        }
    }

    @Override
    public void cancelAppointment(Long appointmentId, Long userId, String reason) {
        SurgeryAppointment appointment = appointmentRepository.findByIdWithDetails(appointmentId)
                .orElseThrow(() -> new RuntimeException("手术预约不存在"));
        
        // 检查取消规则
        validateCancellation(appointment, userId);
        
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(reason);
        appointmentRepository.save(appointment);
        
        // 更新病人状态 - 使用正确的方法获取完整的Patient对象
        Long patientId = appointment.getPatient().getId();
        Optional<Patient> patientOpt = patientRepository.findByIdWithUser(patientId);
        if (patientOpt.isPresent()) {
            Patient patient = patientOpt.get();
            patient.setStatus("WAITING");
            patientRepository.save(patient);
        }
        
        // 记录取消记录
        recordCancellation(appointment, userId, reason);
        
        // 发送取消通知
        notificationService.sendSurgeryCancelledNotification(appointment);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SurgeryAppointment> findById(Long id) {
        return appointmentRepository.findByIdWithDetails(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurgeryAppointment> findAppointmentsForDate(LocalDate date) {
        return appointmentRepository.findByPlannedDate(date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurgeryAppointment> findActiveAppointmentsForDate(LocalDate date) {
        return appointmentRepository.findActiveSurgeriesForDate(date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurgeryAppointment> findAppointmentsForWeek(LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(6);
        return appointmentRepository.findSurgeriesBetweenDates(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurgeryAppointment> findAppointmentsForDoctor(User doctor) {
        return appointmentRepository.findByDoctorWithDetails(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurgeryAppointment> findAppointmentsForNurse(User nurse) {
        return appointmentRepository.findByNurseWithDetails(nurse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurgeryAppointment> findAppointmentsForAnesthesiologist(User anesthesiologist) {
        return appointmentRepository.findByAnesthesiologistWithDetails(anesthesiologist);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurgeryAppointment> findPendingConfirmationsForStaff(User user, LocalDate date) {
        return appointmentRepository.findPendingConfirmationsForStaff(user, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurgeryAppointment> findRoomScheduleForDate(Long roomId, LocalDate date) {
        return appointmentRepository.findRoomScheduleForDate(roomId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SurgeryAppointment> findBedScheduleForDate(Long bedId, LocalDate date) {
        return appointmentRepository.findBedScheduleForDate(bedId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SurgeryAppointment> findAppointmentsByStatus(List<AppointmentStatus> statuses, Pageable pageable) {
        return appointmentRepository.findByStatusIn(statuses, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SurgeryAppointment> searchAppointments(String patientName, String status, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return appointmentRepository.searchAppointments(patientName, status, startDate, endDate, pageable);
    }

    @Override
    public List<SurgeryAppointment> findAppointmentsForPatient(Long patientId) {
        Patient patient = patientRepository.findByIdWithUser(patientId)
                .orElseThrow(() -> new RuntimeException("病人不存在"));
        return appointmentRepository.findByPatientWithDetails(patient);
    }

    @Override
    public List<SurgeryAppointment> findUpcomingAppointmentsForPatient(Long patientId) {
        LocalDate today = LocalDate.now();
        Patient patient = patientRepository.findByIdWithUser(patientId)
                .orElseThrow(() -> new RuntimeException("病人不存在"));
        return appointmentRepository.findByPatientAndPlannedDateGreaterThanEqualOrderByPlannedDateAsc(patient, today);
    }

    private void validateTimeConflict(SurgeryAppointment appointment) {
        // 检查医生时间冲突
        List<SurgeryAppointment> doctorAppointments = appointmentRepository.findActiveSurgeriesForDate(appointment.getPlannedDate());
        boolean hasConflict = doctorAppointments.stream()
                .anyMatch(existing -> existing.getDoctor().getId().equals(appointment.getDoctor().getId()) &&
                        timeOverlaps(existing, appointment));
        
        if (hasConflict) {
            throw new RuntimeException("医生在此时间段已有其他手术安排");
        }
    }

    private void validateResourceAvailability(SurgeryAppointment appointment) {
        // 检查手术室和床位是否可用
        List<SurgeryAppointment> roomAppointments = appointmentRepository.findRoomScheduleForDate(
                appointment.getRoom().getId(), appointment.getPlannedDate());
        
        boolean roomOccupied = roomAppointments.stream()
                .anyMatch(existing -> timeOverlaps(existing, appointment));
        
        if (roomOccupied) {
            throw new RuntimeException("手术室在此时间段不可用");
        }
    }

    private boolean timeOverlaps(SurgeryAppointment existing, SurgeryAppointment newAppointment) {
        return existing.getPlannedStartTime().isBefore(newAppointment.getPlannedEndTime()) &&
               existing.getPlannedEndTime().isAfter(newAppointment.getPlannedStartTime());
    }

    private void sendAppointmentNotifications(SurgeryAppointment appointment) {
        // 重新加载完整的预约信息，确保包含所有关联对象
        SurgeryAppointment fullAppointment = appointmentRepository.findByIdWithDetails(appointment.getId())
                .orElse(appointment);
        
        // 发送基础的手术安排通知给病人
        notificationService.sendSurgeryScheduledNotification(fullAppointment);
        
        // 如果分配了医疗团队，发送确认请求
        if (fullAppointment.getAnesthesiologist() != null || fullAppointment.getNurse() != null) {
            fullAppointment.setStatus(AppointmentStatus.PENDING_CONFIRMATION);
            appointmentRepository.save(fullAppointment);
            notificationService.sendTeamConfirmationRequest(fullAppointment);
        }
    }

    private boolean canConfirmAppointment(SurgeryAppointment appointment, Long userId) {
        return appointment.getAnesthesiologist() != null && appointment.getAnesthesiologist().getId().equals(userId) ||
               appointment.getNurse() != null && appointment.getNurse().getId().equals(userId) ||
               appointment.getDoctor().getId().equals(userId);
    }

    private void validateCancellation(SurgeryAppointment appointment, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 只允许病人取消预约
        if (user.getRole() != UserRole.PATIENT) {
            throw new RuntimeException("只有病人可以取消预约");
        }
        
        // 检查用户是否在黑名单中
        if (blacklistRepository.isUserBlacklisted(user)) {
            throw new RuntimeException("您已被列入黑名单，无法取消预约");
        }
        
        // 检查本月取消次数
        LocalDate now = LocalDate.now();
        Long thisMonthCancellations = cancellationRecordRepository.countByUserAndMonth(
                user, now.getYear(), now.getMonthValue());
        
        if (thisMonthCancellations >= 1) {
            throw new RuntimeException("每月只能取消一次预约");
        }
        
        // 检查是否提前48小时
        LocalDateTime surgeryDateTime = LocalDateTime.of(appointment.getPlannedDate(), appointment.getPlannedStartTime());
        long hoursUntilSurgery = ChronoUnit.HOURS.between(LocalDateTime.now(), surgeryDateTime);
        
        if (hoursUntilSurgery < 48) {
            throw new RuntimeException("取消手术需要提前48小时，否则将被加入黑名单");
        }
        
        // 病人只能取消自己的预约
        if (appointment.getPatient() == null || appointment.getPatient().getUser() == null ||
            !appointment.getPatient().getUser().getId().equals(userId)) {
            throw new RuntimeException("您只能取消自己的预约");
        }
        
        // 检查预约状态是否可以取消
        if (!canBeCancelled(appointment.getStatus())) {
            throw new RuntimeException("当前状态的预约无法取消");
        }
    }
    
    private boolean canBeCancelled(AppointmentStatus status) {
        // 只有这些状态的预约可以被取消
        return status == AppointmentStatus.SCHEDULED || 
               status == AppointmentStatus.PENDING_CONFIRMATION ||
               status == AppointmentStatus.TEAM_CONFIRMED ||
               status == AppointmentStatus.DOCTOR_FINAL_CONFIRMED ||
               status == AppointmentStatus.NOTIFIED;
    }

    private void recordCancellation(SurgeryAppointment appointment, Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 计算取消时距离手术的小时数
        LocalDateTime surgeryDateTime = LocalDateTime.of(appointment.getPlannedDate(), appointment.getPlannedStartTime());
        long hoursUntilSurgery = ChronoUnit.HOURS.between(LocalDateTime.now(), surgeryDateTime);
        
        // 记录取消记录
        CancellationRecord record = new CancellationRecord();
        record.setUser(user);
        record.setAppointment(appointment);
        record.setHoursBeforeSurgery((int) hoursUntilSurgery);
        record.setReason(reason);
        cancellationRecordRepository.save(record);
        
        // 如果是病人且不足48小时取消，加入黑名单
        if (user.getRole() == UserRole.PATIENT && hoursUntilSurgery < 48) {
            addToBlacklist(user, "违规取消预约：未提前48小时取消手术预约");
        }
    }
    
    private void addToBlacklist(User user, String reason) {
        Blacklist blacklist = new Blacklist();
        blacklist.setUser(user);
        blacklist.setReason(reason);
        blacklist.setBlacklistStartDate(LocalDate.now());
        blacklist.setBlacklistEndDate(LocalDate.now().plusMonths(1)); // 拉黑1个月
        // 这里应该从当前登录用户获取，暂时用系统管理员
        blacklist.setCreatedBy(user); // 临时设置，实际应该是管理员
        blacklistRepository.save(blacklist);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countAllAppointments() {
        return appointmentRepository.count();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countTodayAppointments() {
        LocalDate today = LocalDate.now();
        return appointmentRepository.findByPlannedDate(today).size();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countTodayAppointmentsForDoctor(Long doctorId) {
        LocalDate today = LocalDate.now();
        return appointmentRepository.findByPlannedDate(today).stream()
                .filter(appointment -> appointment.getDoctor() != null && 
                                     appointment.getDoctor().getId().equals(doctorId))
                .count();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countCompletedAppointmentsForDoctor(Long doctorId, LocalDate date) {
        return appointmentRepository.findByPlannedDate(date).stream()
                .filter(appointment -> appointment.getDoctor() != null && 
                                     appointment.getDoctor().getId().equals(doctorId) &&
                                     appointment.getStatus() == AppointmentStatus.COMPLETED)
                .count();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countPendingAppointmentsForDoctor(Long doctorId) {
        return appointmentRepository.findAll().stream()
                .filter(appointment -> appointment.getDoctor() != null && 
                                     appointment.getDoctor().getId().equals(doctorId) &&
                                     (appointment.getStatus() == AppointmentStatus.PENDING_CONFIRMATION ||
                                      appointment.getStatus() == AppointmentStatus.TEAM_CONFIRMED))
                .count();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countTodayAppointmentsForStaff(Long staffId, UserRole role) {
        LocalDate today = LocalDate.now();
        return appointmentRepository.findByPlannedDate(today).stream()
                .filter(appointment -> {
                    if (role == UserRole.ANESTHESIOLOGIST) {
                        return appointment.getAnesthesiologist() != null && 
                               appointment.getAnesthesiologist().getId().equals(staffId);
                    } else if (role == UserRole.NURSE) {
                        return appointment.getNurse() != null && 
                               appointment.getNurse().getId().equals(staffId);
                    }
                    return false;
                })
                .count();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countCompletedAppointmentsForStaff(Long staffId, UserRole role, LocalDate date) {
        return appointmentRepository.findByPlannedDate(date).stream()
                .filter(appointment -> {
                    boolean isAssigned = false;
                    if (role == UserRole.ANESTHESIOLOGIST) {
                        isAssigned = appointment.getAnesthesiologist() != null && 
                                   appointment.getAnesthesiologist().getId().equals(staffId);
                    } else if (role == UserRole.NURSE) {
                        isAssigned = appointment.getNurse() != null && 
                                   appointment.getNurse().getId().equals(staffId);
                    }
                    return isAssigned && appointment.getStatus() == AppointmentStatus.COMPLETED;
                })
                .count();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countPendingAppointmentsForStaff(Long staffId, UserRole role) {
        return appointmentRepository.findAll().stream()
                .filter(appointment -> {
                    boolean isAssigned = false;
                    if (role == UserRole.ANESTHESIOLOGIST) {
                        isAssigned = appointment.getAnesthesiologist() != null && 
                                   appointment.getAnesthesiologist().getId().equals(staffId);
                    } else if (role == UserRole.NURSE) {
                        isAssigned = appointment.getNurse() != null && 
                                   appointment.getNurse().getId().equals(staffId);
                    }
                    return isAssigned && appointment.getStatus() == AppointmentStatus.PENDING_CONFIRMATION;
                })
                .count();
    }

    @Override
    @Transactional
    public void deleteAppointment(Long id) {
        SurgeryAppointment appointment = appointmentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("预约不存在"));
        
        // 只能删除已取消的预约
        if (appointment.getStatus() != AppointmentStatus.CANCELLED) {
            throw new RuntimeException("只能删除已取消的预约");
        }
        
        // 删除相关的通知记录
        notificationService.deleteNotificationsByAppointmentId(id);
        
        // 删除相关的取消记录
        cancellationRecordRepository.deleteByAppointment(appointment);
        
        // 删除预约
        appointmentRepository.delete(appointment);
        
        log.info("删除预约成功 - 预约ID: {}, 病人: {}", 
                id, appointment.getPatient() != null && appointment.getPatient().getUser() != null ? 
                    appointment.getPatient().getUser().getRealName() : "未知");
    }
} 