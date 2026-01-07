package com.sep490.backendclubmanagement.service.event;

import com.sep490.backendclubmanagement.dto.request.BatchMarkAttendanceRequest;
import com.sep490.backendclubmanagement.dto.request.EventRequest;
import com.sep490.backendclubmanagement.dto.response.ClubDto;
import com.sep490.backendclubmanagement.dto.response.EventData;
import com.sep490.backendclubmanagement.dto.response.EventRegistrationDto;
import com.sep490.backendclubmanagement.dto.response.EventResponse;
import com.sep490.backendclubmanagement.dto.response.EventTypesDto;
import com.sep490.backendclubmanagement.entity.AttendanceStatus;
import com.sep490.backendclubmanagement.dto.response.EventWithoutReportRequirementDto;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.event.Event;
import com.sep490.backendclubmanagement.entity.event.EventAttendance;
import com.sep490.backendclubmanagement.entity.event.EventType;
import com.sep490.backendclubmanagement.entity.User;
import com.sep490.backendclubmanagement.exception.NotFoundException;
import com.sep490.backendclubmanagement.mapper.EventMapper;
import com.sep490.backendclubmanagement.repository.ClubMemberShipRepository;
import com.sep490.backendclubmanagement.repository.EventAttendanceRepository;
import com.sep490.backendclubmanagement.repository.EventMediaRepository;
import com.sep490.backendclubmanagement.entity.event.EventMedia;
import com.sep490.backendclubmanagement.repository.EventRepository;
import com.sep490.backendclubmanagement.repository.UserRepository;
import com.sep490.backendclubmanagement.shared.ModelMapperUtils;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Builder
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final EventMediaRepository eventMediaRepository;
    private final ClubMemberShipRepository clubMemberShipRepository;
    private final EventAttendanceRepository eventAttendanceRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final MessageSource messageSource;

    public EventResponse getAllEventsByFilter(EventRequest request) {
        final List<String> keywords = (request.getKeyword() != null && !request.getKeyword().isBlank())
                ? Arrays.stream(request.getKeyword().split(","))
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .toList()
                : List.of();

        Page<Event> page = this.eventRepository.getAllByFilter(request, request.getPageable());
        List<Event> events = page.getContent();


        if (!keywords.isEmpty()) {
            events = events.stream()
                    .filter(event -> {
                        String title = normalizeVietnamese(event.getTitle() != null ? event.getTitle() : "");
                        String desc = normalizeVietnamese(event.getDescription() != null ? event.getDescription() : "");
                        String loc = normalizeVietnamese(event.getLocation() != null ? event.getLocation() : "");

                        // Ít nhất một keyword khớp (normalize keyword trước khi so sánh)
                        return keywords.stream().anyMatch(kw -> {
                            String normalizedKw = normalizeVietnamese(kw);
                            return title.contains(normalizedKw) ||
                                    desc.contains(normalizedKw) ||
                                    loc.contains(normalizedKw);
                        });
                    })
                    .toList();
        }


        List<EventData> list = events.stream()
                .map(event -> {
                    EventData dto = eventMapper.toDto(event);
                    dto.setClubId(event.getClub() != null ? event.getClub().getId() : null);
                    return dto;
                })
                .toList();
        setMediaUrlsAndTypesBatch(list, events.stream().map(Event::getId).toList());


        return EventResponse.builder()
                .total(page.getTotalElements())
                .count(list.size())
                .data(list)
                .build();
    }

    public List<EventTypesDto> getAllEventTypes() {
        List<EventType> eventTypes = eventRepository.findAllEventTypes();
        return ModelMapperUtils.mapList(eventTypes, EventTypesDto.class);
    }

    private void setMediaUrlsAndTypes(EventData dto, Long eventId) {
        List<EventMedia> mediaList = eventMediaRepository.findByEventIdOrderByDisplayOrder(eventId);
        dto.setMediaUrls(mediaList.stream().map(EventMedia::getMediaUrl).toList());
        dto.setMediaTypes(mediaList.stream().map(m -> m.getMediaType() != null ? m.getMediaType().name() : "IMAGE").toList());
        dto.setMediaIds(mediaList.stream().map(EventMedia::getId).toList());
    }

    private void setMediaUrlsAndTypesBatch(List<EventData> dtos, List<Long> eventIds) {
        if (dtos == null || dtos.isEmpty() || eventIds == null || eventIds.isEmpty()) return;
        List<EventMedia> allMedia = eventMediaRepository.findByEventIdInOrderByDisplayOrder(eventIds);
        Map<Long, List<EventMedia>> byEventId = allMedia.stream()
                .collect(Collectors.groupingBy(em -> em.getEvent().getId(), LinkedHashMap::new, Collectors.toList()));
        Map<Long, EventData> dtoById = dtos.stream().collect(Collectors.toMap(EventData::getId, d -> d));
        for (Map.Entry<Long, List<EventMedia>> entry : byEventId.entrySet()) {
            EventData dto = dtoById.get(entry.getKey());
            if (dto == null) continue;
            List<EventMedia> mediaList = entry.getValue();
            dto.setMediaUrls(mediaList.stream().map(EventMedia::getMediaUrl).toList());
            dto.setMediaTypes(mediaList.stream().map(m -> m.getMediaType() != null ? m.getMediaType().name() : "IMAGE").toList());
            dto.setMediaIds(mediaList.stream().map(EventMedia::getId).toList());
        }
    }

    public EventData getEventById(Long id) {
        Optional<Event> event = eventRepository.findById(id);
        if(event.isEmpty()){
            throw new NotFoundException("Event not found");
        }
        EventData dto = eventMapper.toDto(event.get());
        setMediaUrlsAndTypes(dto, event.get().getId());
        return dto;
    }

    public List<ClubDto> getAllClubs() {
        List<Club> clubs = eventRepository.findAllClubs();
        return ModelMapperUtils.mapList(clubs, ClubDto.class);
    }

    public List<EventData> getEventsByClubId(Long clubId, Long userId, String startTime, String endTime) {
        if (!clubMemberShipRepository.existsByClubIdAndUserIdAndStatusActive(clubId, userId)) {
            throw new NotFoundException("You are not a member of this club or your membership is not active");
        }
        LocalDateTime start = parseIsoDateTimeNullable(startTime);
        LocalDateTime end = parseIsoDateTimeNullable(endTime);
        List<Event> events = (start == null || end == null)
                ? eventRepository.findByClubIdAndIsDraftFalse(clubId)
                : eventRepository.findByClubIdAndIsDraftFalseInRange(clubId, start, end);
        List<EventData> list = events.stream()
                .map(event -> {
                    EventData dto = eventMapper.toDto(event);
                    dto.setClubId(event.getClub() != null ? event.getClub().getId() : null);
                    return dto;
                })
                .toList();
        setMediaUrlsAndTypesBatch(list, events.stream().map(Event::getId).toList());
        return list;
    }


    public List<EventData> getStaffAllEvents(String startTime, String endTime) {
        LocalDateTime start = parseIsoDateTimeNullable(startTime);
        LocalDateTime end = parseIsoDateTimeNullable(endTime);
        List<Event> events = (start == null || end == null)
                ? eventRepository.findStaffAllEventsExcludingMeeting()
                : eventRepository.findStaffAllEventsExcludingMeetingInRange(start, end);
        List<EventData> list = events.stream()
                .map(event -> {
                    EventData dto = eventMapper.toDto(event);
                    dto.setClubId(event.getClub() != null ? event.getClub().getId() : null);
                    return dto;
                })
                .toList();
        setMediaUrlsAndTypesBatch(list, events.stream().map(Event::getId).toList());
        return list;
    }


    public List<EventData> getStaffEventsByClubId(Long clubId, String startTime, String endTime) {
        LocalDateTime start = parseIsoDateTimeNullable(startTime);
        LocalDateTime end = parseIsoDateTimeNullable(endTime);
        List<Event> events = (start == null || end == null)
                ? eventRepository.findStaffEventsByClubIdExcludingMeeting(clubId)
                : eventRepository.findStaffEventsByClubIdExcludingMeetingInRange(clubId, start, end);
        List<EventData> list = events.stream()
                .map(event -> {
                    EventData dto = eventMapper.toDto(event);
                    dto.setClubId(event.getClub() != null ? event.getClub().getId() : null);
                    return dto;
                })
                .toList();
        setMediaUrlsAndTypesBatch(list, events.stream().map(Event::getId).toList());
        return list;
    }

    private LocalDateTime parseIsoDateTimeNullable(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            // Support ISO strings with 'Z' or timezone offset
            java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(value);
            return odt.toLocalDateTime();
        } catch (java.time.format.DateTimeParseException ex) {
            // Fallback to LocalDateTime without zone if provided
            return LocalDateTime.parse(value);
        }
    }


    private String normalizeVietnamese(String text) {
        if (text == null || text.isBlank()) return "";
        String normalized = text.replace("đ", "d").replace("Đ", "d");
        normalized = java.text.Normalizer.normalize(normalized, java.text.Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase();
    }


    public void registerForEvent(Long eventId, Long userId) {
        // Kiểm tra event tồn tại
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        
        // Kiểm tra event đã được publish chưa
        if (event.getIsDraft() != null && event.getIsDraft()) {
            throw new RuntimeException("Cannot register for draft event");
        }
        
        // Cho phép MEETING đăng ký trong khi đang diễn ra; các loại khác chỉ trước khi bắt đầu
        LocalDateTime now = LocalDateTime.now();
        boolean isMeeting = event.getEventType() != null &&
                event.getEventType().getTypeName() != null &&
                "MEETING".equalsIgnoreCase(event.getEventType().getTypeName());
        if (event.getStartTime() != null) {
            boolean hasStarted = event.getStartTime().isBefore(now);
            if (hasStarted && !isMeeting) {
                throw new RuntimeException("Cannot register for event that has already started");
            }
        }
        if (event.getEndTime() != null && event.getEndTime().isBefore(now)) {
            throw new RuntimeException("Cannot register for event that has already ended");
        }

        // Kiểm tra user tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        
        // Kiểm tra đã đăng ký chưa
        if (eventAttendanceRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new RuntimeException("You have already registered for this event");
        }
        
        // Tạo event attendance với status REGISTERED
        EventAttendance eventAttendance = EventAttendance.builder()
                .event(event)
                .user(user)
                .registrationTime(LocalDateTime.now())
                .attendanceStatus(AttendanceStatus.REGISTERED)
                .build();
        
        eventAttendanceRepository.save(eventAttendance);
    }


    public void cancelEventRegistration(Long eventId, Long userId) {
        // Kiểm tra đã đăng ký chưa
        EventAttendance eventAttendance = eventAttendanceRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("You have not registered for this event"));
        
        // Kiểm tra event đã bắt đầu chưa
        Event event = eventAttendance.getEvent();
        if (event.getStartTime() != null && event.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Cannot cancel registration for event that has already started");
        }
        
        // Xóa đăng ký
        eventAttendanceRepository.delete(eventAttendance);
    }


    public boolean isUserRegisteredForEvent(Long eventId, Long userId) {
        return eventAttendanceRepository.existsByEventIdAndUserId(eventId, userId);
    }


    public List<EventRegistrationDto> getEventRegistrations(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        
        List<EventAttendance> attendances = eventAttendanceRepository.findByEventId(eventId);
        
        return attendances.stream()
                .map(attendance -> {
                    User user = attendance.getUser();
                    return EventRegistrationDto.builder()
                            .id(attendance.getId())
                            .userId(user.getId())
                            .fullName(user.getFullName())
                            .studentCode(user.getStudentCode())
                            .email(user.getEmail())
                            .avatarUrl(user.getAvatarUrl())
                            .registrationTime(attendance.getRegistrationTime())
                            .attendanceStatus(attendance.getAttendanceStatus() != null 
                                    ? attendance.getAttendanceStatus().name() 
                                    : null)
                            .checkInTime(attendance.getCheckInTime())
                            .notes(attendance.getNotes())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<EventRegistrationDto> getEventRegistrations(Long eventId, String keyword) {
        List<EventRegistrationDto> list = getEventRegistrations(eventId);
        if (keyword == null || keyword.isBlank()) return list;
        String kw = keyword.trim().toLowerCase();
        return list.stream()
                .filter(r -> {
                    String name = r.getFullName() != null ? r.getFullName().toLowerCase() : "";
                    String code = r.getStudentCode() != null ? r.getStudentCode().toLowerCase() : "";
                    return name.contains(kw) || code.contains(kw);
                })
                .toList();
    }


    public Long getEventRegistrationCount(Long eventId) {
        return eventAttendanceRepository.countByEventIdAndStatus(eventId);
    }


    @Transactional
    public void batchMarkAttendance(Long eventId, List<BatchMarkAttendanceRequest.AttendanceItem> attendances) {
        // Kiểm tra event tồn tại và thuộc club
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        
        // Kiểm tra event thuộc club nào
        if (event.getClub() == null) {
            throw new NotFoundException("Event không thuộc về club nào");
        }
        
        // Không cho điểm danh nếu sự kiện đã kết thúc quá 1 ngày
        if (event.getEndTime() != null && event.getEndTime().isBefore(LocalDateTime.now().minusDays(1))) {
            throw new RuntimeException("Event ended more than 1 day ago. Attendance can no longer be modified");
        }
        
        if (attendances == null || attendances.isEmpty()) {
            throw new RuntimeException("Danh sách điểm danh không được rỗng");
        }
        
        List<EventAttendance> updatedAttendances = new ArrayList<>();
        
        for (BatchMarkAttendanceRequest.AttendanceItem item : attendances) {
            // Kiểm tra status hợp lệ
            if (item.getAttendanceStatus() != AttendanceStatus.PRESENT && 
                item.getAttendanceStatus() != AttendanceStatus.ABSENT) {
                throw new RuntimeException("Chỉ có thể điểm danh PRESENT hoặc ABSENT cho userId: " + item.getUserId());
            }
            
            // Kiểm tra user đã đăng ký event chưa
            EventAttendance attendance = eventAttendanceRepository.findByEventIdAndUserId(eventId, item.getUserId())
                    .orElseThrow(() -> new NotFoundException("User với ID " + item.getUserId() + " chưa đăng ký sự kiện này"));
            
            // Cập nhật điểm danh
            attendance.setAttendanceStatus(item.getAttendanceStatus());
            attendance.setCheckInTime(item.getAttendanceStatus() == AttendanceStatus.PRESENT ? LocalDateTime.now() : null);
            attendance.setNotes(item.getNotes());
            
            updatedAttendances.add(attendance);
        }
        
        eventAttendanceRepository.saveAll(updatedAttendances);
    }

    /**
     * Lấy danh sách events chưa được yêu cầu nộp báo cáo
     * @return Danh sách events với id, tên event, id và tên club
     */
    public List<EventWithoutReportRequirementDto> getEventsWithoutReportRequirement() {
        return eventRepository.findEventsWithoutReportRequirement();
    }

    /**
     * Lấy danh sách events đã được publish của một câu lạc bộ với phân trang và tìm kiếm
     */
    public com.sep490.backendclubmanagement.dto.response.PagedResponse<EventData> getPublishedEventsByClubId(
            Long clubId, String keyword, Pageable pageable) {
        Page<Event> eventPage = eventRepository.findPublishedEventsByClubId(clubId, keyword, pageable);

        Page<EventData> dataPage = eventPage.map(event -> {
            EventData dto = eventMapper.toDto(event);
            dto.setClubId(event.getClub() != null ? event.getClub().getId() : null);
            return dto;
        });

        // Set media URLs for all events in the page
        List<Long> eventIds = eventPage.getContent().stream().map(Event::getId).collect(Collectors.toList());
        if (!eventIds.isEmpty()) {
            setMediaUrlsAndTypesBatch(dataPage.getContent(), eventIds);
        }

        return com.sep490.backendclubmanagement.dto.response.PagedResponse.of(dataPage);
    }

    /**
     * Export danh sách điểm danh sự kiện ra file Excel
     * @param eventId ID của sự kiện
     * @return ByteArrayOutputStream chứa file Excel
     */
    public ByteArrayOutputStream exportAttendanceToExcel(Long eventId) throws IOException {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        List<EventRegistrationDto> attendances = getEventRegistrations(eventId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Điểm danh");

            // Tạo style cho header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setFontName("Times New Roman"); // Font hỗ trợ tiếng Việt
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Tạo style cho cell
            CellStyle cellStyle = workbook.createCellStyle();
            Font cellFont = workbook.createFont();
            cellFont.setFontName("Times New Roman"); // Font hỗ trợ tiếng Việt
            cellStyle.setFont(cellFont);
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Tạo header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"STT", "Mã sinh viên", "Họ và tên", "Email", "Trạng thái điểm danh", "Thời gian check-in", "Ghi chú"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Tạo style cho date cell
            CellStyle dateCellStyle = workbook.createCellStyle();
            Font dateFont = workbook.createFont();
            dateFont.setFontName("Times New Roman");
            dateCellStyle.setFont(dateFont);
            dateCellStyle.setBorderBottom(BorderStyle.THIN);
            dateCellStyle.setBorderTop(BorderStyle.THIN);
            dateCellStyle.setBorderLeft(BorderStyle.THIN);
            dateCellStyle.setBorderRight(BorderStyle.THIN);
            dateCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            // Format date: dd/MM/yyyy HH:mm
            CreationHelper createHelper = workbook.getCreationHelper();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy HH:mm"));

            // Tạo data rows
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int rowNum = 1;
            for (EventRegistrationDto attendance : attendances) {
                Row row = sheet.createRow(rowNum++);
                
                // STT
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(rowNum - 1);
                cell0.setCellStyle(cellStyle);
                
                // Mã sinh viên
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(attendance.getStudentCode() != null ? attendance.getStudentCode() : "");
                cell1.setCellStyle(cellStyle);
                
                // Họ và tên
                Cell cell2 = row.createCell(2);
                cell2.setCellValue(attendance.getFullName() != null ? attendance.getFullName() : "");
                cell2.setCellStyle(cellStyle);
                
                // Email
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(attendance.getEmail() != null ? attendance.getEmail() : "");
                cell3.setCellStyle(cellStyle);
                
                // Trạng thái điểm danh
                Cell cell4 = row.createCell(4);
                String statusText = "";
                if (attendance.getAttendanceStatus() != null) {
                    switch (attendance.getAttendanceStatus()) {
                        case "PRESENT":
                            statusText = "Có mặt";
                            break;
                        case "ABSENT":
                            statusText = "Vắng mặt";
                            break;
                        case "REGISTERED":
                            statusText = "Chưa điểm danh";
                            break;
                        default:
                            statusText = attendance.getAttendanceStatus();
                    }
                } else {
                    statusText = "Chưa điểm danh";
                }
                cell4.setCellValue(statusText);
                cell4.setCellStyle(cellStyle);
                
                // Thời gian check-in
                Cell cell5 = row.createCell(5);
                if (attendance.getCheckInTime() != null) {
                    // Convert LocalDateTime to Date for Excel
                    java.util.Date checkInDate = java.sql.Timestamp.valueOf(attendance.getCheckInTime());
                    cell5.setCellValue(checkInDate);
                    cell5.setCellStyle(dateCellStyle);
                } else {
                    cell5.setCellValue("");
                    cell5.setCellStyle(cellStyle);
                }
                
                // Ghi chú
                Cell cell6 = row.createCell(6);
                cell6.setCellValue(attendance.getNotes() != null ? attendance.getNotes() : "");
                cell6.setCellStyle(cellStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Thêm padding
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }

            // Set row height cho header
            headerRow.setHeightInPoints(20);

            workbook.write(out);
        }
        
        return out;
    }
}

