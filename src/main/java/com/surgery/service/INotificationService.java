package com.surgery.service;

import com.surgery.entity.SurgeryAppointment;
import com.surgery.entity.User;
import com.surgery.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface INotificationService {

    void sendSurgeryScheduledNotification(SurgeryAppointment appointment);

    void sendSurgeryConfirmedNotification(SurgeryAppointment appointment);

    void sendSurgeryUpdatedNotification(SurgeryAppointment appointment);

    void sendSurgeryCancelledNotification(SurgeryAppointment appointment);

    void sendSurgeryReminderNotification(SurgeryAppointment appointment);

    // 创建通知消息
    void createNotification(Long userId, String title, String content, Notification.NotificationType type);

    // 查询用户的通知消息
    Page<Notification> findNotificationsForUser(User user, Pageable pageable);

    // 标记通知为已读
    void markAsRead(Long notificationId, Long userId);

    // 统计未读通知数量
    long countUnreadNotifications(User user);

    // 查询用户的未读通知列表
    List<Notification> findUnreadNotifications(User user);
    
    // 发送医疗团队确认请求
    void sendTeamConfirmationRequest(SurgeryAppointment appointment);
    
    // 发送医生最终确认请求
    void sendDoctorFinalConfirmationRequest(SurgeryAppointment appointment);
    
    // 发送病人手术通知单
    void sendPatientSurgeryNotice(SurgeryAppointment appointment);
    
    // 根据ID获取通知详情
    Notification findById(Long id);
    
    // 删除与特定预约相关的通知
    void deleteNotificationsByAppointmentId(Long appointmentId);
}