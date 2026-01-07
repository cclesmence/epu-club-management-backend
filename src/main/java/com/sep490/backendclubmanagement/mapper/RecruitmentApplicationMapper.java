package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.RecruitmentApplicationData;
import com.sep490.backendclubmanagement.dto.response.RecruitmentApplicationListData;
import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentApplication;
import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentFormAnswer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RecruitmentApplicationMapper {

    @Mapping(source = "recruitment.id", target = "recruitmentId")
    @Mapping(source = "applicant.id", target = "applicantId")
    @Mapping(source = "applicant.fullName", target = "userName")
    @Mapping(source = "applicant.email", target = "userEmail")
    @Mapping(source = "applicant.phoneNumber", target = "userPhone")
    @Mapping(source = "applicant.studentCode", target = "studentId")
    @Mapping(source = "interviewTime", target = "interviewTime")
    @Mapping(source = "interviewAddress", target = "interviewAddress")
    @Mapping(source = "interviewPreparationRequirements", target = "interviewPreparationRequirements")
    @Mapping(source = "recruitment.club.clubName", target = "clubName")
    @Mapping(source = "recruitment.title", target = "recruitmentTitle")
    @Mapping(source = "answers", target = "answers", qualifiedByName = "mapAnswersList")
    @Mapping(target = "teamName", ignore = true)  // Will be set manually in service
    RecruitmentApplicationData toDto(RecruitmentApplication application);

    @Mapping(source = "recruitment.id", target = "recruitmentId")
    @Mapping(source = "applicant.id", target = "applicantId")
    @Mapping(source = "applicant.fullName", target = "userName")
    @Mapping(source = "applicant.email", target = "userEmail")
    @Mapping(source = "applicant.phoneNumber", target = "userPhone")
    @Mapping(source = "applicant.studentCode", target = "studentId")
    @Mapping(source = "applicant.avatarUrl", target = "avatar")
    @Mapping(source = "interviewTime", target = "interviewTime")
    @Mapping(source = "interviewAddress", target = "interviewAddress")
    @Mapping(source = "interviewPreparationRequirements", target = "interviewPreparationRequirements")
    @Mapping(target = "teamName", ignore = true)  // Will be set manually in service
    RecruitmentApplicationListData toListDto(RecruitmentApplication application);

    @Mapping(source = "question.id", target = "questionId")
    @Mapping(source = "question.questionText", target = "questionText")
    RecruitmentApplicationData.ApplicationAnswerData toAnswerDto(RecruitmentFormAnswer answer);

    @Named("mapAnswersList")
    default List<RecruitmentApplicationData.ApplicationAnswerData> mapAnswersList(Set<RecruitmentFormAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return null;
        }
        return answers.stream()
                .map(this::toAnswerDto)
                .collect(Collectors.toList());
    }
}
