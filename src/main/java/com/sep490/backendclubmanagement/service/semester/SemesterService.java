package com.sep490.backendclubmanagement.service.semester;

import com.sep490.backendclubmanagement.dto.response.SemesterResponse;
import com.sep490.backendclubmanagement.entity.Semester;
import com.sep490.backendclubmanagement.exception.AppException;

import java.util.List;

public interface SemesterService {
    List<SemesterResponse> getSemestersFromClubEstablishment(Long clubId) throws AppException;
    Long getCurrentSemesterId() throws AppException;
    Semester getCurrentSemester() throws AppException;
}

