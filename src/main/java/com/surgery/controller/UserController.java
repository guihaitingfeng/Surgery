package com.surgery.controller;

import com.surgery.entity.User;
import com.surgery.enums.UserRole;
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
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    /**
     * 获取所有用户列表（分页）
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) UserRole role) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<User> users;
            if (role != null) {
                users = userService.findUsersByRole(role, pageable);
            } else {
                users = userService.findAllUsers(pageable);
            }
            
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取用户列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 根据ID获取用户信息
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            Optional<User> user = userService.findById(id);
            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取用户信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 创建新用户
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody User user) {
        try {
            // 检查用户名是否已存在
            if (userService.findByUsername(user.getUsername()).isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "用户名已存在");
                return ResponseEntity.badRequest().body(error);
            }
            
            // 检查邮箱是否已存在
            if (userService.findByEmail(user.getEmail()).isPresent()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "邮箱已存在");
                return ResponseEntity.badRequest().body(error);
            }
            
            User savedUser = userService.createUser(user);
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "创建用户失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody User userDetails) {
        try {
            Optional<User> existingUser = userService.findById(id);
            if (!existingUser.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查邮箱是否被其他用户使用
            Optional<User> userWithSameEmail = userService.findByEmail(userDetails.getEmail());
            if (userWithSameEmail.isPresent() && !userWithSameEmail.get().getId().equals(id)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "邮箱已被其他用户使用");
                return ResponseEntity.badRequest().body(error);
            }
            
            User updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "更新用户失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 切换用户启用/禁用状态
     */
    @PutMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id, Authentication authentication) {
        try {
            // 获取当前管理员用户
            String currentUsername = authentication.getName();
            User currentUser = userService.findByUsername(currentUsername).orElse(null);
            
            if (currentUser != null && currentUser.getId().equals(id)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "不能禁用自己的账户");
                return ResponseEntity.badRequest().body(error);
            }
            
            User updatedUser = userService.toggleUserStatus(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "用户状态更新成功");
            response.put("user", updatedUser);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "更新用户状态失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        try {
            // 获取当前管理员用户
            String currentUsername = authentication.getName();
            User currentUser = userService.findByUsername(currentUsername).orElse(null);
            
            if (currentUser != null && currentUser.getId().equals(id)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "不能删除自己的账户");
                return ResponseEntity.badRequest().body(error);
            }
            
            Optional<User> userToDelete = userService.findById(id);
            if (!userToDelete.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            // 检查是否是管理员账户
            if (userToDelete.get().getRole() == UserRole.ADMIN) {
                // 检查是否是最后一个管理员
                long adminCount = userService.countUsersByRole(UserRole.ADMIN);
                if (adminCount <= 1) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "不能删除最后一个管理员账户");
                    return ResponseEntity.badRequest().body(error);
                }
            }
            
            userService.deleteUser(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "用户删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "删除用户失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> batchDeleteUsers(@RequestBody List<Long> userIds, Authentication authentication) {
        try {
            // 获取当前管理员用户
            String currentUsername = authentication.getName();
            User currentUser = userService.findByUsername(currentUsername).orElse(null);
            
            if (currentUser != null && userIds.contains(currentUser.getId())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "不能删除自己的账户");
                return ResponseEntity.badRequest().body(error);
            }
            
            userService.batchDeleteUsers(userIds);
            Map<String, String> response = new HashMap<>();
            response.put("message", "批量删除用户成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "批量删除用户失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 重置用户密码
     */
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetUserPassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("newPassword");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "新密码不能为空");
                return ResponseEntity.badRequest().body(error);
            }
            
            userService.resetUserPassword(id, newPassword);
            Map<String, String> response = new HashMap<>();
            response.put("message", "密码重置成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "重置密码失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", userService.countAllUsers());
            stats.put("doctorCount", userService.countUsersByRole(UserRole.DOCTOR));
            stats.put("patientCount", userService.countUsersByRole(UserRole.PATIENT));
            stats.put("nurseCount", userService.countUsersByRole(UserRole.NURSE));
            stats.put("anesthesiologistCount", userService.countUsersByRole(UserRole.ANESTHESIOLOGIST));
            stats.put("adminCount", userService.countUsersByRole(UserRole.ADMIN));
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取用户统计信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
} 