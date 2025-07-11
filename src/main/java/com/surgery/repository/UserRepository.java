package com.surgery.repository;

import com.surgery.entity.User;
import com.surgery.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    List<User> findByRole(UserRole role);
    
    List<User> findByRoleAndIsActiveTrue(UserRole role);
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true AND u.department = :department")
    List<User> findActiveUsersByRoleAndDepartment(@Param("role") UserRole role, @Param("department") String department);
    
    @Query("SELECT u FROM User u WHERE u.role IN ('ANESTHESIOLOGIST', 'NURSE') AND u.isActive = true")
    List<User> findActiveAnesthesiologistsAndNurses();
    
    Page<User> findByRoleAndIsActiveTrue(UserRole role, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.isActive = true AND " +
           "(u.realName LIKE %:keyword% OR u.username LIKE %:keyword% OR u.email LIKE %:keyword%)")
    Page<User> searchActiveUsers(@Param("keyword") String keyword, Pageable pageable);
    
    long countByRole(UserRole role);
    
    @Modifying
    @Query("UPDATE User u SET u.isActive = :isActive WHERE u.id = :id")
    void updateUserStatus(@Param("id") Long id, @Param("isActive") Boolean isActive);
    
    @Query("SELECT u FROM User u WHERE u.role IS NULL")
    List<User> findUsersWithNullRole();
    
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.role IS NOT NULL")
    Optional<User> findByIdSafe(@Param("id") Long id);
    
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.role IS NOT NULL")
    Optional<User> findByUsernameSafe(@Param("username") String username);
} 