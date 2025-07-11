package com.surgery.repository;

import com.surgery.entity.Blacklist;
import com.surgery.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {
    
    /**
     * 检查用户是否在黑名单中（当前日期在黑名单期间内）
     */
    @Query("SELECT b FROM Blacklist b WHERE b.user = :user AND :currentDate BETWEEN b.blacklistStartDate AND b.blacklistEndDate")
    Optional<Blacklist> findActiveBlacklistByUser(@Param("user") User user, @Param("currentDate") LocalDate currentDate);

    
    /**
     * 查找指定日期范围内的黑名单记录
     */
    @Query("SELECT b FROM Blacklist b WHERE b.blacklistStartDate <= :endDate AND b.blacklistEndDate >= :startDate")
    List<Blacklist> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 检查用户当前是否被拉黑
     */
    default boolean isUserBlacklisted(User user) {
        return findActiveBlacklistByUser(user, LocalDate.now()).isPresent();
    }
} 