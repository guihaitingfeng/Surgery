package com.surgery.service;

import com.surgery.entity.User;
import com.surgery.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;

public interface IUserService extends UserDetailsService {
    
    User registerUser(User user);
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findById(Long id);
    
    List<User> findByRole(UserRole role);
    
    User saveUser(User user);
    
    long countAllUsers();
    
    long countUsersByRole(UserRole role);
    
    // 用户管理相关方法
    Page<User> findAllUsers(Pageable pageable);
    
    Page<User> findUsersByRole(UserRole role, Pageable pageable);
    
    User createUser(User user);
    
    User updateUser(Long id, User userDetails);
    
    User toggleUserStatus(Long id);
    
    void deleteUser(Long id);
    
    void batchDeleteUsers(List<Long> userIds);
    
    void resetUserPassword(Long id, String newPassword);
    
    // 添加缺失的方法定义
    void deactivateUser(Long id);
    
    void activateUser(Long id);
    
    List<User> findActiveAnesthesiologistsAndNurses();
    
    List<User> findDoctorsByDepartment(String department);
    
    Page<User> searchUsers(String keyword, Pageable pageable);
    
    boolean changePassword(Long userId, String oldPassword, String newPassword);
} 