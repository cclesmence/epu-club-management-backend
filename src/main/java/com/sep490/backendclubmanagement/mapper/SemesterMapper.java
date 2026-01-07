package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.SemesterResponse;
import com.sep490.backendclubmanagement.entity.Semester;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface SemesterMapper {
    SemesterResponse toDto(Semester semester);
    List<SemesterResponse> toDtos(List<Semester> semesters);
}









