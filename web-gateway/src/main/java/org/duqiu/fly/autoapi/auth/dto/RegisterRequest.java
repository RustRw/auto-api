package org.duqiu.fly.autoapi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度应在3-50字符之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度应在6-100字符之间")
    private String password;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Size(max = 50, message = "真实姓名长度不能超过50字符")
    private String realName;
}