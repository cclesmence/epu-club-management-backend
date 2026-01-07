package com.sep490.backendclubmanagement.service.admin.impl;

import com.sep490.backendclubmanagement.dto.request.AdminDepartmentUpdateRequest;
import com.sep490.backendclubmanagement.dto.response.AdminDepartmentResponse;
import com.sep490.backendclubmanagement.entity.AdminDepartment;
import com.sep490.backendclubmanagement.entity.Campus;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.mapper.AdminDepartmentMapper;
import com.sep490.backendclubmanagement.repository.AdminDepartmentRepository;
import com.sep490.backendclubmanagement.repository.CampusRepository;
import com.sep490.backendclubmanagement.service.admin.AdminDepartmentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDepartmentServiceImpl implements AdminDepartmentService {

    private final AdminDepartmentRepository adminDepartmentRepository;
    private final CampusRepository campusRepository;
    private final AdminDepartmentMapper adminDepartmentMapper;

    @Override
    @Transactional()
    public AdminDepartmentResponse getDepartmentById(Long id) throws AppException{
        AdminDepartment department = adminDepartmentRepository.findByIdWithCampus(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        return adminDepartmentMapper.toDTO(department);
    }

    @Override
    @Transactional()
    public List<AdminDepartmentResponse> getDepartmentsByCampus(Long campusId) {
        // nếu bạn cần filter theo campus, có thể viết query riêng trong repository
        List<AdminDepartment> departments = adminDepartmentRepository.findAll()
                .stream()
                .filter(d -> d.getCampus() != null
                        && d.getCampus().getId().equals(campusId))
                .toList();

        return adminDepartmentMapper.toDTOs(departments);
    }

    @Override
    @Transactional
    public AdminDepartmentResponse updateDepartment(Long id, AdminDepartmentUpdateRequest request) throws AppException {
        AdminDepartment department = adminDepartmentRepository.findByIdWithCampus(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // MapStruct cập nhật các field same-name từ request sang entity
        adminDepartmentMapper.updateEntityFromRequest(request, department);

        // Xử lý riêng campusId
        if (request.getCampusId() != null) {
            Campus campus = campusRepository.findById(request.getCampusId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
            department.setCampus(campus);
        }

        AdminDepartment saved = adminDepartmentRepository.save(department);
        return adminDepartmentMapper.toDTO(saved);
    }
}
