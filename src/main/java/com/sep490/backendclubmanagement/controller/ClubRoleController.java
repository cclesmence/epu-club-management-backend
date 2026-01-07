package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.CreateClubRoleRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateClubRoleRequest;
import com.sep490.backendclubmanagement.dto.response.ClubRoleResponse;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.service.club.role.ClubRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/management/clubs/{clubId}/roles")
@RequiredArgsConstructor
public class ClubRoleController {

    private final ClubRoleService clubRoleService;

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        Object principal = auth.getPrincipal();
        return !(principal instanceof String s && "anonymousUser".equalsIgnoreCase(s));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClubRoleResponse>>> getClubRoles(
            @PathVariable Long clubId
    ) {
        if (!isAuthenticated()) {
            return ResponseEntity.status(401).body(
                    ApiResponse.error(ErrorCode.UNAUTHORIZED, "Unauthorized", null)
            );
        }

        List<ClubRoleResponse> data = clubRoleService.getClubRolesByClubId(clubId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }


    @PostMapping
    public ResponseEntity<ApiResponse<ClubRoleResponse>> createClubRole(
            @PathVariable Long clubId,
            @Valid @RequestBody CreateClubRoleRequest request
    ) throws AppException {   //  <-- THÊM throws ở đây
        if (!isAuthenticated()) {
            return ResponseEntity.status(401).body(
                    ApiResponse.error(ErrorCode.UNAUTHORIZED, "Unauthorized", null)
            );
        }

        ClubRoleResponse data = clubRoleService.createClubRole(clubId, request);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<ApiResponse<ClubRoleResponse>> updateClubRole(
            @PathVariable Long clubId,
            @PathVariable Long roleId,
            @Valid @RequestBody UpdateClubRoleRequest request
    ) throws AppException {
        if (!isAuthenticated()) {
            return ResponseEntity.status(401).body(
                    ApiResponse.error(ErrorCode.UNAUTHORIZED, "Unauthorized", null)
            );
        }

        ClubRoleResponse data = clubRoleService.updateClubRole(clubId, roleId, request);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<ApiResponse<Void>> deleteClubRole(
            @PathVariable Long clubId,
            @PathVariable Long roleId
    ) throws AppException {
        if (!isAuthenticated()) {
            return ResponseEntity.status(401).body(
                    ApiResponse.error(ErrorCode.UNAUTHORIZED, "Unauthorized", null)
            );
        }

        clubRoleService.deleteClubRole(clubId, roleId);

        // trả về success với data = null
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}