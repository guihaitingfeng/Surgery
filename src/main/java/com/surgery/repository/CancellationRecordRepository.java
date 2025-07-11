package com.surgery.repository;

import com.surgery.entity.CancellationRecord;
import com.surgery.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CancellationRecordRepository extends JpaRepository<CancellationRecord, Long> {
    
    /**
     * 统计用户在指定月份的取消次数
     */
    @Query("SELECT COUNT(c) FROM CancellationRecord c WHERE c.user = :user AND YEAR(c.cancellationDate) = :year AND MONTH(c.cancellationDate) = :month")
    Long countByUserAndMonth(@Param("user") User user, @Param("year") int year, @Param("month") int month);

    /**
     * 查找用户的所有取消记录
     */
    List<CancellationRecord> findByUserOrderByCancellationTimeDesc(User user);

    /**
     * 查找用户在指定月份的取消记录
     */
    @Query("SELECT c FROM CancellationRecord c WHERE c.user = :user AND YEAR(c.cancellationDate) = :year AND MONTH(c.cancellationDate) = :month ORDER BY c.cancellationTime DESC")
    List<CancellationRecord> findByUserAndMonth(@Param("user") User user, @Param("year") int year, @Param("month") int month);

    /**
     * 查找指定日期范围内的取消记录
     */
    @Query("SELECT c FROM CancellationRecord c WHERE c.cancellationDate BETWEEN :startDate AND :endDate ORDER BY c.cancellationTime DESC")
    List<CancellationRecord> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 查找48小时内取消的记录（违规取消）
     */
    @Query("SELECT c FROM CancellationRecord c WHERE c.hoursBeforeSurgery < 48 ORDER BY c.cancellationTime DESC")
    List<CancellationRecord> findViolationCancellations();

    /**
     * 删除指定预约的取消记录
     */
    void deleteByAppointment(com.surgery.entity.SurgeryAppointment appointment);
} 