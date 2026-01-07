package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.controller.EventController;
import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.CreateEventRequest;
import com.sep490.backendclubmanagement.dto.response.EventData;
import com.sep490.backendclubmanagement.exception.ForbiddenException;
import com.sep490.backendclubmanagement.service.event.EventManagementService;
import com.sep490.backendclubmanagement.service.event.EventService;
import com.sep490.backendclubmanagement.service.role.RoleService;
import com.sep490.backendclubmanagement.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Controller tests focus on role checks + delegation.
 */
@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @Mock
    private EventManagementService eventManagementService;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private EventController eventController;

    @Test
    void getEventsByClubId_shouldPassCurrentUserToService() {
        try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(5L);
            List<EventData> events = List.of(EventData.builder().id(1L).title("Test").build());
            when(eventService.getEventsByClubId(10L, 5L, null, null)).thenReturn(events);

            ApiResponse<List<EventData>> response = eventController.getEventsByClubId(10L, null, null);

            assertNotNull(response);
            assertEquals(events, response.getData());
            verify(eventService).getEventsByClubId(10L, 5L, null, null);
        }
    }

    @Test
    void getStaffAllEvents_whenStaff_shouldReturnData() {
        try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(roleService.isStaff(1L)).thenReturn(true);
            when(eventService.getStaffAllEvents("start", "end")).thenReturn(List.of());

            ApiResponse<List<EventData>> response = eventController.getStaffAllEvents("start", "end");

            assertNotNull(response);
            verify(eventService).getStaffAllEvents("start", "end");
        }
    }

    @Test
    void getStaffAllEvents_whenNotStaff_shouldThrowForbidden() {
        try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(2L);
            when(roleService.isStaff(2L)).thenReturn(false);

            assertThrows(ForbiddenException.class,
                    () -> eventController.getStaffAllEvents(null, null));
        }
    }

    @Test
    void getStaffEventsByClubId_whenStaff_shouldCallService() {
        try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(3L);
            when(roleService.isStaff(3L)).thenReturn(true);
            when(eventService.getStaffEventsByClubId(99L, null, null)).thenReturn(List.of());

            ApiResponse<List<EventData>> response = eventController.getStaffEventsByClubId(99L, null, null);

            assertNotNull(response);
            verify(eventService).getStaffEventsByClubId(99L, null, null);
        }
    }

    @Test
    void getStaffEventsByClubId_whenNotStaff_shouldThrowForbidden() {
        try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(4L);
            when(roleService.isStaff(4L)).thenReturn(false);

            assertThrows(ForbiddenException.class,
                    () -> eventController.getStaffEventsByClubId(11L, null, null));
        }
    }

    @Test
    void createEvent_shouldPassRequestToService() {
        try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(7L);
            CreateEventRequest req = new CreateEventRequest();
            EventData dto = EventData.builder().id(123L).title("Created").build();
            when(eventManagementService.createEvent(eq(req), eq(7L))).thenReturn(dto);

            ApiResponse<EventData> response = eventController.createEvent(req);

            assertNotNull(response);
            assertEquals(dto, response.getData());
        }
    }

    @Test
    void publishEventByStaff_shouldInvokeService() {
        try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(8L);
            EventData dto = EventData.builder().id(1L).title("Published").build();
            when(eventManagementService.publishEventByStaff(50L, 8L)).thenReturn(dto);

            ApiResponse<EventData> response = eventController.publishEventByStaff(50L);

            assertEquals(dto, response.getData());
            verify(eventManagementService).publishEventByStaff(50L, 8L);
        }
    }

    @Test
    void registerForEvent_shouldCallServiceWithCurrentUser() {
        try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(9L);
            doNothing().when(eventService).registerForEvent(77L, 9L);

            ApiResponse<Void> response = eventController.registerForEvent(77L);

            assertNotNull(response);
            verify(eventService).registerForEvent(77L, 9L);
        }
    }
}




