package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentFormQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecruitmentFormQuestionRepository extends JpaRepository<RecruitmentFormQuestion, Long> {
    List<RecruitmentFormQuestion> findByRecruitment_IdOrderByQuestionOrderAsc(Long recruitmentId);
}


