-- 手术预约与排班管理系统数据库设计

-- 设置数据库字符集
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(20),
    role ENUM('PATIENT', 'DOCTOR', 'NURSE', 'ANESTHESIOLOGIST', 'ADMIN') NOT NULL,
    real_name VARCHAR(100) NOT NULL,
    gender ENUM('MALE', 'FEMALE') NOT NULL,
    birth_date DATE,
    department VARCHAR(100),
    professional_title VARCHAR(100),
    license_number VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 病人信息表
CREATE TABLE IF NOT EXISTS patients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    medical_record_number VARCHAR(50) UNIQUE NOT NULL,
    id_card VARCHAR(18),
    emergency_contact VARCHAR(100),
    emergency_phone VARCHAR(20),
    medical_history TEXT,
    allergies TEXT,
    current_medications TEXT,
    admission_date DATE,
    ward_number VARCHAR(50),
    bed_number VARCHAR(20),
    disease_description TEXT NOT NULL,
    severity_level ENUM('EMERGENCY', 'URGENT', 'NORMAL', 'LOW') DEFAULT 'NORMAL',
    assigned_doctor_id BIGINT,
    status ENUM('WAITING', 'SCHEDULED', 'COMPLETED', 'CANCELLED') DEFAULT 'WAITING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (assigned_doctor_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 手术室表
CREATE TABLE IF NOT EXISTS operating_rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(20) UNIQUE NOT NULL,
    room_name VARCHAR(100) NOT NULL,
    floor_number INT,
    equipment_list TEXT,
    capacity INT DEFAULT 1,
    status ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'RESERVED') DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 手术床位表
CREATE TABLE IF NOT EXISTS operating_beds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    bed_number VARCHAR(20) NOT NULL,
    bed_type VARCHAR(50),
    status ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE') DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES operating_rooms(id),
    UNIQUE KEY unique_room_bed (room_id, bed_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 手术预约表
CREATE TABLE IF NOT EXISTS surgery_appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    anesthesiologist_id BIGINT,
    nurse_id BIGINT,
    room_id BIGINT NOT NULL,
    bed_id BIGINT NOT NULL,
    surgery_name VARCHAR(200) NOT NULL,
    surgery_type VARCHAR(100),
    planned_date DATE NOT NULL,
    planned_start_time TIME NOT NULL,
    planned_end_time TIME NOT NULL,
    estimated_duration INT NOT NULL, -- 预计时长(分钟)
    actual_start_time DATETIME,
    actual_end_time DATETIME,
    surgery_description TEXT,
    pre_surgery_notes TEXT,
    post_surgery_notes TEXT,
    status VARCHAR(30) DEFAULT 'SCHEDULED',
    priority_level ENUM('EMERGENCY', 'URGENT', 'NORMAL', 'LOW') DEFAULT 'NORMAL',
    anesthesiologist_confirmed BOOLEAN DEFAULT FALSE,
    nurse_confirmed BOOLEAN DEFAULT FALSE,
    anesthesiologist_confirmed_at TIMESTAMP NULL,
    nurse_confirmed_at TIMESTAMP NULL,
    doctor_final_confirmed BOOLEAN DEFAULT FALSE,
    doctor_final_confirmed_at TIMESTAMP NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (doctor_id) REFERENCES users(id),
    FOREIGN KEY (anesthesiologist_id) REFERENCES users(id),
    FOREIGN KEY (nurse_id) REFERENCES users(id),
    FOREIGN KEY (room_id) REFERENCES operating_rooms(id),
    FOREIGN KEY (bed_id) REFERENCES operating_beds(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 手术记录表
CREATE TABLE IF NOT EXISTS surgery_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    surgery_result ENUM('SUCCESS', 'COMPLICATED', 'FAILED') NOT NULL,
    complications TEXT,
    anesthesia_type VARCHAR(100),
    blood_loss_ml INT,
    surgery_notes TEXT,
    pathology_report TEXT,
    recovery_notes TEXT,
    discharge_date DATE,
    follow_up_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (appointment_id) REFERENCES surgery_appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 排班表
CREATE TABLE IF NOT EXISTS schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    schedule_date DATE NOT NULL,
    shift_type ENUM('MORNING', 'AFTERNOON', 'NIGHT', 'FULL_DAY') NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    max_surgeries INT DEFAULT 3,
    current_surgeries INT DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY unique_user_date_shift (user_id, schedule_date, shift_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 通知消息表
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    sender_id BIGINT,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    related_appointment_id BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recipient_id) REFERENCES users(id),
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (related_appointment_id) REFERENCES surgery_appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 黑名单表
CREATE TABLE IF NOT EXISTS blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    blacklist_start_date DATE NOT NULL,
    blacklist_end_date DATE NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 预约取消记录表
CREATE TABLE IF NOT EXISTS cancellation_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    appointment_id BIGINT NOT NULL,
    cancellation_date DATE NOT NULL,
    cancellation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    hours_before_surgery INT NOT NULL,
    reason TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (appointment_id) REFERENCES surgery_appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建索引以提高查询性能
CREATE INDEX idx_patients_assigned_doctor ON patients(assigned_doctor_id);
CREATE INDEX idx_appointments_date ON surgery_appointments(planned_date);
CREATE INDEX idx_appointments_doctor ON surgery_appointments(doctor_id);
CREATE INDEX idx_appointments_status ON surgery_appointments(status);
CREATE INDEX idx_schedules_date ON schedules(schedule_date);
CREATE INDEX idx_schedules_user ON schedules(user_id);
CREATE INDEX idx_notifications_recipient ON notifications(recipient_id);
CREATE INDEX idx_blacklist_user ON blacklist(user_id);

-- 插入默认管理员用户
INSERT INTO users (username, password, email, phone, role, real_name, gender, birth_date, department, professional_title, is_active, created_at, updated_at) 
VALUES (
    'admin', 
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', -- 密码是 'password'，已经用BCrypt加密
    'admin@hospital.com', 
    '13800000000', 
    'ADMIN', 
    '系统管理员', 
    'MALE', 
    '1980-01-01', 
    '信息技术部', 
    '系统管理员', 
    TRUE, 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP
) ON DUPLICATE KEY UPDATE username = username; 