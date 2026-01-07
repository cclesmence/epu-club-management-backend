package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
    List<QuestionOption> findByQuestion_IdOrderByOptionOrderAsc(Long questionId);

    /**
     * Batch load all options for multiple questions to avoid N+1 queries
     * @param questionIds List of question IDs
     * @return List of options ordered by question ID and option order
     */
    @Query("SELECT qo FROM QuestionOption qo WHERE qo.question.id IN :questionIds ORDER BY qo.question.id ASC, qo.optionOrder ASC")
    List<QuestionOption> findByQuestionIdInOrderByQuestionIdAscOptionOrderAsc(@Param("questionIds") List<Long> questionIds);
}