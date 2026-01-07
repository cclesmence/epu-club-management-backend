package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemRoleRepository extends JpaRepository<SystemRole, Long> {
    Optional<SystemRole> findByRoleName(String roleName);
}


