package com.sep490.backendclubmanagement.service.semester;

import com.sep490.backendclubmanagement.dto.request.CreateSemesterRequest;
import com.sep490.backendclubmanagement.dto.request.SemesterFilterRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateSemesterRequest;
import com.sep490.backendclubmanagement.dto.response.SemesterListResponse;
import com.sep490.backendclubmanagement.dto.response.SemesterSummaryResponse;
import com.sep490.backendclubmanagement.entity.Semester;
import com.sep490.backendclubmanagement.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemesterManagementService {

    private final SemesterRepository semesterRepository;

    public SemesterListResponse getAllSemestersByFilter(SemesterFilterRequest request) {
        Page<Semester> page = semesterRepository.getAllByFilter(request, request.getPageable("id,desc"));
        List<SemesterSummaryResponse> data = page.getContent().stream()
                .map(semester -> SemesterSummaryResponse.builder()
                        .id(semester.getId())
                        .semesterName(semester.getSemesterName())
                        .semesterCode(semester.getSemesterCode())
                        .startDate(semester.getStartDate())
                        .endDate(semester.getEndDate())
                        .isCurrent(semester.getIsCurrent())
                        .build())
                .toList();

        return SemesterListResponse.builder()
                .total(page.getTotalElements())
                .count(data.size())
                .data(data)
                .build();
    }

    @Transactional
    public SemesterSummaryResponse createSemester(CreateSemesterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request không hợp lệ");
        }
        if (request.getSemesterName() == null || request.getSemesterName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên kỳ học không được để trống");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc không được để trống");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        // Kiểm tra semesterName unique
        String semesterName = request.getSemesterName().trim();
        boolean nameExists = semesterRepository.existsBySemesterName(semesterName);
        if (nameExists) {
            throw new IllegalArgumentException("Tên kỳ học đã tồn tại");
        }

        // Kiểm tra semesterCode unique nếu có
        if (request.getSemesterCode() != null && !request.getSemesterCode().trim().isEmpty()) {
            boolean codeExists = semesterRepository.existsBySemesterCode(request.getSemesterCode().trim());
            if (codeExists) {
                throw new IllegalArgumentException("Mã kỳ học đã tồn tại");
            }
        }

        // Nếu set isCurrent = true, set các kỳ khác thành false
        if (Boolean.TRUE.equals(request.getIsCurrent())) {
            List<Semester> currentSemesters = semesterRepository.findAll().stream()
                    .filter(Semester::getIsCurrent)
                    .toList();
            for (Semester s : currentSemesters) {
                s.setIsCurrent(false);
            }
            semesterRepository.saveAll(currentSemesters);
        }

        Semester semester = Semester.builder()
                .semesterName(request.getSemesterName().trim())
                .semesterCode(request.getSemesterCode() != null && !request.getSemesterCode().trim().isEmpty()
                        ? request.getSemesterCode().trim() : null)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isCurrent(Boolean.TRUE.equals(request.getIsCurrent()))
                .build();

        Semester saved = semesterRepository.save(semester);
        return toSemesterSummary(saved);
    }

    @Transactional
    public SemesterSummaryResponse updateSemester(Long semesterId, UpdateSemesterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request không hợp lệ");
        }
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Kỳ học không tồn tại"));

        if (request.getSemesterName() != null) {
            String name = request.getSemesterName().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Tên kỳ học không được để trống");
            }
            // Kiểm tra unique nếu name khác với name hiện tại
            if (!name.equals(semester.getSemesterName())) {
                boolean nameExists = semesterRepository.existsBySemesterNameAndIdNot(name, semesterId);
                if (nameExists) {
                    throw new IllegalArgumentException("Tên kỳ học đã tồn tại");
                }
            }
            semester.setSemesterName(name);
        }

        if (request.getSemesterCode() != null) {
            String code = request.getSemesterCode().trim();
            if (!code.isEmpty()) {
                // Kiểm tra unique nếu code khác với code hiện tại
                if (!code.equals(semester.getSemesterCode())) {
                    boolean exists = semesterRepository.existsBySemesterCode(code);
                    if (exists) {
                        throw new IllegalArgumentException("Mã kỳ học đã tồn tại");
                    }
                }
                semester.setSemesterCode(code);
            } else {
                semester.setSemesterCode(null);
            }
        }

        if (request.getStartDate() != null) {
            semester.setStartDate(request.getStartDate());
        }

        if (request.getEndDate() != null) {
            semester.setEndDate(request.getEndDate());
        }

        // Validate startDate < endDate
        if (semester.getStartDate() != null && semester.getEndDate() != null
                && semester.getStartDate().isAfter(semester.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        // Nếu set isCurrent = true, set các kỳ khác thành false
        if (request.getIsCurrent() != null) {
            if (Boolean.TRUE.equals(request.getIsCurrent())) {
                List<Semester> currentSemesters = semesterRepository.findAll().stream()
                        .filter(s -> s.getIsCurrent() && !s.getId().equals(semesterId))
                        .toList();
                for (Semester s : currentSemesters) {
                    s.setIsCurrent(false);
                }
                semesterRepository.saveAll(currentSemesters);
            }
            semester.setIsCurrent(Boolean.TRUE.equals(request.getIsCurrent()));
        }

        Semester saved = semesterRepository.save(semester);
        return toSemesterSummary(saved);
    }

    @Transactional
    public void deleteSemester(Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Kỳ học không tồn tại"));
        semesterRepository.delete(semester);
    }

    private SemesterSummaryResponse toSemesterSummary(Semester semester) {
        return SemesterSummaryResponse.builder()
                .id(semester.getId())
                .semesterName(semester.getSemesterName())
                .semesterCode(semester.getSemesterCode())
                .startDate(semester.getStartDate())
                .endDate(semester.getEndDate())
                .isCurrent(semester.getIsCurrent())
                .build();
    }
}

