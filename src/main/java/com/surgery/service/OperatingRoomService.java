package com.surgery.service;

import com.surgery.entity.OperatingRoom;
import com.surgery.repository.OperatingRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OperatingRoomService implements IOperatingRoomService {

    private final OperatingRoomRepository operatingRoomRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OperatingRoom> findAllRooms(Pageable pageable) {
        return operatingRoomRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperatingRoom> findAvailableRooms() {
        return operatingRoomRepository.findByStatus("AVAILABLE");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OperatingRoom> findById(Long id) {
        return operatingRoomRepository.findById(id);
    }

    @Override
    public OperatingRoom createRoom(OperatingRoom room) {
        if (operatingRoomRepository.existsByRoomNumber(room.getRoomNumber())) {
            throw new RuntimeException("房间号已存在");
        }
        return operatingRoomRepository.save(room);
    }

    @Override
    public OperatingRoom updateRoom(OperatingRoom room) {
        OperatingRoom existingRoom = operatingRoomRepository.findById(room.getId())
                .orElseThrow(() -> new RuntimeException("手术室不存在"));
        
        // 检查房间号是否被其他手术室使用
        if (!existingRoom.getRoomNumber().equals(room.getRoomNumber()) && 
            operatingRoomRepository.existsByRoomNumber(room.getRoomNumber())) {
            throw new RuntimeException("房间号已存在");
        }
        
        return operatingRoomRepository.save(room);
    }

    @Override
    public void deleteRoom(Long id) {
        OperatingRoom room = operatingRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("手术室不存在"));
        
        // 可以添加检查是否有正在进行的手术预约
        // 这里暂时直接删除
        operatingRoomRepository.delete(room);
    }

    @Override
    public OperatingRoom updateRoomStatus(Long id, String status) {
        OperatingRoom room = operatingRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("手术室不存在"));
        
        // 验证状态值
        if (!isValidStatus(status)) {
            throw new RuntimeException("无效的状态值");
        }
        
        room.setStatus(status);
        return operatingRoomRepository.save(room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperatingRoom> findByStatus(String status) {
        return operatingRoomRepository.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAllRooms() {
        return operatingRoomRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return operatingRoomRepository.countByStatus(status);
    }

    private boolean isValidStatus(String status) {
        return status != null && (
            "AVAILABLE".equals(status) || 
            "OCCUPIED".equals(status) || 
            "MAINTENANCE".equals(status) || 
            "RESERVED".equals(status)
        );
    }
} 