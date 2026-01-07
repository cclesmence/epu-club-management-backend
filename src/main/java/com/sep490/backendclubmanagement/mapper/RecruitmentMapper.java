package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.request.RecruitmentCreateRequest;
import com.sep490.backendclubmanagement.dto.request.RecruitmentUpdateRequest;
import com.sep490.backendclubmanagement.dto.response.RecruitmentData;
import com.sep490.backendclubmanagement.dto.response.RecruitmentQuestionData;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.recruitment.Recruitment;
import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentApplicationStatus;
import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentFormQuestion;
import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RecruitmentMapper {

    // Override toDto to manually map questions
    default RecruitmentData toDto(Recruitment recruitment) {
        if (recruitment == null) {
            return null;
        }
        
        // Calculate application statistics
        int totalApplications = 0;
        int acceptedApplications = 0;
        if (recruitment.getApplications() != null) {
            totalApplications = recruitment.getApplications().size();
            acceptedApplications = (int) recruitment.getApplications().stream()
                    .filter(app -> app.getStatus() == RecruitmentApplicationStatus.ACCEPTED)
                    .count();
        }
        
        RecruitmentData.RecruitmentDataBuilder builder = RecruitmentData.builder()
                .id(recruitment.getId())
                .title(recruitment.getTitle())
                .description(recruitment.getDescription())
                .endDate(recruitment.getEndDate())
                .status(recruitment.getStatus())
                .requirements(recruitment.getRequirements())
                .clubId(recruitment.getClub() != null ? recruitment.getClub().getId() : null)
                .totalApplications(totalApplications)
                .acceptedApplications(acceptedApplications)
                .createdAt(recruitment.getCreatedAt())
                .updatedAt(recruitment.getUpdatedAt());
        
        // Map questions - sort by questionOrder to maintain order
        if (recruitment.getFormQuestions() != null && !recruitment.getFormQuestions().isEmpty()) {
            List<RecruitmentQuestionData> questionData = recruitment.getFormQuestions().stream()
                    .sorted((q1, q2) -> {
                        if (q1.getQuestionOrder() == null) return 1;
                        if (q2.getQuestionOrder() == null) return -1;
                        return q1.getQuestionOrder().compareTo(q2.getQuestionOrder());
                    })
                    .map(q -> RecruitmentQuestionData.builder()
                            .id(q.getId())
                            .questionText(q.getQuestionText())
                            .questionType(q.getQuestionType())
                            .questionOrder(q.getQuestionOrder())
                            .isRequired(q.getIsRequired())
                            .options(mapOptionsInternal(q.getOptions()))
                            .createdAt(q.getCreatedAt())
                            .updatedAt(q.getUpdatedAt())
                            .build())
                    .collect(Collectors.toList());
            builder.questions(questionData);
        }
        
        // Map teamOptions - extract team information
        if (recruitment.getTeamOptions() != null && !recruitment.getTeamOptions().isEmpty()) {
            List<com.sep490.backendclubmanagement.dto.response.TeamOptionData> teamOptions = recruitment.getTeamOptions().stream()
                    .map(teamOption -> com.sep490.backendclubmanagement.dto.response.TeamOptionData.builder()
                            .id(teamOption.getTeam().getId())
                            .teamName(teamOption.getTeam().getTeamName())
                            .description(teamOption.getTeam().getDescription())
                            .build())
                    .collect(Collectors.toList());
            builder.teamOptions(teamOptions);
        }
        
        return builder.build();
    }

    // Map question options to string list
    @Mapping(target = "options", ignore = true)
    RecruitmentQuestionData toQuestionDto(RecruitmentFormQuestion question);

    // Convert RecruitmentCreateRequest to entity (manual mapping for complex logic)
    default Recruitment toEntity(RecruitmentCreateRequest request, Long clubId) {
        return Recruitment.builder()
                .title(request.title)
                .description(request.description)
                .endDate(request.endDate)
                .requirements(request.requirements)
                .status(request.status != null ? request.status : RecruitmentStatus.DRAFT)
                .club(Club.builder().id(clubId).build())
                .build();
    }

    // Update entity from request
    default void updateEntity(Recruitment entity, RecruitmentUpdateRequest request) {
        entity.setTitle(request.title);
        entity.setDescription(request.description);
        entity.setEndDate(request.endDate);
        entity.setRequirements(request.requirements);
        if (request.status != null) {
            entity.setStatus(request.status);
        }
    }


    // Internal helper method - sort options by optionOrder
    default List<String> mapOptionsInternal(Set<QuestionOption> options) {
        if (options == null) return List.of();
        return options.stream()
                .sorted((o1, o2) -> {
                    if (o1.getOptionOrder() == null) return 1;
                    if (o2.getOptionOrder() == null) return -1;
                    return o1.getOptionOrder().compareTo(o2.getOptionOrder());
                })
                .map(QuestionOption::getOptionText)
                .collect(Collectors.toList());
    }

    // Minimal DTO for listing - without questions and teamOptions
    default RecruitmentData toDtoForList(Recruitment recruitment) {
        if (recruitment == null) {
            return null;
        }

        // Calculate application statistics
        int totalApplications = 0;
        int acceptedApplications = 0;
        if (recruitment.getApplications() != null) {
            totalApplications = recruitment.getApplications().size();
            acceptedApplications = (int) recruitment.getApplications().stream()
                    .filter(app -> app.getStatus() == RecruitmentApplicationStatus.ACCEPTED)
                    .count();
        }

        return RecruitmentData.builder()
                .id(recruitment.getId())
                .title(recruitment.getTitle())
                .description(recruitment.getDescription())
                .endDate(recruitment.getEndDate())
                .status(recruitment.getStatus())
                .requirements(recruitment.getRequirements())
                .clubId(recruitment.getClub() != null ? recruitment.getClub().getId() : null)
                .totalApplications(totalApplications)
                .acceptedApplications(acceptedApplications)
                .createdAt(recruitment.getCreatedAt())
                .updatedAt(recruitment.getUpdatedAt())
                .questions(null)  // Don't map questions for list
                .teamOptions(null)  // Don't map teamOptions for list
                .build();
    }
}
