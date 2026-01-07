package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.request.CreateFeeRequest;
import com.sep490.backendclubmanagement.dto.request.PayOSCreatePaymentRequest;
import com.sep490.backendclubmanagement.dto.request.PayOSWebhookRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateFeeRequest;
import com.sep490.backendclubmanagement.dto.response.FeeDetailResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.dto.response.PayOSCreatePaymentResponse;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubWallet;
import com.sep490.backendclubmanagement.entity.fee.Fee;
import com.sep490.backendclubmanagement.entity.fee.FeeType;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.mapper.FeeMapper;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.service.club.club.ClubWalletService;
import com.sep490.backendclubmanagement.service.fee.FeeServiceImpl;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.service.payment.PayOSIntegrationService;
import com.sep490.backendclubmanagement.service.websocket.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho FeeServiceImpl (JUnit5 + Mockito)
 */
@ExtendWith(MockitoExtension.class)
class FeeServiceImplTest {

    @Mock
    private FeeRepository feeRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private FeeMapper feeMapper;

    @Mock
    private PayOSIntegrationService payOSIntegrationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IncomeTransactionRepository incomeTransactionRepository;

    @Mock
    private ClubWalletRepository clubWalletRepository;

    @Mock
    private PayOSPaymentRepository payOSPaymentRepository;

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private RoleMemberShipRepository roleMemberShipRepository;

    @Mock
    private ClubMemberShipRepository clubMemberShipRepository;

    @Mock
    private ClubWalletService clubWalletService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FeeServiceImpl feeService;

    @BeforeEach
    void setUp() {
        // Inject frontend URL for testing
        ReflectionTestUtils.setField(feeService, "frontendUrl", "http://localhost:5173");
    }

    // ========= Helper Methods =========


    private Long createTestOrderCode(Long feeId, Long userId, long timestamp) {
        int feeIdLength = String.valueOf(feeId).length();
        int userIdLength = String.valueOf(userId).length();
        int usedDigits = 2 + feeIdLength + userIdLength;
        int timestampDigits = 16 - usedDigits;

        long timestampModulo = (long) Math.pow(10, timestampDigits);
        long timestampTrimmed = timestamp % timestampModulo;

        String orderCodeStr = String.format("%d%d%d%d%0" + timestampDigits + "d",
                feeIdLength,
                userIdLength,
                feeId,
                userId,
                timestampTrimmed
        );

        return Long.parseLong(orderCodeStr);
    }

    private Club buildClub(Long id, String name) {
        Club club = new Club();
        club.setId(id);
        club.setClubName(name);
        club.setClubCode("CLB" + id);
        return club;
    }

    private User buildUser(Long id, String email, String fullName) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setStudentCode("SE" + id);
        user.setPhoneNumber("0123456789");
        return user;
    }

    private Semester buildSemester(Long id, String name, boolean isCurrent) {
        Semester semester = new Semester();
        semester.setId(id);
        semester.setSemesterName(name);
        semester.setIsCurrent(isCurrent);
        semester.setStartDate(LocalDate.of(2025, 1, 1));
        semester.setEndDate(LocalDate.of(2025, 6, 30));
        return semester;
    }

    private Fee buildFee(Long id, Long clubId, String title, BigDecimal amount, FeeType feeType, boolean isDraft) {
        Club club = buildClub(clubId, "Test Club");

        Fee fee = new Fee();
        fee.setId(id);
        fee.setTitle(title);
        fee.setDescription("Description for " + title);
        fee.setAmount(amount);
        fee.setFeeType(feeType);
        fee.setDueDate(LocalDate.now().plusDays(30));
        fee.setIsMandatory(true);
        fee.setIsDraft(isDraft);
        fee.setClub(club);
        fee.setCreatedAt(LocalDateTime.now());
        fee.setIncomeTransactions(new HashSet<>());
        return fee;
    }

    private FeeDetailResponse buildFeeDetailResponse(Long id, String title, BigDecimal amount) {
        FeeDetailResponse response = new FeeDetailResponse();
        response.setId(id);
        response.setTitle(title);
        response.setAmount(amount);
        response.setFeeType(FeeType.MEMBERSHIP);
        response.setIsDraft(false);
        return response;
    }

    private IncomeTransaction buildIncomeTransaction(Long id, User user, Fee fee, TransactionStatus status) {
        IncomeTransaction transaction = new IncomeTransaction();
        transaction.setId(id);
        transaction.setUser(user);
        transaction.setFee(fee);
        transaction.setAmount(fee.getAmount());
        transaction.setStatus(status);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setReference("REF" + id);
        return transaction;
    }

    // ==========================================
    // Test: getFeesByClubId
    // ==========================================

    @Test
    void getFeesByClubId_shouldReturnPageResponseWithPaidMembersInfo() {
        // Arrange
        Long clubId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Fee fee1 = buildFee(1L, clubId, "Membership Fee", new BigDecimal("100000"), FeeType.MEMBERSHIP, false);
        User user1 = buildUser(1L, "user1@fpt.edu.vn", "User 1");
        User user2 = buildUser(2L, "user2@fpt.edu.vn", "User 2");

        IncomeTransaction trans1 = buildIncomeTransaction(1L, user1, fee1, TransactionStatus.SUCCESS);
        IncomeTransaction trans2 = buildIncomeTransaction(2L, user2, fee1, TransactionStatus.SUCCESS);
        fee1.setIncomeTransactions(new HashSet<>(Arrays.asList(trans1, trans2)));

        Page<Fee> feePage = new PageImpl<>(List.of(fee1), pageable, 1);
        FeeDetailResponse feeResponse = buildFeeDetailResponse(1L, "Membership Fee", new BigDecimal("100000"));

        when(feeRepository.findByClub_Id(clubId, pageable)).thenReturn(feePage);
        when(roleMemberShipRepository.countActiveMembersInCurrentSemester(clubId)).thenReturn(10L);
        when(feeMapper.toFeeDetailResponse(fee1)).thenReturn(feeResponse);

        // Act
        PageResponse<FeeDetailResponse> result = feeService.getFeesByClubId(clubId, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(2, result.getContent().get(0).getPaidMembers()); // 2 unique users paid
        assertEquals(10, result.getContent().get(0).getTotalMembers());

        verify(feeRepository).findByClub_Id(clubId, pageable);
        verify(roleMemberShipRepository).countActiveMembersInCurrentSemester(clubId);
        verify(feeMapper).toFeeDetailResponse(fee1);
    }

    // ==========================================
    // Test: getPaidFeesByUser (List version)
    // ==========================================

    @Test
    void getPaidFeesByUser_shouldReturnListWithPaymentInfo() {
        // Arrange
        Long clubId = 1L;
        Long userId = 1L;
        User user = buildUser(userId, "user@fpt.edu.vn", "User 1");
        Fee fee = buildFee(1L, clubId, "Event Fee", new BigDecimal("50000"), FeeType.EVENT, false);
        IncomeTransaction transaction = buildIncomeTransaction(1L, user, fee, TransactionStatus.SUCCESS);
        fee.setIncomeTransactions(new HashSet<>(List.of(transaction)));

        FeeDetailResponse feeResponse = buildFeeDetailResponse(1L, "Event Fee", new BigDecimal("50000"));

        when(feeRepository.findPaidFeesByClubIdAndUserId(clubId, userId)).thenReturn(List.of(fee));
        when(feeMapper.toFeeDetailResponse(fee)).thenReturn(feeResponse);

        // Act
        List<FeeDetailResponse> result = feeService.getPaidFeesByUser(clubId, userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(transaction.getTransactionDate(), result.get(0).getPaidDate());
        assertEquals("REF1", result.get(0).getTransactionReference());

        verify(feeRepository).findPaidFeesByClubIdAndUserId(clubId, userId);
        verify(feeMapper).toFeeDetailResponse(fee);
    }

    // ==========================================
    // Test: getPaidFeesByUser (Page version)
    // ==========================================

    @Test
    void getPaidFeesByUserWithPagination_shouldReturnPageResponse() {
        // Arrange
        Long clubId = 1L;
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        User user = buildUser(userId, "user@fpt.edu.vn", "User 1");
        Fee fee = buildFee(1L, clubId, "Other Fee", new BigDecimal("30000"), FeeType.OTHER, false);
        IncomeTransaction transaction = buildIncomeTransaction(1L, user, fee, TransactionStatus.SUCCESS);
        fee.setIncomeTransactions(new HashSet<>(List.of(transaction)));

        Page<Fee> feePage = new PageImpl<>(List.of(fee), pageable, 1);
        FeeDetailResponse feeResponse = buildFeeDetailResponse(1L, "Activity Fee", new BigDecimal("30000"));

        when(feeRepository.findPaidFeesByClubIdAndUserId(clubId, userId, pageable)).thenReturn(feePage);
        when(feeMapper.toFeeDetailResponse(fee)).thenReturn(feeResponse);

        // Act
        PageResponse<FeeDetailResponse> result = feeService.getPaidFeesByUser(clubId, userId, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(transaction.getTransactionDate(), result.getContent().get(0).getPaidDate());

        verify(feeRepository).findPaidFeesByClubIdAndUserId(clubId, userId, pageable);
    }

    // ==========================================
    // Test: createFee
    // ==========================================

    @Test
    void createFee_shouldCreateDraftFeeSuccessfully() throws AppException {
        // Arrange
        Long clubId = 1L;
        Club club = buildClub(clubId, "Test Club");

        CreateFeeRequest request = new CreateFeeRequest();
        request.setTitle("New Fee");
        request.setDescription("Test Description");
        request.setAmount(new BigDecimal("100000"));
        request.setFeeType(FeeType.MEMBERSHIP);
        request.setDueDate(LocalDate.now().plusDays(30));
        request.setIsMandatory(true);
        request.setIsDraft(true);


        Fee savedFee = buildFee(1L, clubId, "New Fee", new BigDecimal("100000"), FeeType.MEMBERSHIP, true);
        FeeDetailResponse response = buildFeeDetailResponse(1L, "New Fee", new BigDecimal("100000"));
        response.setIsDraft(true);

        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(feeRepository.save(any(Fee.class))).thenReturn(savedFee);
        when(feeMapper.toFeeDetailResponse(savedFee)).thenReturn(response);

        // Act
        FeeDetailResponse result = feeService.createFee(clubId, request);

        // Assert
        assertNotNull(result);
        assertEquals("New Fee", result.getTitle());
        assertTrue(result.getIsDraft());

        verify(clubRepository).findById(clubId);
        verify(feeRepository).save(any(Fee.class));
        verify(feeMapper).toFeeDetailResponse(savedFee);
    }

    @Test
    void createFee_withSemester_shouldLinkSemesterForMembershipFee() throws AppException {
        // Arrange
        Long clubId = 1L;
        Long semesterId = 10L;
        Club club = buildClub(clubId, "Test Club");
        Semester semester = buildSemester(semesterId, "Spring 2025", true);

        CreateFeeRequest request = new CreateFeeRequest();
        request.setTitle("Semester Fee");
        request.setAmount(new BigDecimal("200000"));
        request.setFeeType(FeeType.MEMBERSHIP);
        request.setSemesterId(semesterId);
        request.setIsDraft(false);

        Fee savedFee = buildFee(1L, clubId, "Semester Fee", new BigDecimal("200000"), FeeType.MEMBERSHIP, false);
        savedFee.setSemester(semester);
        FeeDetailResponse response = buildFeeDetailResponse(1L, "Semester Fee", new BigDecimal("200000"));

        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(semesterRepository.findById(semesterId)).thenReturn(Optional.of(semester));
        when(feeRepository.save(any(Fee.class))).thenReturn(savedFee);
        when(feeMapper.toFeeDetailResponse(savedFee)).thenReturn(response);

        // Act
        FeeDetailResponse result = feeService.createFee(clubId, request);

        // Assert
        assertNotNull(result);
        verify(semesterRepository).findById(semesterId);
    }

    @Test
    void createFee_whenClubNotFound_shouldThrowException() {
        // Arrange
        Long clubId = 999L;
        CreateFeeRequest request = new CreateFeeRequest();
        request.setTitle("Test Fee");
        request.setAmount(new BigDecimal("50000"));
        request.setFeeType(FeeType.OTHER);

        when(clubRepository.findById(clubId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class,
            () -> feeService.createFee(clubId, request));

        assertEquals(ErrorCode.CLUB_NOT_FOUND, exception.getErrorCode());
        verify(clubRepository).findById(clubId);
        verify(feeRepository, never()).save(any());
    }

    // ==========================================
    // Test: isFeeTitleExists
    // ==========================================

    @Test
    void isFeeTitleExists_whenTitleExists_shouldReturnTrue() {
        // Arrange
        Long clubId = 1L;
        String title = "Existing Fee";

        when(feeRepository.existsByTitleIgnoreCaseAndClub_IdAndIsDraftFalse(title, clubId)).thenReturn(true);

        // Act
        boolean result = feeService.isFeeTitleExists(clubId, title);

        // Assert
        assertTrue(result);
        verify(feeRepository).existsByTitleIgnoreCaseAndClub_IdAndIsDraftFalse(title, clubId);
    }

    @Test
    void isFeeTitleExists_whenTitleNotExists_shouldReturnFalse() {
        // Arrange
        Long clubId = 1L;
        String title = "New Fee";

        when(feeRepository.existsByTitleIgnoreCaseAndClub_IdAndIsDraftFalse(title, clubId)).thenReturn(false);

        // Act
        boolean result = feeService.isFeeTitleExists(clubId, title);

        // Assert
        assertFalse(result);
    }

    // ==========================================
    // Test: isFeeTitleExistsExcluding
    // ==========================================

    @Test
    void isFeeTitleExistsExcluding_whenTitleExistsInOtherFee_shouldReturnTrue() {
        // Arrange
        Long clubId = 1L;
        String title = "Duplicate Fee";
        Long excludeFeeId = 5L;

        when(feeRepository.existsByTitleIgnoreCaseAndClub_IdAndIsDraftFalseAndIdNot(title, clubId, excludeFeeId))
            .thenReturn(true);

        // Act
        boolean result = feeService.isFeeTitleExistsExcluding(clubId, title, excludeFeeId);

        // Assert
        assertTrue(result);
    }

    // ==========================================
    // Test: updateFee
    // ==========================================

    @Test
    void updateFee_shouldUpdateSuccessfully() throws AppException {
        // Arrange
        Long feeId = 1L;
        Long clubId = 1L;
        Fee existingFee = buildFee(feeId, clubId, "Old Title", new BigDecimal("100000"), FeeType.MEMBERSHIP, false);
        existingFee.setHasEverExpired(false);

        UpdateFeeRequest request = new UpdateFeeRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated Description");
        request.setAmount(new BigDecimal("150000"));
        request.setFeeType(FeeType.MEMBERSHIP);
        request.setDueDate(LocalDate.now().plusDays(60));
        request.setIsMandatory(true);

        Fee updatedFee = buildFee(feeId, clubId, "Updated Title", new BigDecimal("150000"), FeeType.MEMBERSHIP, false);
        FeeDetailResponse response = buildFeeDetailResponse(feeId, "Updated Title", new BigDecimal("150000"));

        when(feeRepository.findById(feeId)).thenReturn(Optional.of(existingFee));
        when(feeRepository.existsByTitleIgnoreCaseAndClub_IdAndIsDraftFalseAndIdNot(
            request.getTitle(), clubId, feeId)).thenReturn(false);
        when(feeRepository.save(any(Fee.class))).thenReturn(updatedFee);
        when(feeMapper.toFeeDetailResponse(updatedFee)).thenReturn(response);

        // Act
        FeeDetailResponse result = feeService.updateFee(feeId, request);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals(new BigDecimal("150000"), result.getAmount());

        verify(feeRepository).findById(feeId);
        verify(feeRepository).save(any(Fee.class));
    }

    @Test
    void updateFee_whenFeeExpiredAndAmountChanged_shouldThrowException() {
        // Arrange
        Long feeId = 1L;
        Long clubId = 1L;
        Fee expiredFee = buildFee(feeId, clubId, "Expired Fee", new BigDecimal("100000"), FeeType.MEMBERSHIP, false);
        expiredFee.setHasEverExpired(true);

        UpdateFeeRequest request = new UpdateFeeRequest();
        request.setTitle("Expired Fee");
        request.setAmount(new BigDecimal("200000")); // Different amount
        request.setFeeType(FeeType.MEMBERSHIP);
        request.setIsMandatory(true);

        when(feeRepository.findById(feeId)).thenReturn(Optional.of(expiredFee));

        // Act & Assert
        AppException exception = assertThrows(AppException.class,
            () -> feeService.updateFee(feeId, request));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("hết hạn"));
        verify(feeRepository, never()).save(any());
    }

    @Test
    void updateFee_whenTitleDuplicate_shouldThrowException() {
        // Arrange
        Long feeId = 1L;
        Long clubId = 1L;
        Fee existingFee = buildFee(feeId, clubId, "Original Title", new BigDecimal("100000"), FeeType.MEMBERSHIP, false);

        UpdateFeeRequest request = new UpdateFeeRequest();
        request.setTitle("Duplicate Title");
        request.setAmount(new BigDecimal("100000"));
        request.setFeeType(FeeType.MEMBERSHIP);

        when(feeRepository.findById(feeId)).thenReturn(Optional.of(existingFee));
        when(feeRepository.existsByTitleIgnoreCaseAndClub_IdAndIsDraftFalseAndIdNot(
            "Duplicate Title", clubId, feeId)).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class,
            () -> feeService.updateFee(feeId, request));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("đã tồn tại"));
    }

    // ==========================================
    // Test: getDraftFeesByClubId
    // ==========================================

    @Test
    void getDraftFeesByClubId_shouldReturnOnlyDraftFees() {
        // Arrange
        Long clubId = 1L;
        Fee draftFee1 = buildFee(1L, clubId, "Draft Fee 1", new BigDecimal("50000"), FeeType.OTHER, true);
        Fee draftFee2 = buildFee(2L, clubId, "Draft Fee 2", new BigDecimal("60000"), FeeType.EVENT, true);

        FeeDetailResponse response1 = buildFeeDetailResponse(1L, "Draft Fee 1", new BigDecimal("50000"));
        response1.setIsDraft(true);
        FeeDetailResponse response2 = buildFeeDetailResponse(2L, "Draft Fee 2", new BigDecimal("60000"));
        response2.setIsDraft(true);

        when(feeRepository.findByClub_IdAndIsDraftTrue(clubId)).thenReturn(Arrays.asList(draftFee1, draftFee2));
        when(feeMapper.toFeeDetailResponse(draftFee1)).thenReturn(response1);
        when(feeMapper.toFeeDetailResponse(draftFee2)).thenReturn(response2);

        // Act
        List<FeeDetailResponse> result = feeService.getDraftFeesByClubId(clubId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0).getIsDraft());
        assertTrue(result.get(1).getIsDraft());

        verify(feeRepository).findByClub_IdAndIsDraftTrue(clubId);
    }

    // ==========================================
    // Test: publishFee
    // ==========================================

    @Test
    void publishFee_shouldPublishFeeAndSendNotifications() throws AppException {
        // Arrange
        Long feeId = 1L;
        Long clubId = 1L;
        Fee draftFee = buildFee(feeId, clubId, "Draft Fee", new BigDecimal("100000"), FeeType.MEMBERSHIP, true);
        Fee publishedFee = buildFee(feeId, clubId, "Draft Fee", new BigDecimal("100000"), FeeType.MEMBERSHIP, false);
        FeeDetailResponse response = buildFeeDetailResponse(feeId, "Draft Fee", new BigDecimal("100000"));

        List<Long> activeMemberIds = Arrays.asList(1L, 2L, 3L);

        when(feeRepository.findById(feeId)).thenReturn(Optional.of(draftFee));
        when(feeRepository.save(any(Fee.class))).thenReturn(publishedFee);
        when(feeMapper.toFeeDetailResponse(any(Fee.class))).thenReturn(response);
        when(roleMemberShipRepository.findActiveMemberUserIdsByClubId(clubId)).thenReturn(activeMemberIds);
        doNothing().when(notificationService).sendToUsers(anyList(), any(), anyString(), anyString(),
            any(), any(), anyString(), any(), any(), any(), any());

        // Act
        FeeDetailResponse result = feeService.publishFee(feeId);

        // Assert
        assertNotNull(result);
        assertFalse(result.getIsDraft());

        verify(feeRepository).findById(feeId);
        verify(feeRepository).save(argThat(fee -> !fee.getIsDraft()));
        verify(roleMemberShipRepository).findActiveMemberUserIdsByClubId(clubId);
        verify(notificationService).sendToUsers(
            eq(activeMemberIds),
            isNull(),
            contains("Khoản phí mới"),
            anyString(),
            eq(NotificationType.FEE_PUBLISHED),
            eq(NotificationPriority.HIGH),
            anyString(),
            eq(clubId),
            isNull(),
            isNull(),
            isNull()
        );
    }

    @Test
    void publishFee_whenFeeNotFound_shouldThrowException() {
        // Arrange
        Long feeId = 999L;

        when(feeRepository.findById(feeId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class,
            () -> feeService.publishFee(feeId));

        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
        verify(feeRepository, never()).save(any());
    }

    // ==========================================
    // Test: deleteFee
    // ==========================================

    @Test
    void deleteFee_whenNoPayments_shouldDeleteSuccessfully() throws AppException {
        // Arrange
        Long feeId = 1L;
        Long clubId = 1L;
        Fee fee = buildFee(feeId, clubId, "Test Fee", new BigDecimal("50000"), FeeType.OTHER, true);
        fee.setIncomeTransactions(Collections.emptySet());

        when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));
        doNothing().when(feeRepository).delete(fee);

        // Act
        feeService.deleteFee(feeId);

        // Assert
        verify(feeRepository).findById(feeId);
        verify(feeRepository).delete(fee);
    }

    @Test
    void deleteFee_whenHasPayments_shouldThrowException() {
        // Arrange
        Long feeId = 1L;
        Long clubId = 1L;
        Fee fee = buildFee(feeId, clubId, "Paid Fee", new BigDecimal("100000"), FeeType.MEMBERSHIP, false);
        User user = buildUser(1L, "user@fpt.edu.vn", "User 1");
        IncomeTransaction transaction = buildIncomeTransaction(1L, user, fee, TransactionStatus.SUCCESS);
        fee.setIncomeTransactions(new HashSet<>(List.of(transaction)));

        when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));

        // Act & Assert
        AppException exception = assertThrows(AppException.class,
            () -> feeService.deleteFee(feeId));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("thành viên đóng phí"));
        verify(feeRepository, never()).delete(any());
    }

    // ==========================================
    // Test: generatePaymentQR
    // ==========================================

    @Test
    void generatePaymentQR_shouldGenerateQRSuccessfully() throws AppException {
        // Arrange
        Long clubId = 1L;
        Long feeId = 1L;
        Long userId = 1L;

        Club club = buildClub(clubId, "Test Club");
        User user = buildUser(userId, "user@fpt.edu.vn", "User 1");
        Fee fee = buildFee(feeId, clubId, "Payment Fee", new BigDecimal("100000"), FeeType.MEMBERSHIP, false);

        PayOSCreatePaymentResponse paymentResponse = new PayOSCreatePaymentResponse();
        paymentResponse.setPaymentLink("https://pay.payos.vn/checkout/xxx");
        paymentResponse.setQrCode("data:image/png;base64,xxx");

        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(payOSIntegrationService.createPaymentRequest(eq(clubId), any(PayOSCreatePaymentRequest.class)))
            .thenReturn(paymentResponse);

        // Act
        PayOSCreatePaymentResponse result = feeService.generatePaymentQR(clubId, feeId, userId);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getPaymentLink());

        verify(clubRepository).findById(clubId);
        verify(feeRepository).findById(feeId);
        verify(userRepository).findById(userId);
        verify(payOSIntegrationService).createPaymentRequest(eq(clubId), any(PayOSCreatePaymentRequest.class));
    }

    @Test
    void generatePaymentQR_whenFeeIsDraft_shouldThrowException() throws AppException {
        // Arrange
        Long clubId = 1L;
        Long feeId = 1L;
        Long userId = 1L;

        Club club = buildClub(clubId, "Test Club");
        Fee draftFee = buildFee(feeId, clubId, "Draft Fee", new BigDecimal("100000"), FeeType.MEMBERSHIP, true);

        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(feeRepository.findById(feeId)).thenReturn(Optional.of(draftFee));

        // Act & Assert
        AppException exception = assertThrows(AppException.class,
            () -> feeService.generatePaymentQR(clubId, feeId, userId));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("bản nháp"));
        verify(payOSIntegrationService, never()).createPaymentRequest(any(), any());
    }

    @Test
    void generatePaymentQR_whenClubNotFound_shouldThrowException() {
        // Arrange
        Long clubId = 999L;
        Long feeId = 1L;
        Long userId = 1L;

        when(clubRepository.findById(clubId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class,
            () -> feeService.generatePaymentQR(clubId, feeId, userId));

        assertEquals(ErrorCode.CLUB_NOT_FOUND, exception.getErrorCode());
    }

    // ==========================================
    // Test: getUnpaidFeesByUser
    // ==========================================

    @Test
    void getUnpaidFeesByUser_shouldReturnUnpaidFeesOnly() {
        // Arrange
        Long clubId = 1L;
        Long userId = 1L;

        Fee unpaidFee1 = buildFee(1L, clubId, "Unpaid Fee 1", new BigDecimal("50000"), FeeType.OTHER, false);
        Fee unpaidFee2 = buildFee(2L, clubId, "Unpaid Fee 2", new BigDecimal("60000"), FeeType.EVENT, false);

        FeeDetailResponse response1 = buildFeeDetailResponse(1L, "Unpaid Fee 1", new BigDecimal("50000"));
        FeeDetailResponse response2 = buildFeeDetailResponse(2L, "Unpaid Fee 2", new BigDecimal("60000"));

        when(feeRepository.findUnpaidFeesByClubIdAndUserId(clubId, userId))
            .thenReturn(Arrays.asList(unpaidFee1, unpaidFee2));
        when(feeMapper.toFeeDetailResponse(unpaidFee1)).thenReturn(response1);
        when(feeMapper.toFeeDetailResponse(unpaidFee2)).thenReturn(response2);

        // Act
        List<FeeDetailResponse> result = feeService.getUnpaidFeesByUser(clubId, userId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Unpaid Fee 1", result.get(0).getTitle());
        assertEquals("Unpaid Fee 2", result.get(1).getTitle());

        verify(feeRepository).findUnpaidFeesByClubIdAndUserId(clubId, userId);
    }

    // ==========================================
    // Test: OrderCode Algorithm (createOrderCode & parseOrderCode)
    // ==========================================

    @Test
    void orderCodeAlgorithm_shouldEncodeAndDecodeCorrectly() throws AppException {
        // Arrange
        Long clubId = 1L;
        Long feeId = 123L; // 3 digits
        Long userId = 45L;  // 2 digits

        Club club = buildClub(clubId, "Test Club");
        Fee fee = buildFee(feeId, clubId, "Test Fee", new BigDecimal("100000"), FeeType.MEMBERSHIP, false);
        User user = buildUser(userId, "user@fpt.edu.vn", "User");

        when(clubRepository.findById(clubId)).thenReturn(Optional.of(club));
        when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(payOSIntegrationService.createPaymentRequest(eq(clubId), any(PayOSCreatePaymentRequest.class)))
            .thenReturn(new PayOSCreatePaymentResponse());

        // Act - Generate orderCode
        PayOSCreatePaymentResponse response = feeService.generatePaymentQR(clubId, feeId, userId);

        // Assert - The orderCode should follow format:
        // Digit 1: feeId length (3)
        // Digit 2: userId length (2)
        // Digits 3-5: feeId (123)
        // Digits 6-7: userId (45)
        // Remaining: timestamp

        // Verify that the service was called (orderCode is generated internally)
        verify(payOSIntegrationService).createPaymentRequest(eq(clubId), argThat(request -> {
            Long orderCode = request.getOrderCode();
            String orderCodeStr = String.valueOf(orderCode);

            // Check format: should start with "3" (feeId length) + "2" (userId length) + "123" (feeId) + "45" (userId)
            return orderCodeStr.startsWith("32123") && orderCodeStr.length() >= 5 && orderCodeStr.length() <= 16;
        }));
    }

    @Test
    void parseOrderCode_withValidFormat_shouldExtractFeeIdAndUserId() throws Exception {
        // Arrange - Use reflection to access private method
        java.lang.reflect.Method parseMethod = FeeServiceImpl.class.getDeclaredMethod("parseOrderCode", Long.class);
        parseMethod.setAccessible(true);

        // OrderCode format breakdown:
        // Position 0: feeIdLen = 3
        // Position 1: userIdLen = 2
        // Create orderCode using helper: feeId=123 (3 digits), userId=45 (2 digits)
        // Format: "3" + "2" + "123" + "45" + timestamp (9 digits to fill to 16 total)
        Long orderCode = createTestOrderCode(123L, 45L, 123456789L);

        // Act
        Long[] result = (Long[]) parseMethod.invoke(feeService, orderCode);

        // Assert
        assertEquals(2, result.length);
        assertEquals(123L, result[0]); // feeId
        assertEquals(45L, result[1]);  // userId
    }

    @Test
    void parseOrderCode_withSingleDigitIds_shouldWork() throws Exception {
        // Arrange
        java.lang.reflect.Method parseMethod = FeeServiceImpl.class.getDeclaredMethod("parseOrderCode", Long.class);
        parseMethod.setAccessible(true);

        // Create orderCode: feeId=5 (1 digit), userId=9 (1 digit)
        // Format: "1" + "1" + "5" + "9" + timestamp (12 digits to fill to 16 total)
        Long orderCode = createTestOrderCode(5L, 9L, 123456789012L);

        // Act
        Long[] result = (Long[]) parseMethod.invoke(feeService, orderCode);

        // Assert
        assertEquals(5L, result[0]); // feeId
        assertEquals(9L, result[1]);  // userId
    }

    @Test
    void parseOrderCode_withInvalidLength_shouldThrowException() throws Exception {
        // Arrange
        java.lang.reflect.Method parseMethod = FeeServiceImpl.class.getDeclaredMethod("parseOrderCode", Long.class);
        parseMethod.setAccessible(true);

        Long invalidOrderCode = 123L; // Too short (< 5 digits)

        // Act & Assert
        try {
            parseMethod.invoke(feeService, invalidOrderCode);
            fail("Expected AppException to be thrown");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof AppException);
            AppException appException = (AppException) e.getCause();
            assertEquals(ErrorCode.VALIDATION_ERROR, appException.getErrorCode());
            assertTrue(appException.getMessage().contains("OrderCode không hợp lệ"));
        }
    }

    @Test
    void parseOrderCode_withTooShortString_shouldThrowException() throws Exception {
        // Arrange
        java.lang.reflect.Method parseMethod = FeeServiceImpl.class.getDeclaredMethod("parseOrderCode", Long.class);
        parseMethod.setAccessible(true);

        // OrderCode: "3211" - feeIdLen=3, userIdLen=2, but string is only 4 chars (need at least 2+3+2=7)
        Long invalidOrderCode = 3211L;

        // Act & Assert
        try {
            parseMethod.invoke(feeService, invalidOrderCode);
            fail("Expected AppException to be thrown");
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Verify AppException was thrown
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof AppException);
            AppException appException = (AppException) e.getCause();
            assertEquals(ErrorCode.VALIDATION_ERROR, appException.getErrorCode());
            // Message should indicate invalid orderCode
            assertTrue(appException.getMessage().contains("OrderCode"));
        }
    }

    // ==========================================
    // Test: handlePaymentWebhook
    // ==========================================

    @Test
    void handlePaymentWebhook_shouldProcessPaymentSuccessfully() throws AppException {
        // Arrange
        Long feeId = 100L;
        Long userId = 1L;
        // OrderCode using createTestOrderCode algorithm: feeId=100(3), userId=1(1), timestamp trimmed to 10 digits
        // Format: "3" + "1" + "100" + "1" + "1234567890" = "3110011234567890" (16 digits)
        Long orderCode = createTestOrderCode(feeId, userId, 1234567890L);

        PayOSWebhookRequest.Data webhookData = new PayOSWebhookRequest.Data();
        webhookData.setOrderCode(orderCode);
        webhookData.setTransactionCode("TXN12345");
        webhookData.setDescription("Payment for fee");
        webhookData.setAccountNumber("0123456789");
        webhookData.setTransactionDateTime("2025-11-22T10:00:00");
        webhookData.setPaymentLinkId("LINK123");
        webhookData.setPaymentMethod("BANK_TRANSFER");

        PayOSWebhookRequest webhookRequest = new PayOSWebhookRequest();
        webhookRequest.setCode("00");
        webhookRequest.setDesc("success");
        webhookRequest.setData(webhookData);

        Club club = buildClub(1L, "Test Club");
        Fee fee = buildFee(feeId, 1L, "Test Fee", new BigDecimal("100000"), FeeType.MEMBERSHIP, false);
        fee.setClub(club);
        User user = buildUser(userId, "user@fpt.edu.vn", "User 1");
        ClubWallet wallet = new ClubWallet();
        wallet.setId(1L);
        wallet.setClub(club);
        wallet.setBalance(BigDecimal.ZERO);

        when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(incomeTransactionRepository.existsByReference(anyString())).thenReturn(false);
        when(incomeTransactionRepository.existsByUser_IdAndFee_IdAndStatus(userId, feeId, TransactionStatus.SUCCESS))
            .thenReturn(false);
        when(clubWalletService.getOrCreateWalletForClub(1L)).thenReturn(wallet);
        when(payOSPaymentRepository.save(any(PayOSPayment.class))).thenAnswer(i -> i.getArgument(0));
        when(incomeTransactionRepository.save(any(IncomeTransaction.class))).thenAnswer(i -> i.getArgument(0));
        doNothing().when(clubWalletService).processIncomeTransaction(any(), any());
        doNothing().when(webSocketService).sendPaymentSuccess(anyString(), any());
        doNothing().when(notificationService).sendToUser(anyLong(), any(), anyString(), anyString(),
            any(), any(), anyString(), any(), any(), any(), any(), any());

        // Act
        feeService.handlePaymentWebhook(webhookRequest);

        // Assert
        verify(feeRepository).findById(feeId);
        verify(userRepository).findById(userId);
        verify(incomeTransactionRepository).save(any(IncomeTransaction.class));
        verify(clubWalletService).processIncomeTransaction(any(), any());
        verify(webSocketService).sendPaymentSuccess(eq(user.getEmail()), any());
        verify(notificationService).sendToUser(
            eq(userId),
            isNull(),
            eq("Thanh toán thành công"),
            anyString(),
            eq(NotificationType.PAYMENT_SUCCESS),
            eq(NotificationPriority.HIGH),
            anyString(),
            eq(1L),
            isNull(),
            isNull(),
            isNull(),
            isNull()
        );
    }

    @Test
    void handlePaymentWebhook_whenDuplicateTransaction_shouldSkipProcessing() throws AppException {
        // Arrange
        Long feeId = 100L;
        Long userId = 1L;
        Long orderCode = createTestOrderCode(feeId, userId, 1234567890L);
        String reference = String.valueOf(orderCode);

        // Mock fee and user (needed for parsing orderCode)
        Club club = buildClub(1L, "Test Club");
        Fee fee = buildFee(feeId, 1L, "Test Fee", new BigDecimal("100000"), FeeType.MEMBERSHIP, false);
        fee.setClub(club);
        User user = buildUser(userId, "user@fpt.edu.vn", "User 1");

        PayOSWebhookRequest.Data webhookData = new PayOSWebhookRequest.Data();
        webhookData.setOrderCode(orderCode);
        webhookData.setTransactionDateTime("2025-11-22T10:00:00");

        PayOSWebhookRequest webhookRequest = new PayOSWebhookRequest();
        webhookRequest.setCode("00");
        webhookRequest.setDesc("success");
        webhookRequest.setData(webhookData);

        when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(incomeTransactionRepository.existsByReference(reference)).thenReturn(true);

        // Act
        feeService.handlePaymentWebhook(webhookRequest);

        // Assert
        verify(incomeTransactionRepository).existsByReference(reference);
        verify(incomeTransactionRepository, never()).save(any());
        verify(webSocketService, never()).sendPaymentSuccess(anyString(), any());
    }

    @Test
    void handlePaymentWebhook_whenUserAlreadyPaid_shouldSkipProcessing() throws AppException {
        // Arrange
        Long feeId = 100L;
        Long userId = 1L;
        Long orderCode = createTestOrderCode(feeId, userId, 1234567890L);

        PayOSWebhookRequest.Data webhookData = new PayOSWebhookRequest.Data();
        webhookData.setOrderCode(orderCode);
        webhookData.setTransactionDateTime("2025-11-22T10:00:00");

        PayOSWebhookRequest webhookRequest = new PayOSWebhookRequest();
        webhookRequest.setCode("00");
        webhookRequest.setDesc("success");
        webhookRequest.setData(webhookData);

        Club club = buildClub(1L, "Test Club");
        Fee fee = buildFee(feeId, 1L, "Test Fee", new BigDecimal("100000"), FeeType.MEMBERSHIP, false);
        fee.setClub(club);
        User user = buildUser(userId, "user@fpt.edu.vn", "User 1");

        when(feeRepository.findById(feeId)).thenReturn(Optional.of(fee));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(incomeTransactionRepository.existsByReference(anyString())).thenReturn(false);
        when(incomeTransactionRepository.existsByUser_IdAndFee_IdAndStatus(userId, feeId, TransactionStatus.SUCCESS))
            .thenReturn(true);

        // Act
        feeService.handlePaymentWebhook(webhookRequest);

        // Assert
        verify(incomeTransactionRepository).existsByUser_IdAndFee_IdAndStatus(userId, feeId, TransactionStatus.SUCCESS);
        verify(incomeTransactionRepository, never()).save(any());
    }

    @Test
    void handlePaymentWebhook_whenPaymentNotSuccess_shouldSkipProcessing() throws AppException {
        // Arrange
        Long orderCode = createTestOrderCode(100L, 1L, 1234567890L);
        PayOSWebhookRequest.Data webhookData = new PayOSWebhookRequest.Data();
        webhookData.setOrderCode(orderCode);
        webhookData.setTransactionDateTime("2025-11-22T10:00:00");

        PayOSWebhookRequest webhookRequest = new PayOSWebhookRequest();
        webhookRequest.setCode("01");
        webhookRequest.setDesc("failed"); // Not success
        webhookRequest.setData(webhookData);

        Club club = buildClub(1L, "Test Club");
        Fee fee = buildFee(100L, 1L, "Test Fee", new BigDecimal("100000"), FeeType.MEMBERSHIP, false);
        fee.setClub(club);
        User user = buildUser(1L, "user@fpt.edu.vn", "User 1");

        when(feeRepository.findById(100L)).thenReturn(Optional.of(fee));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(incomeTransactionRepository.existsByReference(anyString())).thenReturn(false);
        when(incomeTransactionRepository.existsByUser_IdAndFee_IdAndStatus(any(), any(), any())).thenReturn(false);

        // Act
        feeService.handlePaymentWebhook(webhookRequest);

        // Assert
        verify(incomeTransactionRepository, never()).save(any());
    }
}

