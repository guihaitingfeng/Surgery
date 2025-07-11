package com.surgery.controller;

import com.surgery.entity.Notification;
import com.surgery.entity.User;
import com.surgery.service.INotificationService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;
    private final IUserService userService;

    /**
     * 获取当前用户的通知列表
     */
    @GetMapping
    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE') or hasRole('ANESTHESIOLOGIST') or hasRole('PATIENT') or hasRole('ADMIN')")
    public ResponseEntity<?> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElse(null);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }
            
            Sort sort = Sort.by("createdAt").descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Notification> notifications = notificationService.findNotificationsForUser(user, pageable);
            
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取通知详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE') or hasRole('ANESTHESIOLOGIST') or hasRole('PATIENT') or hasRole('ADMIN')")
    public ResponseEntity<?> getNotification(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElse(null);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }
            
            Notification notification = notificationService.findById(id);
            if (notification == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查通知是否属于当前用户
            if (!notification.getRecipient().getId().equals(user.getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "无权访问此通知"));
            }
            
            return ResponseEntity.ok(notification);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE') or hasRole('ANESTHESIOLOGIST') or hasRole('PATIENT') or hasRole('ADMIN')")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElse(null);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }
            
            notificationService.markAsRead(id, user.getId());
            return ResponseEntity.ok(Map.of("message", "标记已读成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE') or hasRole('ANESTHESIOLOGIST') or hasRole('PATIENT') or hasRole('ADMIN')")
    public ResponseEntity<?> getUnreadCount(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElse(null);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }
            
            long unreadCount = notificationService.countUnreadNotifications(user);
            return ResponseEntity.ok(Map.of("count", unreadCount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取未读通知列表
     */
    @GetMapping("/unread")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('NURSE') or hasRole('ANESTHESIOLOGIST') or hasRole('PATIENT') or hasRole('ADMIN')")
    public ResponseEntity<?> getUnreadNotifications(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username).orElse(null);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }
            
            List<Notification> notifications = notificationService.findUnreadNotifications(user);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
} 