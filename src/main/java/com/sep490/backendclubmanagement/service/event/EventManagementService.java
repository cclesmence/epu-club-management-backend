package com.sep490.backendclubmanagement.service.event;

import com.sep490.backendclubmanagement.dto.request.CreateEventRequest;
import com.sep490.backendclubmanagement.dto.request.EventApprovalRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateEventRequest;
import com.sep490.backendclubmanagement.dto.response.EventData;
import com.sep490.backendclubmanagement.dto.response.MyDraftEventDto;
import com.sep490.backendclubmanagement.dto.response.PendingRequestDto;
import com.sep490.backendclubmanagement.dto.websocket.EventWebSocketPayload;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShip;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShipStatus;
import com.sep490.backendclubmanagement.entity.event.Event;
import com.sep490.backendclubmanagement.entity.event.EventMedia;
import com.sep490.backendclubmanagement.entity.event.EventType;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ForbiddenException;
import com.sep490.backendclubmanagement.exception.NotFoundException;
import com.sep490.backendclubmanagement.mapper.EventMapper;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.service.role.RoleService;
import com.sep490.backendclubmanagement.service.websocket.WebSocketService;
import com.sep490.backendclubmanagement.service.file.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventManagementService {
    
    private final EventRepository eventRepository;
    private final RequestEventRepository requestEventRepository;
    private final RoleService roleService;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final EventTypeRepository eventTypeRepository;
    private final EventMapper eventMapper;
    private final EventMediaRepository eventMediaRepository;
    private final CloudinaryService cloudinaryService;
    private final WebSocketService webSocketService;
    private final NotificationService notificationService;
    private final ClubMemberShipRepository clubMemberShipRepository;

    private static final DateTimeFormatter MEETING_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");


    @Transactional
    public EventData createEvent(CreateEventRequest request, Long userId) {
        User user = getUserById(userId);
        boolean isStaff = roleService.isStaff(userId);
        EventType eventType = getEventTypeById(request.getEventTypeId());

        if (!roleService.canCreateEvent(userId, request.getClubId())) {
            throw new ForbiddenException("Bạn không có quyền tạo sự kiện cho câu lạc bộ này.");
        }

        // Kiểm tra thời gian bắt đầu phải >= thời gian hiện tại
        if (request.getStartTime() != null && request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ForbiddenException("Thời gian bắt đầu phải lớn hơn hoặc bằng thời gian hiện tại");
        }

        boolean isClubPresident = request.getClubId() != null && roleService.isClubPresident(userId, request.getClubId());
        boolean isClubOfficer = request.getClubId() != null && hasEventOfficerPrivileges(userId, request.getClubId());
        boolean isMeeting = eventType != null && "MEETING".equalsIgnoreCase(eventType.getTypeName());
        
        // Nhân viên phòng IC-PDP không được tạo sự kiện MEETING
        if (isStaff && isMeeting) {
            throw new ForbiddenException("Nhân viên phòng IC-PDP không được tạo sự kiện loại MEETING");
        }
        
        Club club = null;
        if (!isStaff) {
            if (request.getClubId() == null) {
                throw new NotFoundException("Club ID is required for non-staff creators");
            }
            club = getClubById(request.getClubId());
        }

        // Nhân viên phòng IC-PDP tạo event ở trạng thái draft, cần publish sau
        // MEETING vẫn public ngay
        boolean shouldBeDraft = isStaff && !isMeeting;
        
        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .club(club)
                .eventType(eventType)
                .isDraft(shouldBeDraft)
                .build();
        
        Event savedEvent = eventRepository.save(event);
        
        if (request.getMediaFiles() != null && !request.getMediaFiles().isEmpty()) {
            uploadAndSaveEventMedia(savedEvent, request.getMediaFiles());
        }
        
        if (isMeeting) {
            log.info("Event created directly (MEETING)");
            if (club != null) {
                notifyClubMembersAboutMeeting(savedEvent, club, user);
            } else {
                log.warn("MEETING event {} does not belong to a club - skip notification", savedEvent.getId());
            }
            return eventMapper.toDto(savedEvent);
        } else if (isStaff) {
            log.info("Event created as draft (Nhân viên phòng IC-PDP)");
            return eventMapper.toDto(savedEvent);
        } else if (isClubPresident) {
            // CLUB_OFFICER: Gửi lên Nhân viên phòng IC-PDP
            savedEvent.setIsDraft(true);
            eventRepository.save(savedEvent);
            
            RequestEvent requestEvent = RequestEvent.builder()
                    .requestTitle(request.getTitle())
                    .status(RequestStatus.PENDING_UNIVERSITY)
                    .requestDate(LocalDateTime.now())
                    .description(request.getDescription())
                    .event(savedEvent)
                    .createdBy(user)
                    .build();
            
            requestEventRepository.save(requestEvent);
            requestEventRepository.flush();
            
            // 🔔 WebSocket: Gửi cho tất cả Nhân viên phòng IC-PDP
            try {
                EventWebSocketPayload payload = EventWebSocketPayload.builder()
                        .eventId(savedEvent.getId())
                        .eventTitle(savedEvent.getTitle())
                        .requestEventId(requestEvent.getId())
                        .status(RequestStatus.PENDING_UNIVERSITY)
                        .clubId(club != null ? club.getId() : null)
                        .clubName(club != null ? club.getClubName() : null)
                        .creatorId(user.getId())
                        .creatorName(user.getFullName())
                        .creatorEmail(user.getEmail())
                        .startTime(savedEvent.getStartTime())
                        .endTime(savedEvent.getEndTime())
                        .location(savedEvent.getLocation())
                        .eventTypeName(eventType != null ? eventType.getTypeName() : null)
                        .message(String.format("%s (Chủ nhiệm CLB %s) đã gửi yêu cầu tạo sự kiện: %s",
                                user.getFullName(),
                                club != null ? club.getClubName() : "N/A",
                                savedEvent.getTitle()))
                        .build();
                
                webSocketService.broadcastToSystemRole("STAFF", "EVENT", "REQUEST_SUBMITTED", payload);
                log.info("Sent WebSocket notification to STAFF for event request submission: {}", savedEvent.getId());
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification for event request submission: {}", e.getMessage(), e);
            }
            
            // 🔔 Notification: Gửi cho tất cả Nhân viên phòng IC-PDP
            try {
                List<User> staffUsers = userRepository.findBySystemRole_RoleNameIgnoreCase("STAFF");
                if (!staffUsers.isEmpty()) {
                    String title = "Yêu cầu tạo sự kiện mới";
                    String message = String.format("%s (Chủ nhiệm CLB %s) đã gửi yêu cầu tạo sự kiện \"%s\"",
                            user.getFullName(),
                            club != null ? club.getClubName() : "N/A",
                            savedEvent.getTitle());
                    String actionUrl = "/staff/events";
                    
                    List<Long> staffIds = staffUsers.stream().map(User::getId).toList();
                    notificationService.sendToUsers(
                            staffIds,
                            userId,
                            title,
                            message,
                            NotificationType.EVENT_REQUEST_SUBMITTED,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            club != null ? club.getId() : null,
                            null, // relatedNewsId
                            null, // relatedTeamId
                            null  // relatedRequestId
                    );
                    log.info("Sent notification to {} staff members for event request submission: {}", staffIds.size(), savedEvent.getId());
                }
            } catch (Exception e) {
                log.error("Failed to send notification for event request submission: {}", e.getMessage(), e);
            }
            
            return eventMapper.toDto(savedEvent);
            
        } else if (isClubOfficer) {
            // TEAM_OFFICER: Gửi lên CLUB_OFFICER
            savedEvent.setIsDraft(true);
            eventRepository.save(savedEvent);
            
            RequestEvent requestEvent = RequestEvent.builder()
                    .requestTitle(request.getTitle())
                    .status(RequestStatus.PENDING_CLUB)
                    .requestDate(LocalDateTime.now())
                    .description(request.getDescription())
                    .event(savedEvent)
                    .createdBy(user)
                    .build();
            
            requestEventRepository.save(requestEvent);
            requestEventRepository.flush();
            
            // 🔔 WebSocket + Notification: Gửi cho tất cả Club Officers (Club Presidents) của CLB
            try {
                if (club != null) {
                    List<Long> managerIds = notificationService.getClubManagers(club.getId());
                    if (!managerIds.isEmpty()) {
                        EventWebSocketPayload payload = EventWebSocketPayload.builder()
                                .eventId(savedEvent.getId())
                                .eventTitle(savedEvent.getTitle())
                                .requestEventId(requestEvent.getId())
                                .status(RequestStatus.PENDING_CLUB)
                                .clubId(club.getId())
                                .clubName(club.getClubName())
                                .creatorId(user.getId())
                                .creatorName(user.getFullName())
                                .creatorEmail(user.getEmail())
                                .startTime(savedEvent.getStartTime())
                                .endTime(savedEvent.getEndTime())
                                .location(savedEvent.getLocation())
                                .eventTypeName(eventType != null ? eventType.getTypeName() : null)
                                .message(String.format("%s đã gửi yêu cầu tạo sự kiện: %s",
                                        user.getFullName(),
                                        savedEvent.getTitle()))
                                .build();
                        
                        // Gửi WebSocket cho từng Club Officer (giống như CANCELLED_BY_STAFF và RESTORED_BY_STAFF)
                        List<User> managers = userRepository.findAllById(managerIds);
                        for (User manager : managers) {
                            if (manager.getEmail() != null) {
                                webSocketService.sendToUser(manager.getEmail(), "EVENT", "REQUEST_SUBMITTED", payload);
                            }
                        }
                        log.info("Sent WebSocket notification to {} club managers for event request submission: {}", managers.size(), savedEvent.getId());
                        
                        // Gửi Notification cho từng Club Officer
                        String title = "Yêu cầu tạo sự kiện mới";
                        String message = String.format("%s đã gửi yêu cầu tạo sự kiện \"%s\" cho CLB %s",
                                user.getFullName(),
                                savedEvent.getTitle(),
                                club.getClubName());
                        String actionUrl = "/myclub/" + club.getId() + "/events";
                        
                        for (Long managerId : managerIds) {
                            try {
                                notificationService.sendToUser(
                                        managerId,
                                        userId,
                                        title,
                                        message,
                                        NotificationType.EVENT_REQUEST_SUBMITTED,
                                        NotificationPriority.NORMAL,
                                        actionUrl,
                                        club.getId(),
                                        null, // relatedNewsId
                                        null, // relatedTeamId
                                        null,  // relatedRequestId
                                        savedEvent.getId() // relatedEventId
                                );
                            } catch (Exception e) {
                                log.error("Failed to send notification to manager {}: {}", managerId, e.getMessage());
                            }
                        }
                        log.info("Sent notification to {} club managers for event request submission: {}", managerIds.size(), savedEvent.getId());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to send WebSocket/Notification for event request submission: {}", e.getMessage(), e);
            }
            
            return eventMapper.toDto(savedEvent);
        }
        
        throw new ForbiddenException("Role không được hỗ trợ cho việc tạo sự kiện này.");
    }
    

    private void notifyClubMembersAboutMeeting(Event event, Club club, User creator) {
        try {
            List<ClubMemberShip> activeMembers = clubMemberShipRepository.findByClubIdAndStatus(
                    club.getId(),
                    ClubMemberShipStatus.ACTIVE
            );

            if (activeMembers == null || activeMembers.isEmpty()) {
                log.info("No active members to notify for club {}", club.getId());
                return;
            }

            List<Long> recipientIds = activeMembers.stream()
                    .map(ClubMemberShip::getUser)
                    .filter(Objects::nonNull)
                    .map(User::getId)
                    .filter(id -> creator == null || !Objects.equals(id, creator.getId()))
                    .distinct()
                    .toList();

            if (recipientIds.isEmpty()) {
                log.info("No recipients remain after filtering creator for club {}", club.getId());
            } else {
                String title = String.format("CLB %s có buổi meeting mới", club.getClubName());
                String formattedStart = event.getStartTime() != null
                        ? event.getStartTime().format(MEETING_TIME_FORMATTER)
                        : "thời gian sẽ cập nhật";
                String location = event.getLocation() != null ? event.getLocation() : "địa điểm sẽ cập nhật";
                String message = String.format("Buổi meeting \"%s\" sẽ diễn ra lúc %s tại %s.",
                        event.getTitle(),
                        formattedStart,
                        location);
                String actionUrl = String.format("/events/%d", event.getId());
                for (Long recipientId : recipientIds) {
                    try {
                        notificationService.sendToUser(
                                recipientId,
                                creator != null ? creator.getId() : null,
                                title,
                                message,
                                NotificationType.EVENT_CREATED,
                                NotificationPriority.HIGH,
                                actionUrl,
                                club.getId(),
                                null,
                                null,
                                null,
                                event.getId()
                        );
                    } catch (AppException appException) {
                        log.warn("Failed to send meeting notification to user {}: {}", recipientId, appException.getMessage());
                    }
                }
                log.info("Sent meeting notifications to {} members of club {}", recipientIds.size(), club.getId());
            }

            EventWebSocketPayload payload = EventWebSocketPayload.builder()
                    .eventId(event.getId())
                    .eventTitle(event.getTitle())
                    .clubId(club.getId())
                    .clubName(club.getClubName())
                    .creatorId(creator != null ? creator.getId() : null)
                    .creatorName(creator != null ? creator.getFullName() : null)
                    .creatorEmail(creator != null ? creator.getEmail() : null)
                    .startTime(event.getStartTime())
                    .endTime(event.getEndTime())
                    .location(event.getLocation())
                    .eventTypeName(event.getEventType() != null ? event.getEventType().getTypeName() : "MEETING")
                    .message(String.format("CLB %s vừa tạo buổi meeting \"%s\".",
                            club.getClubName(),
                            event.getTitle()))
                    .build();

            webSocketService.broadcastToClub(club.getId(), "EVENT", "MEETING_CREATED", payload);
            log.info("Broadcast MEETING_CREATED websocket for event {} to club {}", event.getId(), club.getId());
        } catch (Exception e) {
            log.error("Failed to notify members about meeting event {}: {}", event.getId(), e.getMessage(), e);
        }
    }


    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
    }


    private Club getClubById(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException("Club not found with ID: " + clubId));
    }


    private EventType getEventTypeById(Long eventTypeId) {
        if (eventTypeId == null) return null;
        return eventTypeRepository.findById(eventTypeId)
                .orElseThrow(() -> new NotFoundException("Event type not found with ID: " + eventTypeId));
    }
    

    private void uploadAndSaveEventMedia(Event event, List<MultipartFile> mediaFiles) {
        if (mediaFiles == null || mediaFiles.isEmpty()) {
            return;
        }
        
        List<EventMedia> eventMediaList = new ArrayList<>();
        int displayOrder = 1;
        
        for (MultipartFile file : mediaFiles) {
            if (file.isEmpty()) {
                continue;
            }
            
            try {
                String contentType = file.getContentType();
                boolean isVideo = contentType != null && contentType.startsWith("video/");
                
                CloudinaryService.UploadResult uploadResult;
                MediaType mediaType;
                
                if (isVideo) {
                    uploadResult = cloudinaryService.uploadVideo(file, "club/events");
                    mediaType = MediaType.VIDEO;
                } else {
                    uploadResult = cloudinaryService.uploadImage(file, "club/events");
                    mediaType = MediaType.IMAGE;
                }
                
                EventMedia eventMedia = EventMedia.builder()
                        .event(event)
                        .mediaUrl(uploadResult.url())
                        .mediaType(mediaType)
                        .displayOrder(displayOrder++)
                        .build();
                
                eventMediaList.add(eventMedia);
                
            } catch (Exception e) {
                log.error("Failed to upload media for event {}: {}", event.getId(), e.getMessage());
            }
        }
        
        if (!eventMediaList.isEmpty()) {
            eventMediaRepository.saveAll(eventMediaList);
        }
    }
    

    @Transactional
    public void approveEventByClub(EventApprovalRequest request, Long userId) {
        RequestEvent requestEvent = requestEventRepository.findByIdWithEventAndClub(request.getRequestEventId())
                .orElseThrow(() -> new NotFoundException("Request event not found"));
        
        if (!roleService.isClubPresident(userId, requestEvent.getEvent().getClub().getId())) {
            throw new ForbiddenException("Chỉ CLUB_OFFICER mới có quyền duyệt");
        }
        
        // Kiểm tra status
        if (requestEvent.getStatus() != RequestStatus.PENDING_CLUB) {
            throw new ForbiddenException("Request không ở trạng thái PENDING_CLUB");
        }
        
        Event event = requestEvent.getEvent();
        
        // Kiểm tra thời gian bắt đầu: không cho duyệt nếu sự kiện đã bắt đầu
        if (request.getStatus() == RequestStatus.APPROVED_CLUB && event != null) {
            if (event.getStartTime().isBefore(LocalDateTime.now())) {
                throw new ForbiddenException("Sự kiện đã bắt đầu, không thể duyệt");
            }
        }
        
        if (request.getStatus() == RequestStatus.APPROVED_CLUB) {
            requestEvent.setStatus(RequestStatus.PENDING_UNIVERSITY);
        } else {
            requestEvent.setStatus(RequestStatus.REJECTED_CLUB);
        }
        
        requestEvent.setResponseMessage(request.getResponseMessage());
        requestEventRepository.save(requestEvent);
        requestEventRepository.flush();
        
        Club club = event != null ? event.getClub() : null;
        User creator = requestEvent.getCreatedBy();
        User approver = getUserById(userId); // Người duyệt (Club Officer)
        
        // 🔔 WebSocket + Notification
        if (request.getStatus() == RequestStatus.APPROVED_CLUB) {
            // Approve: Gửi cho Team Officer (creator)
            try {
                EventWebSocketPayload payload = EventWebSocketPayload.builder()
                        .eventId(event != null ? event.getId() : null)
                        .eventTitle(event != null ? event.getTitle() : null)
                        .requestEventId(requestEvent.getId())
                        .status(RequestStatus.PENDING_UNIVERSITY)
                        .clubId(club != null ? club.getId() : null)
                        .clubName(club != null ? club.getClubName() : null)
                        .creatorId(creator != null ? creator.getId() : null)
                        .creatorName(creator != null ? creator.getFullName() : null)
                        .creatorEmail(creator != null ? creator.getEmail() : null)
                        .startTime(event != null ? event.getStartTime() : null)
                        .endTime(event != null ? event.getEndTime() : null)
                        .location(event != null ? event.getLocation() : null)
                        .eventTypeName(event != null && event.getEventType() != null ? event.getEventType().getTypeName() : null)
                        .responseMessage(requestEvent.getResponseMessage())
                        .message(String.format("Yêu cầu tạo sự kiện của bạn đã được %s (Chủ nhiệm CLB %s) duyệt và đã chuyển lên Nhân viên phòng IC-PDP",
                                approver.getFullName(),
                                club != null ? club.getClubName() : "N/A"))
                        .build();
                
                if (creator != null && creator.getEmail() != null) {
                    webSocketService.sendToUser(creator.getEmail(), "EVENT", "REQUEST_APPROVED_BY_CLUB", payload);
                    log.info("Sent WebSocket notification to creator for event approval by club: {}", requestEvent.getId());
                }
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification for event approval by club: {}", e.getMessage(), e);
            }
            
            try {
                if (creator != null) {
                    String title = "Yêu cầu tạo sự kiện đã được duyệt";
                    String message = String.format("Yêu cầu tạo sự kiện \"%s\" của bạn đã được %s (Chủ nhiệm CLB %s) duyệt và đã chuyển lên Nhân viên phòng IC-PDP để xem xét",
                            event != null ? event.getTitle() : "N/A",
                            approver.getFullName(),
                            club != null ? club.getClubName() : "N/A");
                    String actionUrl = club != null
                            ? "/myclub/" + club.getId() + "/events"
                            : "/myclub/select";
                    
                    notificationService.sendToUser(
                            creator.getId(),
                            userId,
                            title,
                            message,
                            NotificationType.EVENT_REQUEST_APPROVED_BY_CLUB,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            club != null ? club.getId() : null,
                            null, null, null,
                            event != null ? event.getId() : null
                    );
                    log.info("Sent notification to creator for event approval by club: {}", requestEvent.getId());
                }
            } catch (Exception e) {
                log.error("Failed to send notification for event approval by club: {}", e.getMessage(), e);
            }
            
            // 🔔 WebSocket + Notification: Gửi cho tất cả Nhân viên phòng IC-PDP (vì request đã chuyển sang PENDING_UNIVERSITY)
            try {
                EventWebSocketPayload staffPayload = EventWebSocketPayload.builder()
                        .eventId(event != null ? event.getId() : null)
                        .eventTitle(event != null ? event.getTitle() : null)
                        .requestEventId(requestEvent.getId())
                        .status(RequestStatus.PENDING_UNIVERSITY)
                        .clubId(club != null ? club.getId() : null)
                        .clubName(club != null ? club.getClubName() : null)
                        .creatorId(creator != null ? creator.getId() : null)
                        .creatorName(creator != null ? creator.getFullName() : null)
                        .creatorEmail(creator != null ? creator.getEmail() : null)
                        .startTime(event != null ? event.getStartTime() : null)
                        .endTime(event != null ? event.getEndTime() : null)
                        .location(event != null ? event.getLocation() : null)
                        .eventTypeName(event != null && event.getEventType() != null ? event.getEventType().getTypeName() : null)
                        .message(String.format("Yêu cầu tạo sự kiện \"%s\" từ CLB %s đã được %s (Chủ nhiệm CLB) duyệt và đang chờ bạn xem xét",
                                event != null ? event.getTitle() : "N/A",
                                club != null ? club.getClubName() : "N/A",
                                approver.getFullName()))
                        .build();
                
                webSocketService.broadcastToSystemRole("STAFF", "EVENT", "REQUEST_SUBMITTED", staffPayload);
                log.info("Sent WebSocket notification to STAFF for event approval by club: {}", requestEvent.getId());
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification to STAFF for event approval by club: {}", e.getMessage(), e);
            }
            
            try {
                List<User> staffUsers = userRepository.findBySystemRole_RoleNameIgnoreCase("STAFF");
                if (!staffUsers.isEmpty()) {
                    String title = "Yêu cầu tạo sự kiện mới cần duyệt";
                    String message = String.format("Yêu cầu tạo sự kiện \"%s\" từ CLB %s đã được %s (Chủ nhiệm CLB) duyệt và đang chờ bạn xem xét",
                            event != null ? event.getTitle() : "N/A",
                            club != null ? club.getClubName() : "N/A",
                            approver.getFullName());
                    String actionUrl = "/staff/events";
                    
                    List<Long> staffIds = staffUsers.stream().map(User::getId).toList();
                    notificationService.sendToUsers(
                            staffIds,
                            userId,
                            title,
                            message,
                            NotificationType.EVENT_REQUEST_SUBMITTED,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            club != null ? club.getId() : null,
                            null, // relatedNewsId
                            null, // relatedTeamId
                            null  // relatedRequestId
                    );
                    log.info("Sent notification to {} staff members for event approval by club: {}", staffIds.size(), requestEvent.getId());
                }
            } catch (Exception e) {
                log.error("Failed to send notification to STAFF for event approval by club: {}", e.getMessage(), e);
            }
        } else {
            // Reject: Gửi cho Team Officer (creator)
            try {
                EventWebSocketPayload payload = EventWebSocketPayload.builder()
                        .eventId(event != null ? event.getId() : null)
                        .eventTitle(event != null ? event.getTitle() : null)
                        .requestEventId(requestEvent.getId())
                        .status(RequestStatus.REJECTED_CLUB)
                        .clubId(club != null ? club.getId() : null)
                        .clubName(club != null ? club.getClubName() : null)
                        .creatorId(creator != null ? creator.getId() : null)
                        .creatorName(creator != null ? creator.getFullName() : null)
                        .creatorEmail(creator != null ? creator.getEmail() : null)
                        .startTime(event != null ? event.getStartTime() : null)
                        .endTime(event != null ? event.getEndTime() : null)
                        .location(event != null ? event.getLocation() : null)
                        .eventTypeName(event != null && event.getEventType() != null ? event.getEventType().getTypeName() : null)
                        .responseMessage(requestEvent.getResponseMessage())
                        .reason(requestEvent.getResponseMessage())
                        .message(String.format("Yêu cầu tạo sự kiện \"%s\" của bạn đã bị %s (Chủ nhiệm CLB %s) từ chối. Lý do: %s",
                                event != null ? event.getTitle() : "N/A",
                                approver.getFullName(),
                                club != null ? club.getClubName() : "N/A",
                                requestEvent.getResponseMessage() != null ? requestEvent.getResponseMessage() : "Không có lý do"))
                        .build();
                
                if (creator != null && creator.getEmail() != null) {
                    webSocketService.sendToUser(creator.getEmail(), "EVENT", "REQUEST_REJECTED_BY_CLUB", payload);
                    log.info("Sent WebSocket notification to creator for event rejection by club: {}", requestEvent.getId());
                }
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification for event rejection by club: {}", e.getMessage(), e);
            }
            
            try {
                if (creator != null) {
                    String title = "Yêu cầu tạo sự kiện đã bị từ chối";
                    String message = String.format("Yêu cầu tạo sự kiện \"%s\" của bạn đã bị %s (Chủ nhiệm CLB %s) từ chối. Lý do: %s",
                            event != null ? event.getTitle() : "N/A",
                            approver.getFullName(),
                            club != null ? club.getClubName() : "N/A",
                            requestEvent.getResponseMessage() != null ? requestEvent.getResponseMessage() : "Không có lý do");
                    String actionUrl = club != null
                            ? "/myclub/" + club.getId() + "/events"
                            : "/myclub/select";
                    
                    notificationService.sendToUser(
                            creator.getId(),
                            userId,
                            title,
                            message,
                            NotificationType.EVENT_REQUEST_REJECTED_BY_CLUB,
                            NotificationPriority.HIGH,
                            actionUrl,
                            club != null ? club.getId() : null,
                            null, null, null,
                            event != null ? event.getId() : null
                    );
                    log.info("Sent notification to creator for event rejection by club: {}", requestEvent.getId());
                }
            } catch (Exception e) {
                log.error("Failed to send notification for event rejection by club: {}", e.getMessage(), e);
            }
        }

    }

    private boolean hasEventOfficerPrivileges(Long userId, Long clubId) {
        return roleService.isClubOfficer(userId, clubId) || roleService.isClubTreasurer(userId, clubId);
    }

    private boolean hasEventOfficerPrivileges(Long userId) {
        return roleService.isClubOfficer(userId) || roleService.isClubTreasurer(userId);
    }


    @Transactional
    public void approveEventByStaff(EventApprovalRequest request, Long userId) {
        RequestEvent requestEvent = requestEventRepository.findByIdWithEventAndClub(request.getRequestEventId())
                .orElseThrow(() -> new NotFoundException("Request event not found"));
        
        if (!roleService.isStaff(userId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền duyệt");
        }
        
        // Kiểm tra status
        if (requestEvent.getStatus() != RequestStatus.PENDING_UNIVERSITY) {
            throw new ForbiddenException("Request không ở trạng thái PENDING_UNIVERSITY");
        }
        
        Event event = requestEvent.getEvent();
        
        // Kiểm tra thời gian bắt đầu: không cho duyệt nếu sự kiện đã bắt đầu
        if (request.getStatus() == RequestStatus.APPROVED_UNIVERSITY && event != null) {
            if (event.getStartTime().isBefore(LocalDateTime.now())) {
                throw new ForbiddenException("Sự kiện đã bắt đầu, không thể duyệt");
            }
        }
        
        if (request.getStatus() == RequestStatus.APPROVED_UNIVERSITY) {
            event.setIsDraft(false);
            eventRepository.save(event);
            
            requestEvent.setStatus(RequestStatus.APPROVED_UNIVERSITY);
        } else {
            requestEvent.setStatus(RequestStatus.REJECTED_UNIVERSITY);
        }
        
        requestEvent.setResponseMessage(request.getResponseMessage());
        requestEventRepository.save(requestEvent);
        requestEventRepository.flush();
        
        Club club = event != null ? event.getClub() : null;
        User creator = requestEvent.getCreatedBy();
        User approver = getUserById(userId); // Người duyệt (Nhân viên phòng IC-PDP)
        
        // 🔔 WebSocket + Notification
        if (request.getStatus() == RequestStatus.APPROVED_UNIVERSITY) {
            // Approve: Gửi cho Club Officer (creator)
            try {
                EventWebSocketPayload payload = EventWebSocketPayload.builder()
                        .eventId(event != null ? event.getId() : null)
                        .eventTitle(event != null ? event.getTitle() : null)
                        .requestEventId(requestEvent.getId())
                        .status(RequestStatus.APPROVED_UNIVERSITY)
                        .clubId(club != null ? club.getId() : null)
                        .clubName(club != null ? club.getClubName() : null)
                        .creatorId(creator != null ? creator.getId() : null)
                        .creatorName(creator != null ? creator.getFullName() : null)
                        .creatorEmail(creator != null ? creator.getEmail() : null)
                        .startTime(event != null ? event.getStartTime() : null)
                        .endTime(event != null ? event.getEndTime() : null)
                        .location(event != null ? event.getLocation() : null)
                        .eventTypeName(event != null && event.getEventType() != null ? event.getEventType().getTypeName() : null)
                        .responseMessage(requestEvent.getResponseMessage())
                        .message(String.format("Yêu cầu tạo sự kiện của bạn đã được %s (Nhân viên phòng IC-PDP) duyệt và sự kiện đã được công bố",
                                approver.getFullName()))
                        .build();
                
                if (creator != null && creator.getEmail() != null) {
                    webSocketService.sendToUser(creator.getEmail(), "EVENT", "REQUEST_APPROVED_BY_UNIVERSITY", payload);
                    log.info("Sent WebSocket notification to creator for event approval by staff: {}", requestEvent.getId());
                }
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification for event approval by staff: {}", e.getMessage(), e);
            }
            
            try {
                if (creator != null) {
                    String title = "Yêu cầu tạo sự kiện đã được duyệt";
                    String message = String.format("Yêu cầu tạo sự kiện \"%s\" của bạn đã được %s (Nhân viên phòng IC-PDP) duyệt và sự kiện đã được công bố",
                            event != null ? event.getTitle() : "N/A",
                            approver.getFullName());
                    String actionUrl = "/events/" + (event != null ? event.getId() : "");
                    
                    notificationService.sendToUser(
                            creator.getId(),
                            userId,
                            title,
                            message,
                            NotificationType.EVENT_REQUEST_APPROVED_BY_UNIVERSITY,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            club != null ? club.getId() : null,
                            null, null, null,
                            event != null ? event.getId() : null
                    );
                    log.info("Sent notification to creator for event approval by staff: {}", requestEvent.getId());
                }
            } catch (Exception e) {
                log.error("Failed to send notification for event approval by staff: {}", e.getMessage(), e);
            }
        } else {
            // Reject: Gửi cho người tạo request (creator - Club Officer)
            try {
                EventWebSocketPayload payload = EventWebSocketPayload.builder()
                        .eventId(event != null ? event.getId() : null)
                        .eventTitle(event != null ? event.getTitle() : null)
                        .requestEventId(requestEvent.getId())
                        .status(RequestStatus.REJECTED_UNIVERSITY)
                        .clubId(club != null ? club.getId() : null)
                        .clubName(club != null ? club.getClubName() : null)
                        .creatorId(creator != null ? creator.getId() : null)
                        .creatorName(creator != null ? creator.getFullName() : null)
                        .creatorEmail(creator != null ? creator.getEmail() : null)
                        .startTime(event != null ? event.getStartTime() : null)
                        .endTime(event != null ? event.getEndTime() : null)
                        .location(event != null ? event.getLocation() : null)
                        .eventTypeName(event != null && event.getEventType() != null ? event.getEventType().getTypeName() : null)
                        .responseMessage(requestEvent.getResponseMessage())
                        .reason(requestEvent.getResponseMessage())
                        .approverId(userId)
                        .approverName(approver.getFullName())
                        .approverRole("STAFF")
                        .message(String.format("Yêu cầu tạo sự kiện \"%s\" của bạn đã bị %s (Nhân viên phòng IC-PDP) từ chối. Lý do: %s",
                                event != null ? event.getTitle() : "N/A",
                                approver.getFullName(),
                                requestEvent.getResponseMessage() != null ? requestEvent.getResponseMessage() : "Không có lý do"))
                        .build();
                
                if (creator != null && creator.getEmail() != null) {
                    webSocketService.sendToUser(creator.getEmail(), "EVENT", "REQUEST_REJECTED_BY_UNIVERSITY", payload);
                    log.info("Sent WebSocket notification to creator for event rejection by staff: {}", requestEvent.getId());
                }
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification for event rejection by staff: {}", e.getMessage(), e);
            }
            
            try {
                if (creator != null) {
                    String title = "Yêu cầu tạo sự kiện đã bị từ chối";
                    String message = String.format("Yêu cầu tạo sự kiện \"%s\" của bạn đã bị %s (Nhân viên phòng IC-PDP) từ chối. Lý do: %s",
                            event != null ? event.getTitle() : "N/A",
                            approver.getFullName(),
                            requestEvent.getResponseMessage() != null ? requestEvent.getResponseMessage() : "Không có lý do");
                    String actionUrl = club != null
                            ? "/myclub/" + club.getId() + "/events"
                            : "/myclub/select";
                    
                    notificationService.sendToUser(
                            creator.getId(),
                            userId,
                            title,
                            message,
                            NotificationType.EVENT_REQUEST_REJECTED_BY_UNIVERSITY,
                            NotificationPriority.HIGH,
                            actionUrl,
                            club != null ? club.getId() : null,
                            null, null, null,
                            event != null ? event.getId() : null
                    );
                    log.info("Sent notification to creator for event rejection by staff: {}", requestEvent.getId());
                }
            } catch (Exception e) {
                log.error("Failed to send notification for event rejection by staff: {}", e.getMessage(), e);
            }
        }
    }
    
    public List<PendingRequestDto> getPendingRequests(Long userId, Long clubId) {
        if (roleService.isStaff(userId)) {
            List<RequestEvent> list = requestEventRepository.findAllByStatusWithAll(RequestStatus.PENDING_UNIVERSITY);
            return mapToPendingDtos(list);
        }
        
        // Nếu có clubId, check role trong club cụ thể đó
        if (clubId != null && clubId > 0) {
            if (roleService.isClubPresident(userId, clubId)) {
                // Club Officer: Lấy requests PENDING_CLUB của club này
                List<RequestEvent> result = requestEventRepository.findAllByStatusAndClubIdWithAll(RequestStatus.PENDING_CLUB, clubId);
                return mapToPendingDtos(result);
            }
            // Nếu không phải Club Officer của club này, trả về empty
            return List.of();
        }
        
        // Fallback: Nếu không có clubId, check global role và lấy tất cả clubs
        if (roleService.isClubPresident(userId)) {
            List<Club> clubs = roleService.getClubsWhereUserIsPresident(userId);
            if (clubs.isEmpty()) return List.of();
            List<RequestEvent> result = new ArrayList<>();
            for (Club club : clubs) {
                result.addAll(requestEventRepository.findAllByStatusAndClubIdWithAll(RequestStatus.PENDING_CLUB, club.getId()));
            }
            return mapToPendingDtos(result);
        }

        return List.of();
    }



    /**
     * Lấy các event và trạng thái request chờ duyệt mà user này tạo (theo club)
     * Hoặc draft events của Nhân viên phòng IC-PDP (không có club)
     */
    public List<MyDraftEventDto> getMyDraftEvents(Long userId, Long clubId) {
        // Nhân viên phòng IC-PDP: Lấy draft events không có club (toàn trường)
        // Nhân viên phòng IC-PDP tạo event không có RequestEvent, chỉ có isDraft = true và clubId = null
        if (roleService.isStaff(userId)) {
            List<Event> staffDrafts = eventRepository.findByIsDraftTrueAndClubIsNull();
            return staffDrafts.stream()
                .filter(e -> {
                    // Chỉ lấy events do user này tạo (thông qua RequestEvent hoặc trực tiếp)
                    // Vì Nhân viên phòng IC-PDP tạo event không có RequestEvent, cần check creator
                    // Tạm thời lấy tất cả draft events không có club (vì không có createdBy trong Event)
                    // Có thể cần thêm field createdBy vào Event entity sau
                    return true;
                })
                .map(e -> MyDraftEventDto.builder()
                    .event(eventMapper.toDto(e))
                    .requestStatus(null) // Nhân viên phòng IC-PDP draft events không có RequestStatus
                    .build())
                .toList();
        }
        
        List<RequestStatus> statuses;
        boolean isClubPresident = false;
        boolean isClubOfficer = false;
        
        // Check role theo clubId nếu có, nếu không thì check global role
        if (clubId != null && clubId > 0) {
            isClubPresident = roleService.isClubPresident(userId, clubId);
            isClubOfficer = hasEventOfficerPrivileges(userId, clubId);
            
            if (isClubPresident) {
                statuses = List.of(RequestStatus.PENDING_UNIVERSITY);
            } else if (isClubOfficer) {
                statuses = List.of(RequestStatus.PENDING_CLUB);
            } else {
                return List.of();
            }
        } else {
            // Fallback: check global role (for backward compatibility)
            isClubPresident = roleService.isClubPresident(userId);
            isClubOfficer = hasEventOfficerPrivileges(userId);
            
            if (isClubPresident) {
                statuses = List.of(RequestStatus.PENDING_UNIVERSITY);
            } else if (isClubOfficer) {
                statuses = List.of(RequestStatus.PENDING_CLUB);
            } else {
                return List.of();
            }
        }
        
        List<RequestEvent> reqEvents;
        
        // CLUB_OFFICER (isClubPresident): Lấy tất cả draft events của club với status PENDING_UNIVERSITY
        // (không chỉ events do họ tạo, mà cả events do TEAM_OFFICER tạo và đã được họ duyệt)
        if (isClubPresident && clubId != null && clubId > 0) {
            reqEvents = requestEventRepository.findAllByStatusesAndClubIdWithAll(statuses, clubId);
        } else {
            // TEAM_OFFICER: Chỉ lấy events do chính họ tạo
            reqEvents = requestEventRepository.findByCreatedByIdAndStatusIn(userId, statuses);
        }
        
        if (reqEvents == null || reqEvents.isEmpty()) return List.of();
        
        // Filter by clubId if provided (for TEAM_OFFICER case)
        return reqEvents.stream()
            .filter(re -> re.getEvent() != null)
            .filter(re -> {
                if (clubId == null || clubId <= 0) return true;
                Event event = re.getEvent();
                if (event.getClub() == null) return clubId == null;
                return event.getClub().getId() != null && event.getClub().getId().equals(clubId);
            })
            .map(re -> MyDraftEventDto.builder()
                .event(eventMapper.toDto(re.getEvent()))
                .requestStatus(re.getStatus())
                .build())
            .toList();
    }

    @Transactional
    public EventData updateMyDraftEvent(Long eventId, UpdateEventRequest request, Long userId) {
        // Lấy event trước để check clubId
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện"));
        
        // Lấy clubId từ event
        Long clubId = event.getClub() != null ? event.getClub().getId() : null;

        // Nhân viên phòng IC-PDP: Update draft events (isDraft = true, club = null) hoặc published events (isDraft = false, club = null)
        if (event.getClub() == null && roleService.isStaff(userId)) {
            // Draft events: cho phép update bất cứ lúc nào
            // Published events: chỉ cho update trước khi bắt đầu
            if (Boolean.FALSE.equals(event.getIsDraft()) && event.getStartTime().isBefore(LocalDateTime.now())) {
                throw new ForbiddenException("Sự kiện đã bắt đầu, không thể cập nhật");
            }
            Event eventToUpdate = event;
            if (request.getTitle() != null) eventToUpdate.setTitle(request.getTitle());
            if (request.getDescription() != null) eventToUpdate.setDescription(request.getDescription());
            if (request.getLocation() != null) eventToUpdate.setLocation(request.getLocation());
            if (request.getStartTime() != null) eventToUpdate.setStartTime(request.getStartTime());
            if (request.getEndTime() != null) eventToUpdate.setEndTime(request.getEndTime());
            if (request.getEventTypeId() != null) {
                EventType newType = getEventTypeById(request.getEventTypeId());
                eventToUpdate.setEventType(newType);
            }
            Event savedStaffEvent = eventRepository.save(eventToUpdate);
            
            // Xóa media cũ nếu có
            if (request.getDeleteMediaIds() != null && !request.getDeleteMediaIds().isEmpty()) {
                eventMediaRepository.deleteAllById(request.getDeleteMediaIds());
            }
            
            // Thêm media mới nếu có
            if (request.getMediaFiles() != null && !request.getMediaFiles().isEmpty()) {
                uploadAndSaveEventMedia(savedStaffEvent, request.getMediaFiles());
            }
            return eventMapper.toDto(savedStaffEvent);
        }


        boolean isMeeting = event.getEventType() != null &&
                "MEETING".equalsIgnoreCase(event.getEventType().getTypeName());
        if (Boolean.FALSE.equals(event.getIsDraft()) && isMeeting) {
            if (event.getStartTime().isBefore(LocalDateTime.now())) {
                throw new ForbiddenException("Sự kiện đã bắt đầu, không thể cập nhật");
            }
            // Ưu tiên xác thực theo creator của RequestEvent (nếu có)
            boolean isCreator = requestEventRepository
                    .findByEventIdAndCreatedById(eventId, userId)
                    .isPresent();
            boolean isClubLeader = (clubId != null) && (roleService.isClubPresident(userId, clubId)
                    || hasEventOfficerPrivileges(userId, clubId));
            if (!isCreator && !isClubLeader) {
                throw new ForbiddenException("Bạn không có quyền cập nhật sự kiện này");
            }

            Event eventToUpdate = event; // dùng trực tiếp event đã lấy
            // Không cho đổi club và không cho đổi loại ra khỏi MEETING
            if (request.getEventTypeId() != null) {
                EventType newType = getEventTypeById(request.getEventTypeId());
                if (!"MEETING".equalsIgnoreCase(newType.getTypeName())) {
                    throw new ForbiddenException("Không thể đổi loại sự kiện MEETING thành loại khác sau khi đã công bố");
                }
                eventToUpdate.setEventType(newType);
            }
            if (request.getTitle() != null) eventToUpdate.setTitle(request.getTitle());
            if (request.getDescription() != null) eventToUpdate.setDescription(request.getDescription());
            if (request.getLocation() != null) eventToUpdate.setLocation(request.getLocation());
            if (request.getStartTime() != null) eventToUpdate.setStartTime(request.getStartTime());
            if (request.getEndTime() != null) eventToUpdate.setEndTime(request.getEndTime());

            Event savedMeeting = eventRepository.save(eventToUpdate);
            
            // Xóa media cũ nếu có
            if (request.getDeleteMediaIds() != null && !request.getDeleteMediaIds().isEmpty()) {
                eventMediaRepository.deleteAllById(request.getDeleteMediaIds());
            }
            
            // Thêm media mới nếu có
            if (request.getMediaFiles() != null && !request.getMediaFiles().isEmpty()) {
                uploadAndSaveEventMedia(savedMeeting, request.getMediaFiles());
            }
            return eventMapper.toDto(savedMeeting);
        }
        
        // Xác định status hợp lệ theo role và clubId
        List<RequestStatus> allowedStatuses;
        boolean isClubPresident = false;
        boolean isClubOfficer = false;
        
        if (clubId != null && clubId > 0) {
            // Check role theo clubId
            isClubPresident = roleService.isClubPresident(userId, clubId);
            isClubOfficer = hasEventOfficerPrivileges(userId, clubId);
            
            if (isClubPresident) {
                allowedStatuses = List.of(RequestStatus.PENDING_UNIVERSITY);
            } else if (isClubOfficer) {
                allowedStatuses = List.of(RequestStatus.PENDING_CLUB);
            } else {
                throw new ForbiddenException("Bạn không có quyền cập nhật sự kiện này");
            }
        } else {
            // Event toàn trường hoặc không có club - check global role
            isClubPresident = roleService.isClubPresident(userId);
            isClubOfficer = hasEventOfficerPrivileges(userId);
            
            if (isClubPresident) {
                allowedStatuses = List.of(RequestStatus.PENDING_UNIVERSITY);
            } else if (isClubOfficer) {
                allowedStatuses = List.of(RequestStatus.PENDING_CLUB);
            } else {
                throw new ForbiddenException("Bạn không có quyền cập nhật sự kiện này");
            }
        }

        RequestEvent requestEvent;
        
        // CLUB_OFFICER (isClubPresident): Có thể chỉnh sửa tất cả draft events của club với status PENDING_UNIVERSITY
        // (không chỉ events do họ tạo, mà cả events do TEAM_OFFICER tạo và đã được họ duyệt)
        if (isClubPresident && clubId != null && clubId > 0 && allowedStatuses.contains(RequestStatus.PENDING_UNIVERSITY)) {
            requestEvent = requestEventRepository
                    .findByEventIdAndStatusesAndClubIdWithAll(eventId, allowedStatuses, clubId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện nháp hoặc trạng thái không phù hợp"));
        } else {
            // TEAM_OFFICER: Chỉ có thể chỉnh sửa events do chính họ tạo
            requestEvent = requestEventRepository
                    .findByEventIdAndCreatorWithEventAndStatusIn(eventId, userId, allowedStatuses)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện nháp của bạn hoặc trạng thái không phù hợp"));
        }

        // Đảm bảo event từ requestEvent match với event đã lấy
        Event eventToUpdate = requestEvent.getEvent();
        if (eventToUpdate == null || !eventToUpdate.getId().equals(event.getId())) {
            throw new NotFoundException("Event không tồn tại hoặc không khớp");
        }

        // Cập nhật các trường nếu có (dùng event từ requestEvent để đảm bảo consistency)
        if (request.getTitle() != null) eventToUpdate.setTitle(request.getTitle());
        if (request.getDescription() != null) eventToUpdate.setDescription(request.getDescription());
        if (request.getLocation() != null) eventToUpdate.setLocation(request.getLocation());
        if (request.getStartTime() != null) eventToUpdate.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) eventToUpdate.setEndTime(request.getEndTime());
        if (request.getEventTypeId() != null) {
            EventType eventType = getEventTypeById(request.getEventTypeId());
            eventToUpdate.setEventType(eventType);
        }

        // Nếu đổi sang MEETING thì publish ngay và chốt request
        boolean isMeetingNow = eventToUpdate.getEventType() != null &&
                "MEETING".equalsIgnoreCase(eventToUpdate.getEventType().getTypeName());
        if (isMeetingNow) {
            eventToUpdate.setIsDraft(false);
        }

        Event saved = eventRepository.save(eventToUpdate);

        if (request.getDeleteMediaIds() != null && !request.getDeleteMediaIds().isEmpty()) {
            eventMediaRepository.deleteAllById(request.getDeleteMediaIds());
        }

        if (request.getMediaFiles() != null && !request.getMediaFiles().isEmpty()) {
            uploadAndSaveEventMedia(saved, request.getMediaFiles()); // append ảnh mới
        }

        if (isMeetingNow) {
            requestEvent.setStatus(RequestStatus.APPROVED_UNIVERSITY);
            requestEvent.setResponseMessage("Auto-approved due to MEETING type change");
            // Đồng bộ tiêu đề/mô tả lần cuối trước khi chốt
            requestEvent.setRequestTitle(saved.getTitle());
            requestEvent.setDescription(saved.getDescription());
            requestEventRepository.save(requestEvent);
            requestEventRepository.flush();
            
            // 🔔 WebSocket + Notification: Gửi cho creator (nếu có)
            try {
                User creator = requestEvent.getCreatedBy();
                Club club = saved.getClub();
                
                if (creator != null) {
                    EventWebSocketPayload payload = EventWebSocketPayload.builder()
                            .eventId(saved.getId())
                            .eventTitle(saved.getTitle())
                            .requestEventId(requestEvent.getId())
                            .status(RequestStatus.APPROVED_UNIVERSITY)
                            .clubId(club != null ? club.getId() : null)
                            .clubName(club != null ? club.getClubName() : null)
                            .creatorId(creator.getId())
                            .creatorName(creator.getFullName())
                            .creatorEmail(creator.getEmail())
                            .startTime(saved.getStartTime())
                            .endTime(saved.getEndTime())
                            .location(saved.getLocation())
                            .eventTypeName(saved.getEventType() != null ? saved.getEventType().getTypeName() : null)
                            .responseMessage("Tự động duyệt do đổi sang loại MEETING")
                            .message("Sự kiện \"" + saved.getTitle() + "\" đã được tự động duyệt do đổi sang loại MEETING")
                            .build();
                    
                    if (creator.getEmail() != null) {
                        webSocketService.sendToUser(creator.getEmail(), "EVENT", "AUTO_APPROVED", payload);
                        log.info("Sent WebSocket notification to creator for event auto-approval: {}", saved.getId());
                    }
                    
                    String title = "Sự kiện đã được tự động duyệt";
                    String message = String.format("Sự kiện \"%s\" của bạn đã được tự động duyệt do đổi sang loại MEETING",
                            saved.getTitle());
                    String actionUrl = "/events/" + saved.getId();
                    
                    notificationService.sendToUser(
                            creator.getId(),
                            userId,
                            title,
                            message,
                            NotificationType.EVENT_AUTO_APPROVED,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            club != null ? club.getId() : null,
                            null, null, null,
                            saved.getId()
                    );
                    log.info("Sent notification to creator for event auto-approval: {}", saved.getId());
                }
            } catch (Exception e) {
                log.error("Failed to send WebSocket/Notification for event auto-approval: {}", e.getMessage(), e);
            }
        }
        else {
            // Đồng bộ request title/description với bản nháp đã cập nhật
            if (request.getTitle() != null) requestEvent.setRequestTitle(saved.getTitle());
            if (request.getDescription() != null) requestEvent.setDescription(saved.getDescription());
            requestEventRepository.save(requestEvent);
        }

        return eventMapper.toDto(saved);
    }

    @Transactional
    public void deleteMyDraftEvent(Long eventId, Long userId) {
        // Lấy event trước để check clubId
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện"));
        
        // Lấy clubId từ event
        Long clubId = event.getClub() != null ? event.getClub().getId() : null;

        // Nhân viên phòng IC-PDP: Delete draft events (isDraft = true, club = null) hoặc published events (isDraft = false, club = null)
        if (event.getClub() == null && roleService.isStaff(userId)) {
            // Draft events: cho phép xóa bất cứ lúc nào
            // Published events: chỉ cho xóa trước khi bắt đầu
            if (Boolean.FALSE.equals(event.getIsDraft()) && event.getStartTime().isBefore(LocalDateTime.now())) {
                throw new ForbiddenException("Sự kiện đã bắt đầu, không thể xóa");
            }
            eventMediaRepository.deleteByEvent_Id(event.getId());
            requestEventRepository.findByEventId(eventId).ifPresent(requestEventRepository::delete);
            eventRepository.delete(event);
            return;
        }

        // Trường hợp đặc biệt B: MEETING đã publish — cho phép creator xóa trước khi bắt đầu
        boolean isMeeting = event.getEventType() != null &&
                "MEETING".equalsIgnoreCase(event.getEventType().getTypeName());
        if (Boolean.FALSE.equals(event.getIsDraft()) && isMeeting) {
            if (event.getStartTime().isBefore(LocalDateTime.now())) {
                throw new ForbiddenException("Sự kiện đã bắt đầu, không thể xóa");
            }
            boolean isCreator = requestEventRepository
                    .findByEventIdAndCreatedById(eventId, userId)
                    .isPresent();
            boolean isClubLeader = (clubId != null) && (roleService.isClubPresident(userId, clubId)
                    || hasEventOfficerPrivileges(userId, clubId));
            if (!isCreator && !isClubLeader) {
                throw new ForbiddenException("Bạn không có quyền xóa sự kiện này");
            }
            // Xóa media, request (nếu có), và sự kiện
            eventMediaRepository.deleteByEvent_Id(event.getId());
            requestEventRepository.findByEventId(eventId).ifPresent(requestEventRepository::delete);
            eventRepository.delete(event);
            return;
        }
        
        // Xác định status hợp lệ theo role và clubId
        List<RequestStatus> allowedStatuses;
        boolean isClubPresident = false;
        boolean isClubOfficer = false;
        
        if (clubId != null && clubId > 0) {
            // Check role theo clubId
            isClubPresident = roleService.isClubPresident(userId, clubId);
            isClubOfficer = hasEventOfficerPrivileges(userId, clubId);
            
            if (isClubPresident) {
                allowedStatuses = List.of(RequestStatus.PENDING_UNIVERSITY);
            } else if (isClubOfficer) {
                allowedStatuses = List.of(RequestStatus.PENDING_CLUB);
            } else {
                throw new ForbiddenException("Bạn không có quyền xóa sự kiện này");
            }
        } else {
            // Event toàn trường hoặc không có club - check global role
            isClubPresident = roleService.isClubPresident(userId);
            isClubOfficer = hasEventOfficerPrivileges(userId);
            
            if (isClubPresident) {
                allowedStatuses = List.of(RequestStatus.PENDING_UNIVERSITY);
            } else if (isClubOfficer) {
                allowedStatuses = List.of(RequestStatus.PENDING_CLUB);
            } else {
                throw new ForbiddenException("Bạn không có quyền xóa sự kiện này");
            }
        }

        RequestEvent requestEvent;
        
        // CLUB_OFFICER (isClubPresident): Có thể xóa tất cả draft events của club với status PENDING_UNIVERSITY
        // (không chỉ events do họ tạo, mà cả events do TEAM_OFFICER tạo và đã được họ duyệt)
        if (isClubPresident && clubId != null && clubId > 0 && allowedStatuses.contains(RequestStatus.PENDING_UNIVERSITY)) {
            requestEvent = requestEventRepository
                    .findByEventIdAndStatusesAndClubIdWithAll(eventId, allowedStatuses, clubId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện nháp hoặc trạng thái không phù hợp"));
        } else {
            // TEAM_OFFICER: Chỉ có thể xóa events do chính họ tạo
            requestEvent = requestEventRepository
                    .findByEventIdAndCreatorWithEventAndStatusIn(eventId, userId, allowedStatuses)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện nháp của bạn hoặc trạng thái không phù hợp"));
        }

        if (requestEvent.getEvent() == null) {
            throw new NotFoundException("Event không tồn tại");
        }

        eventMediaRepository.deleteByEvent_Id(event.getId());
        requestEventRepository.delete(requestEvent);
        eventRepository.delete(event);
    }

    // ================= Nhân viên phòng IC-PDP Cancel/Restore =================
    @Transactional
    public void cancelClubEventByStaff(Long eventId, Long userId, String reason) {
        if (!roleService.isStaff(userId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền hủy sự kiện");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện"));
        if (event.getClub() == null) {
            throw new ForbiddenException("Chỉ hủy được sự kiện của CLB");
        }
        if (event.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ForbiddenException("Sự kiện đã bắt đầu, không thể hủy");
        }
        event.setIsDraft(true);
        eventRepository.save(event);
        eventRepository.flush();
        
        Club club = event.getClub();
        RequestEvent requestEvent = requestEventRepository.findByEventId(eventId).orElse(null);
        User creator = requestEvent != null ? requestEvent.getCreatedBy() : null;
        
        // Optionally: lưu reason vào requestEvent nếu tồn tại
        if (requestEvent != null) {
            requestEvent.setResponseMessage(reason);
            requestEventRepository.save(requestEvent);
        }
        
        // 🔔 WebSocket + Notification: Gửi cho tất cả Club Officers của CLB
        try {
            List<Long> recipientIds = new ArrayList<>();
            
            // Thêm Club Officers
            if (club != null) {
                List<Long> managerIds = notificationService.getClubManagers(club.getId());
                recipientIds.addAll(managerIds);
            }
            
            // Thêm creator (Team Officer) nếu có
            if (creator != null && !recipientIds.contains(creator.getId())) {
                recipientIds.add(creator.getId());
            }
            
            if (!recipientIds.isEmpty()) {
                EventWebSocketPayload payload = EventWebSocketPayload.builder()
                        .eventId(event.getId())
                        .eventTitle(event.getTitle())
                        .requestEventId(requestEvent != null ? requestEvent.getId() : null)
                        .status(null) // Cancelled status
                        .clubId(club != null ? club.getId() : null)
                        .clubName(club != null ? club.getClubName() : null)
                        .creatorId(creator != null ? creator.getId() : null)
                        .creatorName(creator != null ? creator.getFullName() : null)
                        .creatorEmail(creator != null ? creator.getEmail() : null)
                        .startTime(event.getStartTime())
                        .endTime(event.getEndTime())
                        .location(event.getLocation())
                        .eventTypeName(event.getEventType() != null ? event.getEventType().getTypeName() : null)
                        .reason(reason)
                        .approverId(userId)
                        .approverName(getUserById(userId).getFullName())
                        .approverRole("STAFF")
                        .message("Sự kiện \"" + event.getTitle() + "\" đã bị Nhân viên phòng IC-PDP hủy" + (reason != null ? ". Lý do: " + reason : ""))
                        .build();
                
                // Gửi WebSocket cho từng recipient (giống như RESTORED_BY_STAFF)
                List<User> recipients = userRepository.findAllById(recipientIds);
                for (User recipient : recipients) {
                    if (recipient.getEmail() != null) {
                        webSocketService.sendToUser(recipient.getEmail(), "EVENT", "CANCELLED_BY_STAFF", payload);
                    }
                }
                log.info("Sent WebSocket notification to {} recipients for event cancellation: {}", recipients.size(), event.getId());
                
                // Gửi Notification cho từng Club Officer
                String title = "Sự kiện đã bị hủy";
                String message = String.format("Sự kiện \"%s\" của CLB %s đã bị Nhân viên phòng IC-PDP hủy%s",
                        event.getTitle(),
                        club != null ? club.getClubName() : "N/A",
                        reason != null ? ". Lý do: " + reason : "");
                String actionUrl = "/events/" + event.getId();
                
                for (Long recipientId : recipientIds) {
                    try {
                        notificationService.sendToUser(
                                recipientId,
                                userId,
                                title,
                                message,
                                NotificationType.EVENT_CANCELLED_BY_STAFF,
                                NotificationPriority.HIGH,
                                actionUrl,
                                club != null ? club.getId() : null,
                                null, null, null,
                                event.getId()
                        );
                    } catch (Exception e) {
                        log.error("Failed to send notification to user {}: {}", recipientId, e.getMessage());
                    }
                }
                log.info("Sent notification to {} recipients for event cancellation: {}", recipientIds.size(), event.getId());
            }
        } catch (Exception e) {
            log.error("Failed to send WebSocket/Notification for event cancellation: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void restoreCancelledEventByStaff(Long eventId, Long userId) {
        if (!roleService.isStaff(userId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền khôi phục sự kiện");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện"));
        if (event.getClub() == null) {
            throw new ForbiddenException("Chỉ khôi phục được sự kiện của CLB");
        }
        if (event.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ForbiddenException("Sự kiện đã bắt đầu, không thể khôi phục");
        }
        event.setIsDraft(false);
        eventRepository.save(event);
        eventRepository.flush();
        
        Club club = event.getClub();
        RequestEvent requestEvent = requestEventRepository.findByEventId(eventId).orElse(null);
        User creator = requestEvent != null ? requestEvent.getCreatedBy() : null;
        
        // 🔔 WebSocket + Notification: Gửi cho Club Officer và Team Officer (nếu có creator)
        try {
            List<Long> recipientIds = new ArrayList<>();
            
            // Thêm Club Officers
            if (club != null) {
                List<Long> managerIds = notificationService.getClubManagers(club.getId());
                recipientIds.addAll(managerIds);
            }
            
            // Thêm creator (Team Officer) nếu có
            if (creator != null && !recipientIds.contains(creator.getId())) {
                recipientIds.add(creator.getId());
            }
            
            if (!recipientIds.isEmpty()) {
                EventWebSocketPayload payload = EventWebSocketPayload.builder()
                        .eventId(event.getId())
                        .eventTitle(event.getTitle())
                        .requestEventId(requestEvent != null ? requestEvent.getId() : null)
                        .status(null) // Restored status
                        .clubId(club != null ? club.getId() : null)
                        .clubName(club != null ? club.getClubName() : null)
                        .creatorId(creator != null ? creator.getId() : null)
                        .creatorName(creator != null ? creator.getFullName() : null)
                        .creatorEmail(creator != null ? creator.getEmail() : null)
                        .startTime(event.getStartTime())
                        .endTime(event.getEndTime())
                        .location(event.getLocation())
                        .eventTypeName(event.getEventType() != null ? event.getEventType().getTypeName() : null)
                        .message("Sự kiện \"" + event.getTitle() + "\" đã được Nhân viên phòng IC-PDP khôi phục")
                        .build();
                
                // Gửi WebSocket cho từng recipient
                List<User> recipients = userRepository.findAllById(recipientIds);
                for (User recipient : recipients) {
                    if (recipient.getEmail() != null) {
                        webSocketService.sendToUser(recipient.getEmail(), "EVENT", "RESTORED_BY_STAFF", payload);
                    }
                }
                log.info("Sent WebSocket notification to {} recipients for event restoration: {}", recipients.size(), event.getId());
                
                // Gửi Notification
                String title = "Sự kiện đã được khôi phục";
                String message = String.format("Sự kiện \"%s\" của CLB %s đã được Nhân viên phòng IC-PDP khôi phục",
                        event.getTitle(),
                        club != null ? club.getClubName() : "N/A");
                String actionUrl = "/events/" + event.getId();
                
                // Gửi notification cho từng user để có thể truyền relatedEventId
                for (Long recipientId : recipientIds) {
                    try {
                        notificationService.sendToUser(
                                recipientId,
                                userId,
                                title,
                                message,
                                NotificationType.EVENT_RESTORED_BY_STAFF,
                                NotificationPriority.NORMAL,
                                actionUrl,
                                club != null ? club.getId() : null,
                                null, null, null,
                                event.getId()
                        );
                    } catch (Exception e) {
                        log.error("Failed to send notification to user {}: {}", recipientId, e.getMessage());
                    }
                }
                log.info("Sent notification to {} recipients for event restoration: {}", recipientIds.size(), event.getId());
            }
        } catch (Exception e) {
            log.error("Failed to send WebSocket/Notification for event restoration: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public EventData publishEventByStaff(Long eventId, Long userId) {
        if (!roleService.isStaff(userId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền publish sự kiện");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện"));
        
        // Chỉ publish được event draft do Nhân viên phòng IC-PDP tạo (không có club hoặc club = null)
        if (event.getClub() != null) {
            throw new ForbiddenException("Chỉ publish được sự kiện toàn trường (không thuộc CLB)");
        }
        
        if (Boolean.FALSE.equals(event.getIsDraft())) {
            throw new ForbiddenException("Sự kiện đã được publish rồi");
        }
        
        event.setIsDraft(false);
        Event saved = eventRepository.save(event);
        eventRepository.flush();
        
        User publisher = getUserById(userId);
        
        // 🔔 WebSocket + Notification: Gửi cho tất cả users không phải Nhân viên phòng IC-PDP (STUDENT, TEAM_OFFICER, CLUB_OFFICER)
        try {
            EventWebSocketPayload payload = EventWebSocketPayload.builder()
                    .eventId(saved.getId())
                    .eventTitle(saved.getTitle())
                    .requestEventId(null) // Nhân viên phòng IC-PDP draft events không có RequestEvent
                    .status(null)
                    .clubId(null) // Event toàn trường
                    .clubName(null)
                    .creatorId(userId)
                    .creatorName(publisher != null ? publisher.getFullName() : null)
                    .creatorEmail(publisher != null ? publisher.getEmail() : null)
                    .startTime(saved.getStartTime())
                    .endTime(saved.getEndTime())
                    .location(saved.getLocation())
                    .eventTypeName(saved.getEventType() != null ? saved.getEventType().getTypeName() : null)
                    .message(String.format("Sự kiện toàn trường \"%s\" đã được %s (Nhân viên phòng IC-PDP) công bố",
                            saved.getTitle(),
                            publisher != null ? publisher.getFullName() : "Nhân viên phòng IC-PDP"))
                    .build();
            
            // Broadcast WebSocket cho STUDENT, TEAM_OFFICER, và CLUB_OFFICER
            webSocketService.broadcastToSystemRole("STUDENT", "EVENT", "PUBLISHED", payload);
            webSocketService.broadcastToSystemRole("TEAM_OFFICER", "EVENT", "PUBLISHED", payload);
            webSocketService.broadcastToSystemRole("CLUB_OFFICER", "EVENT", "PUBLISHED", payload);
            log.info("Sent WebSocket broadcast to STUDENT/TEAM_OFFICER/CLUB_OFFICER roles for event publication: {}", saved.getId());
            
            // Gửi Notification cho tất cả users không phải Nhân viên phòng IC-PDP
            List<Long> recipientIds = new ArrayList<>();
            
            // Lấy STUDENT
            List<User> studentUsers = userRepository.findBySystemRole_RoleNameIgnoreCase("STUDENT");
            recipientIds.addAll(studentUsers.stream().map(User::getId).toList());
            
            // Lấy TEAM_OFFICER
            List<User> teamOfficerUsers = userRepository.findBySystemRole_RoleNameIgnoreCase("TEAM_OFFICER");
            recipientIds.addAll(teamOfficerUsers.stream().map(User::getId).toList());
            
            // Lấy CLUB_OFFICER
            List<User> clubOfficerUsers = userRepository.findBySystemRole_RoleNameIgnoreCase("CLUB_OFFICER");
            recipientIds.addAll(clubOfficerUsers.stream().map(User::getId).toList());
            
            // Loại bỏ duplicate IDs
            recipientIds = recipientIds.stream().distinct().toList();
            
            if (!recipientIds.isEmpty()) {
                String title = "Sự kiện mới đã được công bố";
                String message = String.format("Sự kiện toàn trường \"%s\" đã được %s (Nhân viên phòng IC-PDP) công bố. Thời gian: %s - %s",
                        saved.getTitle(),
                        publisher != null ? publisher.getFullName() : "Nhân viên phòng IC-PDP",
                        saved.getStartTime() != null ? saved.getStartTime().toString() : "N/A",
                        saved.getEndTime() != null ? saved.getEndTime().toString() : "N/A");
                String actionUrl = "/events/" + saved.getId();
                
                // Gửi notification cho từng user để có thể truyền relatedEventId
                for (Long recipientId : recipientIds) {
                    try {
                        notificationService.sendToUser(
                                recipientId,
                                userId,
                                title,
                                message,
                                NotificationType.EVENT_PUBLISHED,
                                NotificationPriority.NORMAL,
                                actionUrl,
                                null, // relatedClubId (event toàn trường)
                                null, null, null,
                                saved.getId() // relatedEventId
                        );
                    } catch (Exception e) {
                        log.error("Failed to send notification to user {}: {}", recipientId, e.getMessage());
                    }
                }
                log.info("Sent notification to {} users (STUDENT/TEAM_OFFICER/CLUB_OFFICER) for event publication: {}", recipientIds.size(), saved.getId());
            }
        } catch (Exception e) {
            log.error("Failed to send WebSocket/Notification for event publication: {}", e.getMessage(), e);
        }
        
        return eventMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<EventData> getStaffCancelledEvents(Long userId, Long clubId) {
        if (!roleService.isStaff(userId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền xem danh sách đã hủy");
        }
        List<Event> list;
        List<RequestStatus> pending = java.util.List.of(RequestStatus.PENDING_CLUB, RequestStatus.PENDING_UNIVERSITY);
        if (clubId != null && clubId > 0) {
            list = eventRepository.findCancelledByStaffAndClubIdExcludingPending(clubId, pending);
        } else {
            list = eventRepository.findCancelledByStaffExcludingPending(pending);
        }
        return list.stream().map(e -> {
            EventData dto = eventMapper.toDto(e);
            List<EventMedia> mediaList = eventMediaRepository.findByEventIdOrderByDisplayOrder(e.getId());
            dto.setMediaUrls(mediaList.stream().map(EventMedia::getMediaUrl).toList());
            dto.setMediaTypes(mediaList.stream().map(m -> m.getMediaType() != null ? m.getMediaType().name() : "IMAGE").toList());
            dto.setMediaIds(mediaList.stream().map(EventMedia::getId).toList());
            dto.setClubId(e.getClub() != null ? e.getClub().getId() : null);
            return dto;
        }).toList();
    }

    @Transactional
    public void staffHardDeleteCancelledEvent(Long eventId, Long userId) {
        if (!roleService.isStaff(userId)) {
            throw new ForbiddenException("Chỉ Nhân viên phòng IC-PDP mới có quyền xóa vĩnh viễn sự kiện");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện"));
        if (event.getClub() == null) {
            throw new ForbiddenException("Chỉ xóa các sự kiện của CLB");
        }
        if (!Boolean.TRUE.equals(event.getIsDraft())) {
            throw new ForbiddenException("Chỉ xóa được sự kiện đang ở trạng thái đã hủy (draft)");
        }
        // Xóa media và request liên quan rồi xóa event
        eventMediaRepository.deleteByEvent_Id(event.getId());
        requestEventRepository.findByEventId(eventId).ifPresent(requestEventRepository::delete);
        eventRepository.delete(event);
    }

    private List<PendingRequestDto> mapToPendingDtos(List<RequestEvent> requestEvents) {
        List<PendingRequestDto> dtos = new ArrayList<>();
        for (RequestEvent re : requestEvents) {
            Event e = re.getEvent();
            Club c = (e != null ? e.getClub() : null);
            User u = re.getCreatedBy();

            PendingRequestDto.EventSummaryDto eventDto = null;
            if (e != null) {
                eventDto = PendingRequestDto.EventSummaryDto.builder()
                        .id(e.getId())
                        .title(e.getTitle())
                        .startTime(e.getStartTime())
                        .endTime(e.getEndTime())
                        .location(e.getLocation())
                        .eventTypeName(e.getEventType() != null ? e.getEventType().getTypeName() : null)
                        .isDraft(Boolean.TRUE.equals(e.getIsDraft()))
                        .build();
            }

            PendingRequestDto.ClubMiniDto clubDto = null;
            if (c != null) {
                clubDto = PendingRequestDto.ClubMiniDto.builder()
                        .id(c.getId())
                        .name(c.getClubName())
                        .build();
            }

            PendingRequestDto.UserMiniDto userDto = null;
            if (u != null) {
                userDto = PendingRequestDto.UserMiniDto.builder()
                        .id(u.getId())
                        .fullName(u.getFullName())
                        .build();
            }

           PendingRequestDto dto = PendingRequestDto.builder()
                    .requestEventId(re.getId())
                    .requestTitle(re.getRequestTitle())
                    .status(re.getStatus())
                    .responseMessage(re.getResponseMessage())
                    .description(re.getDescription())
                    .requestDate(re.getRequestDate())
                    .event(eventDto)
                    .club(clubDto)
                    .createdBy(userDto)
                    .build();
            dtos.add(dto);
        }
        return dtos;
    }
}
