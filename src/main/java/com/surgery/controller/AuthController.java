package com.surgery.controller;

import com.surgery.entity.User;
import com.surgery.service.IUserService;
import com.surgery.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final IUserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // 先检查用户是否存在
            User user = userService.findByUsername(loginRequest.getUsername()).orElse(null);
            if (user == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "用户名或密码错误");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 检查用户是否被禁用
            if (!user.getIsActive()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "用户已被禁用，请联系管理员");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 验证密码
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "用户名或密码错误");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 进行认证
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("type", "Bearer");
            response.put("user", createUserResponse(user));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "登录失败，请稍后重试");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setPassword(registerRequest.getPassword());
            user.setEmail(registerRequest.getEmail());
            user.setRealName(registerRequest.getRealName());
            user.setPhone(registerRequest.getPhone());
            user.setRole(registerRequest.getRole());
            user.setGender(registerRequest.getGender());
            user.setBirthDate(registerRequest.getBirthDate());
            user.setDepartment(registerRequest.getDepartment());
            user.setProfessionalTitle(registerRequest.getProfessionalTitle());
            user.setLicenseNumber(registerRequest.getLicenseNumber());

            User savedUser = userService.createUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "注册成功");
            response.put("user", createUserResponse(savedUser));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "退出登录成功");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        String username = authentication.getName();
        User user = userService.findByUsername(username).orElse(null);
        
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(createUserResponse(user));
    }

    // 调试端点 - 测试密码验证
    @PostMapping("/debug/password")
    public ResponseEntity<?> debugPassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(Map.of("error", "用户不存在"));
        }
        
        Map<String, Object> debug = new HashMap<>();
        debug.put("username", username);
        debug.put("inputPassword", password);
        debug.put("storedPassword", user.getPassword());
        debug.put("isActive", user.getIsActive());
        debug.put("isEnabled", user.isEnabled());
        debug.put("isAccountNonLocked", user.isAccountNonLocked());
        debug.put("passwordMatches", passwordEncoder.matches(password, user.getPassword()));
        
        // 测试重新加密相同密码
        String newEncoded = passwordEncoder.encode(password);
        debug.put("newEncoded", newEncoded);
        debug.put("newMatches", passwordEncoder.matches(password, newEncoded));
        
        return ResponseEntity.ok(debug);
    }
    
    // 调试端点 - 测试AuthenticationManager
    @PostMapping("/debug/auth")
    public ResponseEntity<?> debugAuth(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        
        Map<String, Object> debug = new HashMap<>();
        debug.put("username", username);
        debug.put("password", password);
        
        try {
            // 测试UserDetailsService
            org.springframework.security.core.userdetails.UserDetails userDetails = 
                userService.loadUserByUsername(username);
            debug.put("userDetailsLoaded", true);
            debug.put("userDetailsUsername", userDetails.getUsername());
            debug.put("userDetailsPassword", userDetails.getPassword());
            debug.put("userDetailsEnabled", userDetails.isEnabled());
            debug.put("userDetailsAccountNonLocked", userDetails.isAccountNonLocked());
            debug.put("userDetailsAuthorities", userDetails.getAuthorities().toString());
            
            // 测试AuthenticationManager
            Authentication authRequest = new UsernamePasswordAuthenticationToken(username, password);
            debug.put("authRequestCreated", true);
            
            Authentication authResult = authenticationManager.authenticate(authRequest);
            debug.put("authenticationSuccess", true);
            debug.put("authResultPrincipal", authResult.getPrincipal().getClass().getSimpleName());
            debug.put("authResultAuthenticated", authResult.isAuthenticated());
            
        } catch (Exception e) {
            debug.put("authenticationError", true);
            debug.put("errorMessage", e.getMessage());
            debug.put("errorClass", e.getClass().getSimpleName());
            debug.put("stackTrace", java.util.Arrays.toString(e.getStackTrace()));
        }
        
        return ResponseEntity.ok(debug);
    }
    
    // 调试端点 - 完整登录流程测试
    @PostMapping("/debug/login")
    public ResponseEntity<?> debugLogin(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        
        Map<String, Object> debug = new HashMap<>();
        debug.put("step", "开始登录调试");
        debug.put("username", username);
        
        try {
            debug.put("step1", "开始认证");
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
            debug.put("step1_result", "认证成功");

            debug.put("step2", "获取UserDetails");
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            debug.put("step2_result", "UserDetails获取成功: " + userDetails.getUsername());
            
            debug.put("step3", "生成JWT Token");
            String token = jwtUtil.generateToken(userDetails);
            debug.put("step3_result", token != null ? "Token生成成功" : "Token生成失败");
            debug.put("step3_token", token != null ? token.substring(0, Math.min(50, token.length())) + "..." : null);
            
            debug.put("step4", "查找用户信息");
            User user = userService.findByUsername(userDetails.getUsername()).orElse(null);
            debug.put("step4_result", user != null ? "用户查找成功" : "用户查找失败");
            debug.put("step4_user_id", user != null ? user.getId() : null);

            debug.put("step5", "创建响应对象");
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("type", "Bearer");
            response.put("user", createUserResponse(user));
            debug.put("step5_result", "响应创建成功");
            debug.put("step5_response_keys", response.keySet().toString());
            
            debug.put("final_result", "登录流程全部成功");
            debug.put("actual_response", response);

            return ResponseEntity.ok(debug);
        } catch (Exception e) {
            debug.put("error_occurred", true);
            debug.put("error_class", e.getClass().getSimpleName());
            debug.put("error_message", e.getMessage());
            debug.put("error_stack", java.util.Arrays.toString(e.getStackTrace()));
            return ResponseEntity.ok(debug);
        }
    }

    // 临时管理员登录端点 - 用于解决密码问题
    @PostMapping("/admin-login")
    public ResponseEntity<?> adminLogin(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // 检查是否是管理员账号
            if (!"admin".equals(loginRequest.getUsername())) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "此端点仅供管理员使用");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 查找管理员用户
            User user = userService.findByUsername("admin").orElse(null);
            if (user == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "管理员用户不存在");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 临时允许明文密码 "password" 或验证BCrypt密码
            boolean passwordValid = false;
            if ("password".equals(loginRequest.getPassword())) {
                passwordValid = true;
            } else {
                passwordValid = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
            }
            
            if (!passwordValid) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "密码错误");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 检查用户是否为管理员角色
            if (user.getRole() != com.surgery.enums.UserRole.ADMIN) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "用户不是管理员");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 生成JWT token
            String token = jwtUtil.generateToken(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("type", "Bearer");
            response.put("user", createUserResponse(user));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "登录失败");
            errorResponse.put("details", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put("username", user.getUsername());
        userResponse.put("email", user.getEmail());
        userResponse.put("realName", user.getRealName());
        userResponse.put("phone", user.getPhone());
        userResponse.put("role", user.getRole());
        userResponse.put("gender", user.getGender());
        userResponse.put("birthDate", user.getBirthDate());
        userResponse.put("department", user.getDepartment());
        userResponse.put("professionalTitle", user.getProfessionalTitle());
        userResponse.put("licenseNumber", user.getLicenseNumber());
        userResponse.put("isActive", user.getIsActive());
        return userResponse;
    }

    // 内部类用于接收请求数据
    public static class LoginRequest {
        private String username;
        private String password;
        
        // getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private String realName;
        private String phone;
        private com.surgery.enums.UserRole role;
        private com.surgery.enums.Gender gender;
        private java.time.LocalDate birthDate;
        private String department;
        private String professionalTitle;
        private String licenseNumber;
        
        // getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public com.surgery.enums.UserRole getRole() { return role; }
        public void setRole(com.surgery.enums.UserRole role) { this.role = role; }
        public com.surgery.enums.Gender getGender() { return gender; }
        public void setGender(com.surgery.enums.Gender gender) { this.gender = gender; }
        public java.time.LocalDate getBirthDate() { return birthDate; }
        public void setBirthDate(java.time.LocalDate birthDate) { this.birthDate = birthDate; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public String getProfessionalTitle() { return professionalTitle; }
        public void setProfessionalTitle(String professionalTitle) { this.professionalTitle = professionalTitle; }
        public String getLicenseNumber() { return licenseNumber; }
        public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    }
} 