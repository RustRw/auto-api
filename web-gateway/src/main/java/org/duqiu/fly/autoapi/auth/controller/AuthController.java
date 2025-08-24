package org.duqiu.fly.autoapi.auth.controller;

import jakarta.validation.Valid;
import org.duqiu.fly.autoapi.auth.dto.LoginRequest;
import org.duqiu.fly.autoapi.auth.dto.LoginResponse;
import org.duqiu.fly.autoapi.auth.dto.RegisterRequest;
import org.duqiu.fly.autoapi.auth.service.AuthService;
import org.duqiu.fly.autoapi.common.dto.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse loginResponse = authService.login(loginRequest);
            return Result.success(loginResponse);
        } catch (Exception e) {
            return Result.error("登录失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            authService.register(registerRequest);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("注册失败: " + e.getMessage());
        }
    }
}