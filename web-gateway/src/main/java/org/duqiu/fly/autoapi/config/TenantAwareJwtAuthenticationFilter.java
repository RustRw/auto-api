package org.duqiu.fly.autoapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.duqiu.fly.autoapi.auth.service.JwtTokenProvider;
import org.duqiu.fly.autoapi.auth.service.UserDetailsServiceImpl;
import org.duqiu.fly.autoapi.common.context.TenantContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 支持多租户的JWT认证过滤器
 */
public class TenantAwareJwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    
    public TenantAwareJwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, 
                                           UserDetailsServiceImpl userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String header = request.getHeader("Authorization");
        String token = null;
        String username = null;
        
        try {
            if (header != null && header.startsWith("Bearer ")) {
                token = header.substring(7);
                username = jwtTokenProvider.getUsernameFromToken(token);
                
                // 提取租户信息并设置到上下文中
                if (username != null) {
                    Long tenantId = jwtTokenProvider.getTenantIdFromToken(token);
                    String tenantCode = jwtTokenProvider.getTenantCodeFromToken(token);
                    Long userId = jwtTokenProvider.getUserIdFromToken(token);
                    
                    if (tenantId != null && tenantCode != null && userId != null) {
                        TenantContext.setTenantId(tenantId);
                        TenantContext.setTenantCode(tenantCode);
                        TenantContext.setUserId(userId);
                    }
                }
            }
            
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                if (jwtTokenProvider.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            logger.error("JWT token validation failed", e);
            TenantContext.clear();
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 清理线程本地变量
            TenantContext.clear();
        }
    }
}