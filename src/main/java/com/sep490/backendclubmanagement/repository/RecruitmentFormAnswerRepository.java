package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentFormAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecruitmentFormAnswerRepository extends JpaRepository<RecruitmentFormAnswer, Long> {
    @Query("SELECT a FROM RecruitmentFormAnswer a JOIN FETCH a.question WHERE a.application.id = :applicationId")
    List<RecruitmentFormAnswer> findByApplication_Id(@Param("applicationId") Long applicationId);
}


