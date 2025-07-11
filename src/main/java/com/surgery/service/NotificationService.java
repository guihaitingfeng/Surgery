package com.surgery.service;

import com.surgery.entity.Notification;
import com.surgery.entity.SurgeryAppointment;
import com.surgery.entity.User;
import com.surgery.repository.NotificationRepository;
import com.surgery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public void createNotification(Long userId, String title, String content, Notification.NotificationType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Notification notification = new Notification();
        notification.setRecipient(user);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationRepository.save(notification);
        log.info("创建通知 - 用户: {}, 标题: {}", user.getRealName(), title);
    }

    @Override
    public void sendSurgeryScheduledNotification(SurgeryAppointment appointment) {
        // 添加更全面的空值检查
        String patientName = "未知患者";
        if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
            User patientUser = appointment.getPatient().getUser();
            patientName = patientUser.getRealName() != null ? patientUser.getRealName() : "未知患者";
        }
        
        String doctorName = "未知医生";
        if (appointment.getDoctor() != null) {
            doctorName = appointment.getDoctor().getRealName() != null ? appointment.getDoctor().getRealName() : "未知医生";
        }
        
        log.info("发送手术安排通知 - 预约ID: {}, 患者: {}, 医生: {}", 
                appointment.getId(), 
                patientName,
                doctorName);
        
        // 创建通知给病人
        if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
            createNotification(
                appointment.getPatient().getUser().getId(),
                "手术安排通知",
                String.format("您的手术已安排，手术名称：%s，手术时间：%s %s，主刀医生：%s。",
                    appointment.getSurgeryName(),
                    appointment.getPlannedDate(),
                    appointment.getPlannedStartTime(),
                    doctorName),
                Notification.NotificationType.SURGERY_SCHEDULED
            );
        }
    }

    @Override
    public void sendSurgeryConfirmedNotification(SurgeryAppointment appointment) {
        log.info("发送手术确认通知 - 预约ID: {}, 状态: 已确认", appointment.getId());
        
        // 创建确认通知给病人
        if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
            createNotification(
                appointment.getPatient().getUser().getId(),
                "手术确认通知",
                String.format("您的手术已确认，手术名称：%s，手术时间：%s %s。",
                    appointment.getSurgeryName(),
                    appointment.getPlannedDate(),
                    appointment.getPlannedStartTime()),
                Notification.NotificationType.SURGERY_CONFIRMED
            );
        }
    }

    @Override
    public void sendSurgeryUpdatedNotification(SurgeryAppointment appointment) {
        log.info("发送手术更新通知 - 预约ID: {}, 已更新", appointment.getId());
        
        // 创建更新通知给病人
        if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
            createNotification(
                appointment.getPatient().getUser().getId(),
                "手术更新通知",
                String.format("您的手术信息已更新，手术名称：%s，手术时间：%s %s。",
                    appointment.getSurgeryName(),
                    appointment.getPlannedDate(),
                    appointment.getPlannedStartTime()),
                Notification.NotificationType.SURGERY_UPDATED
            );
        }
    }

    @Override
    public void sendSurgeryCancelledNotification(SurgeryAppointment appointment) {
        log.info("发送手术取消通知 - 预约ID: {}, 状态: 已取消", appointment.getId());
        
        // 获取取消原因
        String cancelReason = appointment.getCancelReason() != null ? 
                            appointment.getCancelReason() : "未说明";
        
        // 获取基本信息
        String patientName = "未知患者";
        if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
            patientName = appointment.getPatient().getUser().getRealName() != null ? 
                         appointment.getPatient().getUser().getRealName() : "未知患者";
        }
        
        String doctorName = "未知医生";
        if (appointment.getDoctor() != null) {
            doctorName = appointment.getDoctor().getRealName() != null ? 
                        appointment.getDoctor().getRealName() : "未知医生";
        }
        
        String roomName = appointment.getRoom() != null ? appointment.getRoom().getRoomName() : "待定";
        
        // 创建取消通知给病人
        if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
            createNotificationWithAppointment(
                appointment.getPatient().getUser().getId(),
                "手术取消通知",
                String.format("您的手术已取消，手术名称：%s，原定时间：%s %s。\n取消原因：%s",
                    appointment.getSurgeryName(),
                    appointment.getPlannedDate(),
                    appointment.getPlannedStartTime(),
                    cancelReason),
                Notification.NotificationType.SURGERY_CANCELLED,
                appointment
            );
        }
        
        // 创建取消通知给主刀医生
        if (appointment.getDoctor() != null) {
            createNotificationWithAppointment(
                appointment.getDoctor().getId(),
                "手术取消通知",
                String.format("病人 %s 取消了手术预约：\n" +
                            "手术名称：%s\n" +
                            "原定时间：%s %s - %s\n" +
                            "手术室：%s\n" +
                            "取消原因：%s\n\n" +
                            "请及时调整您的手术安排。",
                    patientName,
                    appointment.getSurgeryName(),
                    appointment.getPlannedDate(),
                    appointment.getPlannedStartTime(),
                    appointment.getPlannedEndTime(),
                    roomName,
                    cancelReason),
                Notification.NotificationType.SURGERY_CANCELLED,
                appointment
            );
        }
        
        // 创建取消通知给麻醉师
        if (appointment.getAnesthesiologist() != null) {
            createNotificationWithAppointment(
                appointment.getAnesthesiologist().getId(),
                "手术取消通知",
                String.format("病人 %s 取消了手术预约：\n" +
                            "手术名称：%s\n" +
                            "原定时间：%s %s - %s\n" +
                            "主刀医生：%s\n" +
                            "手术室：%s\n" +
                            "取消原因：%s\n\n" +
                            "您的麻醉安排已自动取消。",
                    patientName,
                    appointment.getSurgeryName(),
                    appointment.getPlannedDate(),
                    appointment.getPlannedStartTime(),
                    appointment.getPlannedEndTime(),
                    doctorName,
                    roomName,
                    cancelReason),
                Notification.NotificationType.SURGERY_CANCELLED,
                appointment
            );
        }
        
        // 创建取消通知给护士
        if (appointment.getNurse() != null) {
            createNotificationWithAppointment(
                appointment.getNurse().getId(),
                "手术取消通知",
                String.format("病人 %s 取消了手术预约：\n" +
                            "手术名称：%s\n" +
                            "原定时间：%s %s - %s\n" +
                            "主刀医生：%s\n" +
                            "手术室：%s\n" +
                            "取消原因：%s\n\n" +
                            "您的护理安排已自动取消。",
                    patientName,
                    appointment.getSurgeryName(),
                    appointment.getPlannedDate(),
                    appointment.getPlannedStartTime(),
                    appointment.getPlannedEndTime(),
                    doctorName,
                    roomName,
                    cancelReason),
                Notification.NotificationType.SURGERY_CANCELLED,
                appointment
            );
        }
        
        log.info("手术取消通知发送完成 - 预约ID: {}, 通知对象: 病人{}, 医生{}, 麻醉师{}, 护士{}", 
                appointment.getId(),
                appointment.getPatient() != null ? "✓" : "✗",
                appointment.getDoctor() != null ? "✓" : "✗",
                appointment.getAnesthesiologist() != null ? "✓" : "✗",
                appointment.getNurse() != null ? "✓" : "✗");
    }

    @Override
    public void sendSurgeryReminderNotification(SurgeryAppointment appointment) {
        log.info("发送手术提醒通知 - 预约ID: {}, 手术时间: {} {}", 
                appointment.getId(),
                appointment.getPlannedDate(),
                appointment.getPlannedStartTime());
        
        // 创建提醒通知给病人
        if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
            createNotification(
                appointment.getPatient().getUser().getId(),
                "手术提醒",
                String.format("提醒您明天有手术安排，手术名称：%s，手术时间：%s %s，请按时到达。",
                    appointment.getSurgeryName(),
                    appointment.getPlannedDate(),
                    appointment.getPlannedStartTime()),
                Notification.NotificationType.SURGERY_SCHEDULED
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> findNotificationsForUser(User user, Pageable pageable) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user, pageable);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getRecipient().getId().equals(userId)) {
                notification.setIsRead(true);
                notification.setReadAt(LocalDateTime.now());
                notificationRepository.save(notification);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadNotifications(User user) {
        return notificationRepository.countByRecipientAndIsReadFalse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findUnreadNotifications(User user) {
        return notificationRepository.findByRecipientAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    /**
     * 医生创建手术预约后，发送确认通知给麻醉师和护士
     */
    public void sendTeamConfirmationRequest(SurgeryAppointment appointment) {
        // 添加空值检查
        String patientName = "未知患者";
        if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
            patientName = appointment.getPatient().getUser().getRealName() != null ? 
                         appointment.getPatient().getUser().getRealName() : "未知患者";
        }
        
        String doctorName = "未知医生";
        if (appointment.getDoctor() != null) {
            doctorName = appointment.getDoctor().getRealName() != null ? 
                        appointment.getDoctor().getRealName() : "未知医生";
        }
        
        String roomName = appointment.getRoom() != null ? appointment.getRoom().getRoomName() : "待定";
        String bedNumber = appointment.getBed() != null ? appointment.getBed().getBedNumber() : "待定";
        
        String baseMessage = String.format(
            "医生 %s 安排了新的手术预约，请确认您的参与：\n" +
            "病人：%s\n" +
            "手术名称：%s\n" +
            "手术时间：%s %s - %s\n" +
            "手术室：%s\n" +
            "床位：%s\n" +
            "请及时确认您的参与。",
            doctorName, patientName, appointment.getSurgeryName(),
            appointment.getPlannedDate(), appointment.getPlannedStartTime(), appointment.getPlannedEndTime(),
            roomName, bedNumber
        );
        
        // 发送通知给麻醉师
        if (appointment.getAnesthesiologist() != null) {
            createNotificationWithAppointment(
                appointment.getAnesthesiologist().getId(),
                "手术确认请求 - 麻醉师",
                baseMessage,
                Notification.NotificationType.TEAM_CONFIRMATION_REQUEST,
                appointment
            );
        }
        
        // 发送通知给护士
        if (appointment.getNurse() != null) {
            createNotificationWithAppointment(
                appointment.getNurse().getId(),
                "手术确认请求 - 护士",
                baseMessage,
                Notification.NotificationType.TEAM_CONFIRMATION_REQUEST,
                appointment
            );
        }
        
        log.info("发送医疗团队确认请求 - 预约ID: {}, 麻醉师: {}, 护士: {}", 
                appointment.getId(),
                appointment.getAnesthesiologist() != null ? appointment.getAnesthesiologist().getRealName() : "未分配",
                appointment.getNurse() != null ? appointment.getNurse().getRealName() : "未分配");
    }

    /**
     * 医疗团队确认完成后，通知医生进行最终确认
     */
    public void sendDoctorFinalConfirmationRequest(SurgeryAppointment appointment) {
        // 添加空值检查
        String patientName = "未知患者";
        if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
            patientName = appointment.getPatient().getUser().getRealName() != null ? 
                         appointment.getPatient().getUser().getRealName() : "未知患者";
        }
        
        String anesthesiologistName = appointment.getAnesthesiologist() != null ? 
            appointment.getAnesthesiologist().getRealName() : "未分配";
        String nurseName = appointment.getNurse() != null ? 
            appointment.getNurse().getRealName() : "未分配";
        
        String message = String.format(
            "手术预约的医疗团队确认已完成，请进行最终确认：\n" +
            "病人：%s\n" +
            "手术名称：%s\n" +
            "手术时间：%s %s - %s\n" +
            "麻醉师：%s %s\n" +
            "护士：%s %s\n" +
            "请确认手术安排无误，确认后将通知病人。",
            patientName, appointment.getSurgeryName(),
            appointment.getPlannedDate(), appointment.getPlannedStartTime(), appointment.getPlannedEndTime(),
            anesthesiologistName, appointment.getAnesthesiologistConfirmed() ? "(已确认)" : "(未确认)",
            nurseName, appointment.getNurseConfirmed() ? "(已确认)" : "(未确认)"
        );
        
        createNotificationWithAppointment(
            appointment.getDoctor().getId(),
            "手术最终确认请求",
            message,
            Notification.NotificationType.DOCTOR_FINAL_CONFIRMATION_REQUEST,
            appointment
        );
        
        log.info("发送医生最终确认请求 - 预约ID: {}, 医生: {}", 
                appointment.getId(), appointment.getDoctor().getRealName());
    }

    /**
     * 医生最终确认后，发送手术通知单给病人
     */
    public void sendPatientSurgeryNotice(SurgeryAppointment appointment) {
        // 添加空值检查
        String doctorName = "未知医生";
        if (appointment.getDoctor() != null) {
            doctorName = appointment.getDoctor().getRealName() != null ? 
                        appointment.getDoctor().getRealName() : "未知医生";
        }
        
        String anesthesiologistName = appointment.getAnesthesiologist() != null ? 
            appointment.getAnesthesiologist().getRealName() : "待安排";
        String nurseName = appointment.getNurse() != null ? 
            appointment.getNurse().getRealName() : "待安排";
        String roomName = appointment.getRoom() != null ? appointment.getRoom().getRoomName() : "待定";
        String bedNumber = appointment.getBed() != null ? appointment.getBed().getBedNumber() : "待定";
        
        String message = String.format(
            "您的手术安排已最终确认，请查看详细信息：\n\n" +
            "═══ 手术通知单 ═══\n" +
            "手术名称：%s\n" +
            "手术类型：%s\n" +
            "手术时间：%s %s - %s\n" +
            "预计时长：%d 分钟\n\n" +
            "医疗团队：\n" +
            "主刀医生：%s\n" +
            "麻醉师：%s\n" +
            "手术护士：%s\n\n" +
            "手术地点：\n" +
            "手术室：%s\n" +
            "床位：%s\n\n" +
            "注意事项：\n" +
            "1. 请提前2小时到达医院\n" +
            "2. 术前8小时禁食禁水\n" +
            "3. 请带齐相关检查资料\n" +
            "4. 如有疑问请及时联系医护人员\n\n" +
            "祝您手术顺利！",
            appointment.getSurgeryName(),
            appointment.getSurgeryType() != null ? appointment.getSurgeryType() : "常规手术",
            appointment.getPlannedDate(), appointment.getPlannedStartTime(), appointment.getPlannedEndTime(),
            appointment.getEstimatedDuration() != null ? appointment.getEstimatedDuration() : 0,
            doctorName, anesthesiologistName, nurseName,
            roomName, bedNumber
        );
        
        // 只有在病人和用户都存在时才发送通知
        if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
            createNotificationWithAppointment(
                appointment.getPatient().getUser().getId(),
                "手术通知单",
                message,
                Notification.NotificationType.PATIENT_SURGERY_NOTICE,
                appointment
            );
            
            String patientName = appointment.getPatient().getUser().getRealName() != null ? 
                                appointment.getPatient().getUser().getRealName() : "未知患者";
            log.info("发送病人手术通知单 - 预约ID: {}, 病人: {}", 
                    appointment.getId(), patientName);
        } else {
            log.warn("无法发送病人手术通知单 - 预约ID: {}, 病人信息不完整", appointment.getId());
        }
    }

    /**
     * 创建带预约关联的通知
     */
    private void createNotificationWithAppointment(Long userId, String title, String content, 
                                                  Notification.NotificationType type, SurgeryAppointment appointment) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Notification notification = new Notification();
        notification.setRecipient(user);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setRelatedAppointment(appointment);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationRepository.save(notification);
        log.info("创建通知 - 用户: {}, 标题: {}, 关联预约: {}", 
                user.getRealName(), title, appointment.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Notification findById(Long id) {
        return notificationRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void deleteNotificationsByAppointmentId(Long appointmentId) {
        try {
            // 删除与指定预约相关的所有通知
            notificationRepository.deleteByRelatedAppointmentId(appointmentId);
            log.info("删除预约相关通知成功 - 预约ID: {}", appointmentId);
        } catch (Exception e) {
            log.error("删除预约相关通知失败 - 预约ID: {}, 错误: {}", appointmentId, e.getMessage());
            throw new RuntimeException("删除预约相关通知失败: " + e.getMessage());
        }
    }
} 