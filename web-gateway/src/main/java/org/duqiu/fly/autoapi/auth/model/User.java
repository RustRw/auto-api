package org.duqiu.fly.autoapi.auth.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.duqiu.fly.autoapi.common.model.TenantAwareBaseEntity;

@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends TenantAwareBaseEntity {
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 100)
    private String email;

    @Column(length = 50)
    private String realName;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    public enum Role {
        ADMIN, USER
    }
}