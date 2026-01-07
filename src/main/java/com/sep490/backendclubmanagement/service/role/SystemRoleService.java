package com.sep490.backendclubmanagement.service.role;

import com.sep490.backendclubmanagement.entity.SystemRole;
import com.sep490.backendclubmanagement.repository.SystemRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemRoleService {

    private final SystemRoleRepository systemRoleRepository;

    /**
     * Find system roles by name
     */
    public Optional<SystemRole> findByRoleName(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return Optional.empty();
        }
        return systemRoleRepository.findByRoleName(roleName.trim());
    }

    /**
     * Find system role by ID
     */
    public Optional<SystemRole> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return systemRoleRepository.findById(id);
    }

    /**
     * Save system role
     */
    @Transactional
    public SystemRole save(SystemRole systemRole) {
        if (systemRole == null) {
            throw new IllegalArgumentException("SystemRole cannot be null");
        }
        return systemRoleRepository.save(systemRole);
    }

    /**
     * Create new system role
     */
    @Transactional
    public SystemRole createRole(String roleName, String description) {
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Role name cannot be null or empty");
        }

        // Check if the role exists
        Optional<SystemRole> existingRole = findByRoleName(roleName);
        if (existingRole.isPresent()) {
            throw new IllegalArgumentException("SystemRole with name " + roleName + " already exists");
        }

        SystemRole systemRole = SystemRole.builder()
                .roleName(roleName.trim())
                .description(description != null ? description : "System role for " + roleName.toLowerCase())
                .build();

        return save(systemRole);
    }

    /**
     * Find or create a system role
     */
    @Transactional
    public SystemRole findOrCreateRole(String roleName) {
        final String finalRoleName = (roleName == null || roleName.trim().isEmpty()) ? "STUDENT" : roleName.trim();

        return findByRoleName(finalRoleName)
                .orElseGet(() -> createRole(finalRoleName, "System role for " + finalRoleName.toLowerCase()));
    }

    /**
     * Update system role
     */
    @Transactional
    public SystemRole updateRole(SystemRole systemRole) {
        if (systemRole == null || systemRole.getId() == null) {
            throw new IllegalArgumentException("SystemRole and role ID cannot be null");
        }

        Optional<SystemRole> existingRole = findById(systemRole.getId());
        if (existingRole.isEmpty()) {
            throw new IllegalArgumentException("SystemRole with ID " + systemRole.getId() + " not found");
        }

        return save(systemRole);
    }

    /**
     * Get all system roles
     */
    public List<SystemRole> findAll() {
        return systemRoleRepository.findAll();
    }

    /**
     * Delete system role
     */
    @Transactional
    public void deleteRole(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }

        Optional<SystemRole> roleOpt = findById(roleId);
        if (roleOpt.isEmpty()) {
            throw new IllegalArgumentException("SystemRole with ID " + roleId + " not found");
        }

        SystemRole role = roleOpt.get();
        
        // Kiểm tra xem có user nào đang sử dụng role này không
        if (role.getUsers() != null && !role.getUsers().isEmpty()) {
            throw new IllegalStateException("Cannot delete system role that is being used by users");
        }

        systemRoleRepository.delete(role);
    }

    /**
     * Check if system role exists
     */
    public boolean existsByRoleName(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return false;
        }
        return systemRoleRepository.findByRoleName(roleName.trim()).isPresent();
    }

    /**
     * Get a list of role names
     */
    public List<String> getAllRoleNames() {
        return findAll().stream()
                .map(SystemRole::getRoleName)
                .toList();
    }

}
