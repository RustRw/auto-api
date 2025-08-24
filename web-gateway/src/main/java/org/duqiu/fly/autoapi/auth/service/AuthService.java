package org.duqiu.fly.autoapi.auth.service;

import org.duqiu.fly.autoapi.auth.dto.LoginRequest;
import org.duqiu.fly.autoapi.auth.dto.LoginResponse;
import org.duqiu.fly.autoapi.auth.dto.RegisterRequest;
import org.duqiu.fly.autoapi.auth.model.User;
import org.duqiu.fly.autoapi.auth.repository.UserRepository;
import org.duqiu.fly.autoapi.auth.repository.TenantRepository;
import org.duqiu.fly.autoapi.common.model.Tenant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    
    public AuthService(UserRepository userRepository, 
                      TenantRepository tenantRepository,
                      PasswordEncoder passwordEncoder,
                      AuthenticationManager authenticationManager,
                      JwtTokenProvider jwtTokenProvider,
                      UserDetailsServiceImpl userDetailsService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }
    
    public LoginResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getUsername());
        
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 获取用户的租户信息
        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new RuntimeException("租户信息不存在"));
        
        if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
            throw new RuntimeException("租户已被暂停或删除");
        }
        
        // 生成包含租户信息的JWT token
        String token = jwtTokenProvider.generateToken(userDetails, tenant.getId(), 
                                                    tenant.getTenantCode(), user.getId());
                
        return new LoginResponse(token, user.getUsername(), user.getEmail(), 
                               user.getRealName(), user.getRole().name());
    }
    
    public void register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        
        if (registerRequest.getEmail() != null && userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }
        
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setRealName(registerRequest.getRealName());
        user.setEnabled(true);
        user.setRole(User.Role.USER);
        
        userRepository.save(user);
    }
}