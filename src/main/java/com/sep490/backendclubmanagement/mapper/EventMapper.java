package com.sep490.backendclubmanagement.mapper;

import com.sep490.backendclubmanagement.dto.response.EventData;
import com.sep490.backendclubmanagement.entity.event.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface EventMapper {

    // map field club.id → clubId
    @Mapping(source = "club.id", target = "clubId")
    @Mapping(source = "club.clubName", target = "clubName")
    @Mapping(source = "eventType.id", target = "eventTypeId")
    @Mapping(source = "eventType.typeName", target = "eventTypeName")

    // convert startTime → string
    @Mapping(source = "startTime", target = "startTime", qualifiedByName = "toStringTime")
    @Mapping(source = "endTime", target = "endTime", qualifiedByName = "toStringTime")

    // mediaUrls không map trong entity, ta set sau
    EventData toDto(Event event);

    // custom converter cho time
    @Named("toStringTime")
    static String toStringTime(java.time.LocalDateTime time) {
        if (time == null) return null;
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
