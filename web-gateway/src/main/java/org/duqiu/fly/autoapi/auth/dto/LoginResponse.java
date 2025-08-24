package org.duqiu.fly.autoapi.auth.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private String tokenType = "Bearer";
    private String username;
    private String email;
    private String realName;
    private String role;

    public LoginResponse(String token, String username, String email, String realName, String role) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.realName = realName;
        this.role = role;
    }
}