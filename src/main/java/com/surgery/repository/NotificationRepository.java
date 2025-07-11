package com.surgery.repository;

import com.surgery.entity.Notification;
import com.surgery.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * 查找用户的通知（分页）
     */
    Page<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);
    
    /**
     * 查找用户的未读通知
     */
    List<Notification> findByRecipientAndIsReadFalseOrderByCreatedAtDesc(User recipient);
    
    /**
     * 统计用户的未读通知数量
     */
    long countByRecipientAndIsReadFalse(User recipient);
    
    /**
     * 查找指定时间范围内的通知
     */
    @Query("SELECT n FROM Notification n WHERE n.recipient = :recipient AND n.createdAt BETWEEN :startTime AND :endTime ORDER BY n.createdAt DESC")
    List<Notification> findByRecipientAndTimeRange(@Param("recipient") User recipient, 
                                                   @Param("startTime") LocalDateTime startTime, 
                                                   @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找与特定预约相关的通知
     */
    @Query("SELECT n FROM Notification n WHERE n.relatedAppointment.id = :appointmentId ORDER BY n.createdAt DESC")
    List<Notification> findByRelatedAppointmentId(@Param("appointmentId") Long appointmentId);
    
    /**
     * 批量标记为已读
     */
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readTime WHERE n.recipient = :recipient AND n.isRead = false")
    void markAllAsReadForUser(@Param("recipient") User recipient, @Param("readTime") LocalDateTime readTime);
    
    /**
     * 删除与特定预约相关的通知
     */
    void deleteByRelatedAppointmentId(Long appointmentId);
} 