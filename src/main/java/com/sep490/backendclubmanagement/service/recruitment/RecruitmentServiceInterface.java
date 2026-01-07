package com.sep490.backendclubmanagement.service.recruitment;

import com.sep490.backendclubmanagement.dto.request.*;
import com.sep490.backendclubmanagement.dto.response.RecruitmentApplicationData;
import com.sep490.backendclubmanagement.dto.response.RecruitmentApplicationListData;
import com.sep490.backendclubmanagement.dto.response.PagedResponse;
import com.sep490.backendclubmanagement.dto.response.RecruitmentData;
import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentApplicationStatus;
import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentStatus;
import com.sep490.backendclubmanagement.exception.AppException;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface RecruitmentServiceInterface {

    // Recruitment CRUD
    PagedResponse<RecruitmentData> listRecruitments(Long userId, Long clubId, RecruitmentStatus status, String keyword,Pageable pageable) throws AppException;
    PagedResponse<RecruitmentData> listRecruitmentsForGuest(Long clubId, RecruitmentStatus status, Pageable pageable);
    RecruitmentData getRecruitment(Long id) throws AppException;
    RecruitmentData createRecruitment(Long userId, Long clubId, RecruitmentCreateRequest request) throws AppException;
    RecruitmentData updateRecruitment(Long userId, Long id, RecruitmentUpdateRequest request) throws AppException;
    void changeRecruitmentStatus(Long userId, Long id, RecruitmentStatus status) throws AppException;

    // Application management
    PagedResponse<RecruitmentApplicationListData> listApplications(Long userId, Long recruitmentId, RecruitmentApplicationStatus status,String keyword, Pageable pageable) throws AppException;
    PagedResponse<RecruitmentApplicationListData> listMyApplications(Long applicantId, RecruitmentApplicationStatus status, String keyword, Pageable pageable);
    RecruitmentApplicationData submitApplication(Long applicantId, ApplicationSubmitRequest request, MultipartFile file) throws AppException;
    RecruitmentApplicationData getApplication(Long userId, Long applicationId) throws AppException;
    RecruitmentApplicationData getMyApplication(Long applicantId, Long applicationId) throws AppException;
    RecruitmentApplicationData reviewApplication(Long userId, ApplicationReviewRequest request) throws AppException;
    RecruitmentApplicationData updateInterviewSchedule(Long userId, InterviewUpdateRequest request) throws AppException;
    com.sep490.backendclubmanagement.dto.response.ApplicationStatusCheckData checkApplicationStatus(Long userId, Long recruitmentId) throws AppException;
}