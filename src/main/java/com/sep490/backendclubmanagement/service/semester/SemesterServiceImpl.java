package com.sep490.backendclubmanagement.service.semester;

import com.sep490.backendclubmanagement.dto.response.SemesterResponse;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.Semester;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.repository.ClubRepository;
import com.sep490.backendclubmanagement.repository.SemesterRepository;
import com.sep490.backendclubmanagement.mapper.SemesterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SemesterServiceImpl implements SemesterService {

    private final SemesterRepository semesterRepository;
    private final ClubRepository clubRepository;
    private final SemesterMapper semesterMapper;

    @Override
    public List<SemesterResponse> getSemestersFromClubEstablishment(Long clubId) throws AppException {
        // Get club to find establishment date
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new AppException(ErrorCode.CLUB_NOT_FOUND));

        LocalDate now = LocalDate.now();  // October 22, 2025 theo context
        LocalDate clubEstablishedAt = club.getCreatedAt().toLocalDate();

        // Get semesters overlapping [clubEstablishedAt, now] AND có ít nhất 1 role_membership cho club
        List<Semester> semesters = semesterRepository.findActiveSemestersByClubIdAndDateRange(
                clubId, now, clubEstablishedAt);

        return semesterMapper.toDtos(semesters);
    }
    @Override
    public Long getCurrentSemesterId() throws AppException {
        Semester sem = semesterRepository.findCurrentSemester()
                .orElseThrow(() -> new AppException(ErrorCode.SEMESTER_NOT_FOUND));
        return sem.getId();
    }
    @Override
    public Semester getCurrentSemester() throws AppException {
        return semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }
}
