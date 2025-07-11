package com.surgery.service;

import com.surgery.entity.User;
import com.surgery.enums.UserRole;
import com.surgery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
    }

    @Override
    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setIsActive(true); // 明确设置用户为激活状态
        return userRepository.save(user);
    }

    @Override
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 只更新提供的字段
        if (userDetails.getRealName() != null) {
            user.setRealName(userDetails.getRealName());
        }
        if (userDetails.getEmail() != null) {
            user.setEmail(userDetails.getEmail());
        }
        if (userDetails.getPhone() != null) {
            user.setPhone(userDetails.getPhone());
        }
        if (userDetails.getGender() != null) {
            user.setGender(userDetails.getGender());
        }
        if (userDetails.getBirthDate() != null) {
            user.setBirthDate(userDetails.getBirthDate());
        }
        if (userDetails.getDepartment() != null) {
            user.setDepartment(userDetails.getDepartment());
        }
        if (userDetails.getProfessionalTitle() != null) {
            user.setProfessionalTitle(userDetails.getProfessionalTitle());
        }
        if (userDetails.getLicenseNumber() != null) {
            user.setLicenseNumber(userDetails.getLicenseNumber());
        }
        if (userDetails.getRole() != null) {
            user.setRole(userDetails.getRole());
        }
        if (userDetails.getEnabled() != null) {
            user.setIsActive(userDetails.getEnabled());
        }
        
        return userRepository.save(user);
    }

    @Override
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Override
    public void activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setIsActive(true);
        userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findByIdSafe(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsernameSafe(username);
    }

    @Override
    public List<User> findByRole(UserRole role) {
        return userRepository.findByRoleAndIsActiveTrue(role);
    }

    @Override
    public List<User> findActiveAnesthesiologistsAndNurses() {
        return userRepository.findActiveAnesthesiologistsAndNurses();
    }

    @Override
    public List<User> findDoctorsByDepartment(String department) {
        return userRepository.findActiveUsersByRoleAndDepartment(UserRole.DOCTOR, department);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.searchActiveUsers(keyword.trim(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findUsersByRole(UserRole role, Pageable pageable) {
        return userRepository.findByRoleAndIsActiveTrue(role, pageable);
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return false;
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    @Override
    public long countAllUsers() {
        return userRepository.count();
    }
    
    @Override
    public long countUsersByRole(UserRole role) {
        return userRepository.countByRole(role);
    }

    @Override
    public User registerUser(User user) {
        return createUser(user);
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    @Override
    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    @Override
    public User toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        Boolean newStatus = !user.getIsActive();
        userRepository.updateUserStatus(id, newStatus);
        
        // 重新获取用户以返回更新后的状态
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
    
    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        userRepository.delete(user);
    }
    
    @Override
    public void batchDeleteUsers(List<Long> userIds) {
        List<User> users = userRepository.findAllById(userIds);
        userRepository.deleteAll(users);
    }
    
    @Override
    public void resetUserPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    /**
     * 检查并修复role为null的用户记录
     */
    public void fixUsersWithNullRole() {
        List<User> usersWithNullRole = userRepository.findUsersWithNullRole();
        if (!usersWithNullRole.isEmpty()) {
            System.out.println("发现 " + usersWithNullRole.size() + " 个用户的角色为空，正在修复...");
            for (User user : usersWithNullRole) {
                // 根据用户名或其他信息推断角色，默认设置为PATIENT
                user.setRole(UserRole.PATIENT);
                userRepository.save(user);
                System.out.println("修复用户: " + user.getUsername() + " 角色设置为PATIENT");
            }
        }
    }
} 