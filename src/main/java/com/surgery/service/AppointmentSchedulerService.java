package com.surgery.service;

import com.surgery.entity.SurgeryAppointment;
import com.surgery.enums.AppointmentStatus;
import com.surgery.repository.SurgeryAppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentSchedulerService {

    private final SurgeryAppointmentRepository appointmentRepository;
    private final INotificationService notificationService;

    /**
     * 每分钟检查一次手术状态，自动更新
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    @Transactional
    public void updateAppointmentStatus() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        log.debug("开始检查手术状态自动更新 - 当前时间: {}", now);

        // 获取今天的所有预约
        List<SurgeryAppointment> todayAppointments = appointmentRepository.findByPlannedDate(today);

        for (SurgeryAppointment appointment : todayAppointments) {
            try {
                updateSingleAppointmentStatus(appointment, currentTime);
            } catch (Exception e) {
                log.error("更新预约状态失败 - 预约ID: {}, 错误: {}", appointment.getId(), e.getMessage());
            }
        }
    }

    /**
     * 更新单个预约的状态
     */
    private void updateSingleAppointmentStatus(SurgeryAppointment appointment, LocalTime currentTime) {
        LocalTime startTime = appointment.getPlannedStartTime();
        LocalTime endTime = appointment.getPlannedEndTime();
        AppointmentStatus currentStatus = appointment.getStatus();

        // 只处理已通知病人的预约
        if (currentStatus == AppointmentStatus.NOTIFIED) {
            // 检查是否到了手术开始时间
            if (currentTime.isAfter(startTime) || currentTime.equals(startTime)) {
                // 自动开始手术
                appointment.setStatus(AppointmentStatus.IN_PROGRESS);
                appointment.setActualStartTime(LocalDateTime.now());
                appointmentRepository.save(appointment);

                log.info("自动开始手术 - 预约ID: {}, 手术名称: {}, 开始时间: {}", 
                        appointment.getId(), appointment.getSurgeryName(), startTime);

                // 发送通知
                sendSurgeryStartNotification(appointment);
            }
        }
        // 处理进行中的手术
        else if (currentStatus == AppointmentStatus.IN_PROGRESS) {
            // 检查是否到了手术结束时间
            if (currentTime.isAfter(endTime)) {
                // 自动完成手术
                appointment.setStatus(AppointmentStatus.COMPLETED);
                appointment.setActualEndTime(LocalDateTime.now());
                // 设置默认的术后备注
                if (appointment.getPostSurgeryNotes() == null || appointment.getPostSurgeryNotes().trim().isEmpty()) {
                    appointment.setPostSurgeryNotes("手术按预定时间完成，无特殊情况。");
                }
                appointmentRepository.save(appointment);

                log.info("自动完成手术 - 预约ID: {}, 手术名称: {}, 结束时间: {}", 
                        appointment.getId(), appointment.getSurgeryName(), endTime);

                // 发送通知
                sendSurgeryCompleteNotification(appointment);
            }
        }
    }

    /**
     * 发送手术开始通知
     */
    private void sendSurgeryStartNotification(SurgeryAppointment appointment) {
        try {
            // 通知病人
            if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
                notificationService.createNotification(
                        appointment.getPatient().getUser().getId(),
                        "手术开始",
                        String.format("您的手术【%s】已经开始，请耐心等待。", appointment.getSurgeryName()),
                        com.surgery.entity.Notification.NotificationType.SURGERY_UPDATE
                );
            }

            // 通知医疗团队
            if (appointment.getDoctor() != null) {
                notificationService.createNotification(
                        appointment.getDoctor().getId(),
                        "手术自动开始",
                        String.format("手术【%s】已自动开始，请前往手术室。", appointment.getSurgeryName()),
                        com.surgery.entity.Notification.NotificationType.SURGERY_UPDATE
                );
            }

            if (appointment.getAnesthesiologist() != null) {
                notificationService.createNotification(
                        appointment.getAnesthesiologist().getId(),
                        "手术自动开始",
                        String.format("手术【%s】已自动开始，请前往手术室。", appointment.getSurgeryName()),
                        com.surgery.entity.Notification.NotificationType.SURGERY_UPDATE
                );
            }

            if (appointment.getNurse() != null) {
                notificationService.createNotification(
                        appointment.getNurse().getId(),
                        "手术自动开始",
                        String.format("手术【%s】已自动开始，请前往手术室。", appointment.getSurgeryName()),
                        com.surgery.entity.Notification.NotificationType.SURGERY_UPDATE
                );
            }
        } catch (Exception e) {
            log.error("发送手术开始通知失败 - 预约ID: {}, 错误: {}", appointment.getId(), e.getMessage());
        }
    }

    /**
     * 发送手术完成通知
     */
    private void sendSurgeryCompleteNotification(SurgeryAppointment appointment) {
        try {
            // 通知病人
            if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
                notificationService.createNotification(
                        appointment.getPatient().getUser().getId(),
                        "手术完成",
                        String.format("您的手术【%s】已成功完成，请遵医嘱进行后续治疗。", appointment.getSurgeryName()),
                        com.surgery.entity.Notification.NotificationType.SURGERY_COMPLETE
                );
            }

            // 通知医疗团队
            if (appointment.getDoctor() != null) {
                notificationService.createNotification(
                        appointment.getDoctor().getId(),
                        "手术自动完成",
                        String.format("手术【%s】已按预定时间完成。", appointment.getSurgeryName()),
                        com.surgery.entity.Notification.NotificationType.SURGERY_COMPLETE
                );
            }
        } catch (Exception e) {
            log.error("发送手术完成通知失败 - 预约ID: {}, 错误: {}", appointment.getId(), e.getMessage());
        }
    }

    /**
     * 每小时检查一次，处理延期的手术
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    @Transactional
    public void checkOverdueAppointments() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();

        log.debug("开始检查延期手术 - 当前时间: {}", now);

        // 查找应该已经开始但还没开始的手术
        List<SurgeryAppointment> overdueAppointments = appointmentRepository.findOverdueAppointments(today, currentTime);

        for (SurgeryAppointment appointment : overdueAppointments) {
            try {
                // 发送延期警告
                sendOverdueWarning(appointment);
            } catch (Exception e) {
                log.error("发送延期警告失败 - 预约ID: {}, 错误: {}", appointment.getId(), e.getMessage());
            }
        }
    }

    /**
     * 发送延期警告
     */
    private void sendOverdueWarning(SurgeryAppointment appointment) {
        if (appointment.getDoctor() != null) {
            notificationService.createNotification(
                    appointment.getDoctor().getId(),
                    "手术延期警告",
                    String.format("手术【%s】已超过预定开始时间，请尽快处理。", appointment.getSurgeryName()),
                    com.surgery.entity.Notification.NotificationType.SURGERY_UPDATE
            );
        }
    }
} 