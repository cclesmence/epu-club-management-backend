package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.BatchMarkAttendanceRequest;
import com.sep490.backendclubmanagement.dto.request.CreateEventRequest;
import com.sep490.backendclubmanagement.dto.request.EventApprovalRequest;
import com.sep490.backendclubmanagement.dto.request.EventRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateEventRequest;
import com.sep490.backendclubmanagement.dto.response.*;
import com.sep490.backendclubmanagement.exception.ForbiddenException;
import com.sep490.backendclubmanagement.service.event.EventManagementService;
import com.sep490.backendclubmanagement.service.event.EventService;
import com.sep490.backendclubmanagement.service.role.RoleService;
import com.sep490.backendclubmanagement.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventManagementService eventManagementService;
    private final RoleService roleService;

    @PostMapping("/get-all-by-filter")
    public ApiResponse<EventResponse> getAllEventsByFilter(@RequestBody EventRequest request){
         return ApiResponse.success(eventService.getAllEventsByFilter(request));
    }
    
    @GetMapping("/get-all-event-types")
    public ApiResponse<List<EventTypesDto>> getAllEventTypes(){
        return ApiResponse.success(eventService.getAllEventTypes());
    }

    @GetMapping("/{id}")
    public ApiResponse<EventData> getEventById(@PathVariable Long id){
         return ApiResponse.success(eventService.getEventById(id));
    }

    @GetMapping("/get-all-club")
    public ApiResponse<List<ClubDto>> getAllClubs(){
        return ApiResponse.success(eventService.getAllClubs());
    }

    @GetMapping("/club/{clubId}")
    public ApiResponse<List<EventData>> getEventsByClubId(@PathVariable Long clubId,
                                                          @RequestParam(value = "startTime", required = false) String startTime,
                                                          @RequestParam(value = "endTime", required = false) String endTime) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(eventService.getEventsByClubId(clubId, userId, startTime, endTime));
    }


    @GetMapping("/staff/all")
    public ApiResponse<List<EventData>> getStaffAllEvents(@RequestParam(value = "startTime", required = false) String startTime,
                                                          @RequestParam(value = "endTime", required = false) String endTime) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(userId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        return ApiResponse.success(eventService.getStaffAllEvents(startTime, endTime));
    }


    @GetMapping("/staff/club/{clubId}")
    public ApiResponse<List<EventData>> getStaffEventsByClubId(@PathVariable Long clubId,
                                                               @RequestParam(value = "startTime", required = false) String startTime,
                                                               @RequestParam(value = "endTime", required = false) String endTime) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (!roleService.isStaff(userId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền truy cập");
        }
        return ApiResponse.success(eventService.getStaffEventsByClubId(clubId, startTime, endTime));
    }
    

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EventData> createEvent(@Valid @ModelAttribute CreateEventRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(eventManagementService.createEvent(request, userId));
    }
    

    @PostMapping("/approve/club")
    public ApiResponse<Void> approveByClub(@Valid @RequestBody EventApprovalRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        eventManagementService.approveEventByClub(request, userId);
        return ApiResponse.success();
    }
    

    @PostMapping("/approve/university")
    public ApiResponse<Void> approveByStaff(@Valid @RequestBody EventApprovalRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        eventManagementService.approveEventByStaff(request, userId);
        return ApiResponse.success();
    }
    
    /**
     * Lấy danh sách request chờ duyệt
     */
    @GetMapping("/pending-requests")
    public ApiResponse<List<PendingRequestDto>> getPendingRequests(
            @RequestParam(value = "clubId", required = false) Long clubId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(eventManagementService.getPendingRequests(userId, clubId));
    }

    /**
     * Lấy các event và trạng thái request chờ duyệt mà user hiện tại tạo (theo club)
     */
    @GetMapping("/my-draft-events")
    public ApiResponse<List<MyDraftEventDto>> getMyDraftEvents(
            @RequestParam(value = "clubId", required = false) Long clubId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(eventManagementService.getMyDraftEvents(userId, clubId));
    }


    @PostMapping("/{eventId}/cancel")
    public ApiResponse<Void> cancelClubEventByStaff(@PathVariable Long eventId,
                                                    @RequestParam(value = "reason", required = false) String reason) {
        Long userId = SecurityUtils.getCurrentUserId();
        eventManagementService.cancelClubEventByStaff(eventId, userId, reason);
        return ApiResponse.success();
    }


    @PostMapping("/{eventId}/restore")
    public ApiResponse<Void> restoreCancelledEventByStaff(@PathVariable Long eventId) {
        Long userId = SecurityUtils.getCurrentUserId();
        eventManagementService.restoreCancelledEventByStaff(eventId, userId);
        return ApiResponse.success();
    }

    @PostMapping("/{eventId}/publish")
    public ApiResponse<EventData> publishEventByStaff(@PathVariable Long eventId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(eventManagementService.publishEventByStaff(eventId, userId));
    }


    @GetMapping("/staff/cancelled")
    public ApiResponse<List<EventData>> getStaffCancelledEvents(@RequestParam(value = "clubId", required = false) Long clubId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(eventManagementService.getStaffCancelledEvents(userId, clubId));
    }


    @DeleteMapping("/{eventId}/staff-hard-delete")
    public ApiResponse<Void> staffHardDeleteCancelledEvent(@PathVariable Long eventId) {
        Long userId = SecurityUtils.getCurrentUserId();
        eventManagementService.staffHardDeleteCancelledEvent(eventId, userId);
        return ApiResponse.success();
    }


    @PutMapping(value = "/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EventData> updateMyDraftEvent(@PathVariable Long eventId,
                                                     @ModelAttribute UpdateEventRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(eventManagementService.updateMyDraftEvent(eventId, request, userId));
    }

    /**
     * Xóa sự kiện nháp do user hiện tại tạo (theo role/status)
     */
    @DeleteMapping("/{eventId}")
    public ApiResponse<Void> deleteMyDraftEvent(@PathVariable Long eventId) {
        Long userId = SecurityUtils.getCurrentUserId();
        eventManagementService.deleteMyDraftEvent(eventId, userId);
        return ApiResponse.success();
    }


    @PostMapping("/{eventId}/register")
    public ApiResponse<Void> registerForEvent(@PathVariable Long eventId) {
        Long userId = SecurityUtils.getCurrentUserId();
        eventService.registerForEvent(eventId, userId);
        return ApiResponse.success();
    }


    @DeleteMapping("/{eventId}/register")
    public ApiResponse<Void> cancelEventRegistration(@PathVariable Long eventId) {
        Long userId = SecurityUtils.getCurrentUserId();
        eventService.cancelEventRegistration(eventId, userId);
        return ApiResponse.success();
    }


    @GetMapping("/{eventId}/registration-status")
    public ApiResponse<Boolean> getRegistrationStatus(@PathVariable Long eventId) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean isRegistered = eventService.isUserRegisteredForEvent(eventId, userId);
        return ApiResponse.success(isRegistered);
    }


    @GetMapping("/{eventId}/registration-count")
    public ApiResponse<Long> getEventRegistrationCount(@PathVariable Long eventId) {
        Long count = eventService.getEventRegistrationCount(eventId);
        return ApiResponse.success(count);
    }


    @GetMapping("/{eventId}/registrations")
    public ApiResponse<List<EventRegistrationDto>> getEventRegistrations(@PathVariable Long eventId,
                                                                         @RequestParam(value = "keyword", required = false) String keyword) {
        Long userId = SecurityUtils.getCurrentUserId();

        EventData event = eventService.getEventById(eventId);
        Long clubId = event != null ? event.getClubId() : null;
        if (clubId == null) {
            throw new ForbiddenException("Event không thuộc về club nào");
        }

        if (!roleService.isClubPresident(userId, clubId)
                && !roleService.isClubOfficer(userId, clubId)
                && !roleService.isClubTreasurer(userId, clubId)) {
            throw new ForbiddenException("Chỉ ban cán sự của CLB này mới có quyền xem danh sách đăng ký");
        }
        
        return ApiResponse.success(eventService.getEventRegistrations(eventId, keyword));
    }


    @PostMapping("/batch-mark-attendance")
    public ApiResponse<Void> batchMarkAttendance(@Valid @RequestBody BatchMarkAttendanceRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        EventData event = eventService.getEventById(request.getEventId());
        Long clubId = event != null ? event.getClubId() : null;
        if (clubId == null) {
            throw new ForbiddenException("Event không thuộc về club nào");
        }

        if (!roleService.isClubPresident(userId, clubId)
                && !roleService.isClubOfficer(userId, clubId)
                && !roleService.isClubTreasurer(userId, clubId)) {
            throw new ForbiddenException("Chỉ ban cán sự của CLB này mới có quyền điểm danh");
        }
        
        eventService.batchMarkAttendance(request.getEventId(), request.getAttendances());
        
        return ApiResponse.success();
    }

    /**
     * Lấy danh sách events chưa được yêu cầu nộp báo cáo
     * Trả về: id event, tên event, id club, tên club
     */
    @GetMapping("/without-report-requirement")
    public ApiResponse<List<EventWithoutReportRequirementDto>> getEventsWithoutReportRequirement() {
        return ApiResponse.success(eventService.getEventsWithoutReportRequirement());
    }

    /**
     * Lấy danh sách events đã được publish của một câu lạc bộ với phân trang và tìm kiếm
     */
    @GetMapping("/clubs/{clubId}/published")
    public ApiResponse<PagedResponse<EventData>> getPublishedEventsByClubId(
            @PathVariable Long clubId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startTime,desc") String sort) {
        Pageable pageable = createPageable(page, size, sort);
        return ApiResponse.success(eventService.getPublishedEventsByClubId(clubId, keyword, pageable));
    }

    /**
     * Export danh sách điểm danh sự kiện ra file Excel
     * GET /api/events/{eventId}/attendance/export-excel
     */
    @GetMapping("/{eventId}/attendance/export-excel")
    public ResponseEntity<byte[]> exportAttendanceExcel(@PathVariable Long eventId) throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();

        EventData event = eventService.getEventById(eventId);
        Long clubId = event != null ? event.getClubId() : null;
        if (clubId == null) {
            throw new ForbiddenException("Event không thuộc về club nào");
        }

        // Chỉ ban cán sự của CLB mới có quyền xuất Excel
        if (!roleService.isClubPresident(userId, clubId)
                && !roleService.isClubOfficer(userId, clubId)
                && !roleService.isClubTreasurer(userId, clubId)) {
            throw new ForbiddenException("Chỉ ban cán sự của CLB này mới có quyền xuất Excel điểm danh");
        }

        java.io.ByteArrayOutputStream excelStream = eventService.exportAttendanceToExcel(eventId);
        byte[] excelBytes = excelStream.toByteArray();

        // Tạo tên file với tên sự kiện
        String eventName = event.getTitle() != null ? event.getTitle() : "Event";
        // Loại bỏ ký tự đặc biệt trong tên file
       // String safeFileName = eventName.replaceAll("[^a-zA-Z0-9\\s]", "_").replaceAll("\\s+", "_");
        String fileName = eventName + "_DiemDanh.xlsx";

        HttpHeaders headers = new HttpHeaders();
        // Set content type cho Excel file
        headers.setContentType(new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        // Encode filename với UTF-8 để hỗ trợ tiếng Việt trong tên file
        String encodedFileName = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        headers.add("Content-Disposition", "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName);
        headers.setContentLength(excelBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }

    /**
     * Helper method to create Pageable from sort string
     */
    private Pageable createPageable(int page, int size, String sortStr) {
        String[] sortParams = sortStr.split(",");
        String property = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}
