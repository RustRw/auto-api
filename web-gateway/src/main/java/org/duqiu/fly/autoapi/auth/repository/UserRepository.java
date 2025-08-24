package org.duqiu.fly.autoapi.auth.repository;

import org.duqiu.fly.autoapi.auth.model.User;
import org.duqiu.fly.autoapi.common.repository.TenantAwareRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends TenantAwareRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByUsernameAndTenantId(String username, Long tenantId);
    
    boolean existsByUsername(String username);
    
    boolean existsByUsernameAndTenantId(String username, Long tenantId);
    
    boolean existsByEmail(String email);
    
    boolean existsByEmailAndTenantId(String email, Long tenantId);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.enabled = true")
    long countByTenantId(@Param("tenantId") Long tenantId);
}