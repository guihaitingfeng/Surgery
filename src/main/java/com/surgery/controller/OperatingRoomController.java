package com.surgery.controller;

import com.surgery.entity.OperatingBed;
import com.surgery.entity.OperatingRoom;
import com.surgery.service.IOperatingRoomService;
import com.surgery.repository.OperatingBedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OperatingRoomController {

    private final IOperatingRoomService operatingRoomService;
    private final OperatingBedRepository operatingBedRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<?> getAllRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<OperatingRoom> rooms = operatingRoomService.findAllRooms(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", rooms.getContent());
            response.put("totalElements", rooms.getTotalElements());
            response.put("totalPages", rooms.getTotalPages());
            response.put("currentPage", rooms.getNumber());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取手术室列表失败");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('NURSE') or hasRole('ANESTHESIOLOGIST')")
    public ResponseEntity<?> getAvailableRooms() {
        try {
            List<OperatingRoom> availableRooms = operatingRoomService.findAvailableRooms();
            return ResponseEntity.ok(availableRooms);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取可用手术室失败");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<?> getRoomById(@PathVariable Long id) {
        try {
            OperatingRoom room = operatingRoomService.findById(id)
                    .orElseThrow(() -> new RuntimeException("手术室不存在"));
            return ResponseEntity.ok(room);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取手术室详情失败");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createRoom(@Valid @RequestBody RoomRequest roomRequest) {
        try {
            OperatingRoom room = new OperatingRoom();
            room.setRoomNumber(roomRequest.getRoomNumber());
            room.setRoomName(roomRequest.getRoomName());
            room.setFloorNumber(roomRequest.getFloorNumber());
            room.setEquipmentList(roomRequest.getEquipmentList());
            room.setStatus(roomRequest.getStatus() != null ? roomRequest.getStatus() : "AVAILABLE");

            OperatingRoom savedRoom = operatingRoomService.createRoom(room);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "手术室创建成功");
            response.put("room", savedRoom);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "创建手术室失败");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest roomRequest) {
        try {
            OperatingRoom room = operatingRoomService.findById(id)
                    .orElseThrow(() -> new RuntimeException("手术室不存在"));

            room.setRoomNumber(roomRequest.getRoomNumber());
            room.setRoomName(roomRequest.getRoomName());
            room.setFloorNumber(roomRequest.getFloorNumber());
            room.setEquipmentList(roomRequest.getEquipmentList());
            room.setStatus(roomRequest.getStatus());

            OperatingRoom updatedRoom = operatingRoomService.updateRoom(room);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "手术室更新成功");
            response.put("room", updatedRoom);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "更新手术室失败");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        try {
            operatingRoomService.deleteRoom(id);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "手术室删除成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "删除手术室失败");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<?> updateRoomStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String status = request.get("status");
            if (status == null || status.trim().isEmpty()) {
                throw new RuntimeException("状态不能为空");
            }

            OperatingRoom updatedRoom = operatingRoomService.updateRoomStatus(id, status);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "手术室状态更新成功");
            response.put("room", updatedRoom);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "更新手术室状态失败");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 床位管理相关API
    @GetMapping("/{roomId}/beds")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or hasRole('NURSE') or hasRole('ANESTHESIOLOGIST')")
    public ResponseEntity<?> getRoomBeds(@PathVariable Long roomId) {
        try {
            List<OperatingBed> beds = operatingBedRepository.findByRoomId(roomId);
            return ResponseEntity.ok(beds);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "获取床位列表失败");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/{roomId}/beds")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createBed(@PathVariable Long roomId, @Valid @RequestBody BedRequest bedRequest) {
        try {
            OperatingRoom room = operatingRoomService.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("手术室不存在"));

            if (operatingBedRepository.existsByRoomIdAndBedNumber(roomId, bedRequest.getBedNumber())) {
                throw new RuntimeException("床位号已存在");
            }

            OperatingBed bed = new OperatingBed();
            bed.setRoom(room);
            bed.setBedNumber(bedRequest.getBedNumber());
            bed.setBedType(bedRequest.getBedType());
            bed.setStatus(bedRequest.getStatus() != null ? bedRequest.getStatus() : "AVAILABLE");

            OperatingBed savedBed = operatingBedRepository.save(bed);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "床位创建成功");
            response.put("bed", savedBed);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "创建床位失败");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PutMapping("/{roomId}/beds/{bedId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateBed(@PathVariable Long roomId, @PathVariable Long bedId, @Valid @RequestBody BedRequest bedRequest) {
        try {
            OperatingBed bed = operatingBedRepository.findById(bedId)
                    .orElseThrow(() -> new RuntimeException("床位不存在"));

            if (!bed.getRoom().getId().equals(roomId)) {
                throw new RuntimeException("床位不属于该手术室");
            }

            // 检查床位号是否被其他床位使用
            if (!bed.getBedNumber().equals(bedRequest.getBedNumber()) && 
                operatingBedRepository.existsByRoomIdAndBedNumber(roomId, bedRequest.getBedNumber())) {
                throw new RuntimeException("床位号已存在");
            }

            bed.setBedNumber(bedRequest.getBedNumber());
            bed.setBedType(bedRequest.getBedType());
            bed.setStatus(bedRequest.getStatus());

            OperatingBed updatedBed = operatingBedRepository.save(bed);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "床位更新成功");
            response.put("bed", updatedBed);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "更新床位失败");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @DeleteMapping("/{roomId}/beds/{bedId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteBed(@PathVariable Long roomId, @PathVariable Long bedId) {
        try {
            OperatingBed bed = operatingBedRepository.findById(bedId)
                    .orElseThrow(() -> new RuntimeException("床位不存在"));

            if (!bed.getRoom().getId().equals(roomId)) {
                throw new RuntimeException("床位不属于该手术室");
            }

            operatingBedRepository.delete(bed);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "床位删除成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "删除床位失败");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 内部类用于接收请求数据
    public static class RoomRequest {
        private String roomNumber;
        private String roomName;
        private Integer floorNumber;
        private String equipmentList;
        private String status;

        // getters and setters
        public String getRoomNumber() { return roomNumber; }
        public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
        public String getRoomName() { return roomName; }
        public void setRoomName(String roomName) { this.roomName = roomName; }
        public Integer getFloorNumber() { return floorNumber; }
        public void setFloorNumber(Integer floorNumber) { this.floorNumber = floorNumber; }
        public String getEquipmentList() { return equipmentList; }
        public void setEquipmentList(String equipmentList) { this.equipmentList = equipmentList; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class BedRequest {
        private String bedNumber;
        private String bedType;
        private String status;

        // getters and setters
        public String getBedNumber() { return bedNumber; }
        public void setBedNumber(String bedNumber) { this.bedNumber = bedNumber; }
        public String getBedType() { return bedType; }
        public void setBedType(String bedType) { this.bedType = bedType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
} 