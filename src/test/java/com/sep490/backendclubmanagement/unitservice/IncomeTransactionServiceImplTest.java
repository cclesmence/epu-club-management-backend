package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.request.CreateIncomeTransactionRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateIncomeTransactionRequest;
import com.sep490.backendclubmanagement.dto.response.IncomeTransactionResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubWallet;
import com.sep490.backendclubmanagement.entity.fee.Fee;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.mapper.IncomeTransactionMapper;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.service.club.club.ClubWalletService;
import com.sep490.backendclubmanagement.service.transaction.income.IncomeTransactionServiceImpl;
import com.sep490.backendclubmanagement.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho IncomeTransactionServiceImpl (JUnit5 + Mockito)
 */
@ExtendWith(MockitoExtension.class)
class IncomeTransactionServiceImplTest {

    @Mock
    private IncomeTransactionRepository incomeTransactionRepository;

    @Mock
    private ClubWalletRepository clubWalletRepository;

    @Mock
    private FeeRepository feeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IncomeTransactionMapper incomeTransactionMapper;

    @Mock
    private UserService userService;

    @Mock
    private RoleMemberShipRepository roleMemberShipRepository;

    @Mock
    private ClubWalletService clubWalletService;

    @InjectMocks
    private IncomeTransactionServiceImpl incomeTransactionService;

    private Club club;
    private ClubWallet clubWallet;
    private User user;
    private User createdBy;
    private Fee fee;
    private IncomeTransaction incomeTransaction;
    private IncomeTransactionResponse incomeTransactionResponse;

    @BeforeEach
    void setUp() {
        club = new Club();
        club.setId(1L);
        club.setClubName("Test Club");

        clubWallet = new ClubWallet();
        clubWallet.setId(100L);
        clubWallet.setClub(club);
        clubWallet.setBalance(BigDecimal.valueOf(1000000));

        user = new User();
        user.setId(10L);
        user.setFullName("Nguyễn Văn A");
        user.setEmail("a@example.com");

        createdBy = new User();
        createdBy.setId(20L);
        createdBy.setFullName("Nguyễn Văn B");
        createdBy.setEmail("b@example.com");

        fee = new Fee();
        fee.setId(50L);
        fee.setTitle("Phí thành viên");
        fee.setAmount(BigDecimal.valueOf(100000));

        incomeTransaction = new IncomeTransaction();
        incomeTransaction.setId(1L);
        incomeTransaction.setReference("INC-20240101-ABC123");
        incomeTransaction.setAmount(BigDecimal.valueOf(100000));
        incomeTransaction.setDescription("Payment for member fee");
        incomeTransaction.setTransactionDate(LocalDateTime.now());
        incomeTransaction.setSource("fee");
        incomeTransaction.setStatus(TransactionStatus.PENDING);
        incomeTransaction.setClubWallet(clubWallet);
        incomeTransaction.setUser(user);
        incomeTransaction.setFee(fee);
        incomeTransaction.setCreatedBy(createdBy);

        incomeTransactionResponse = new IncomeTransactionResponse();
        incomeTransactionResponse.setId(1L);
        incomeTransactionResponse.setReference("INC-20240101-ABC123");
        incomeTransactionResponse.setAmount(BigDecimal.valueOf(100000));
        incomeTransactionResponse.setStatus(TransactionStatus.PENDING);
    }

    // ========= Test getIncomeTransactions =========

    @Test
    void getIncomeTransactions_success() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<IncomeTransaction> page = new PageImpl<>(List.of(incomeTransaction), pageable, 1);

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(incomeTransactionRepository.findByClubWalletId(clubWallet.getId(), pageable)).thenReturn(page);
        when(incomeTransactionMapper.toResponse(incomeTransaction)).thenReturn(incomeTransactionResponse);

        // Act
        PageResponse<IncomeTransactionResponse> result = incomeTransactionService.getIncomeTransactions(club.getId(), pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(incomeTransactionResponse, result.getContent().get(0));

        verify(clubWalletService).getOrCreateWalletForClub(club.getId());
        verify(incomeTransactionRepository).findByClubWalletId(clubWallet.getId(), pageable);
        verify(incomeTransactionMapper).toResponse(incomeTransaction);
    }

    @Test
    void getIncomeTransactions_emptyList() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<IncomeTransaction> page = new PageImpl<>(List.of(), pageable, 0);

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(incomeTransactionRepository.findByClubWalletId(clubWallet.getId(), pageable)).thenReturn(page);

        // Act
        PageResponse<IncomeTransactionResponse> result = incomeTransactionService.getIncomeTransactions(club.getId(), pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getTotalElements());

        verify(clubWalletService).getOrCreateWalletForClub(club.getId());
        verify(incomeTransactionRepository).findByClubWalletId(clubWallet.getId(), pageable);
    }

    // ========= Test getIncomeTransactionsByStatus =========

    @Test
    void getIncomeTransactionsByStatus_success() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        TransactionStatus status = TransactionStatus.PENDING;
        Page<IncomeTransaction> page = new PageImpl<>(List.of(incomeTransaction), pageable, 1);

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(incomeTransactionRepository.findByClubWalletIdAndStatus(clubWallet.getId(), status, pageable)).thenReturn(page);
        when(incomeTransactionMapper.toResponse(incomeTransaction)).thenReturn(incomeTransactionResponse);

        // Act
        PageResponse<IncomeTransactionResponse> result = incomeTransactionService.getIncomeTransactionsByStatus(club.getId(), status, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(incomeTransactionResponse, result.getContent().get(0));

        verify(clubWalletService).getOrCreateWalletForClub(club.getId());
        verify(incomeTransactionRepository).findByClubWalletIdAndStatus(clubWallet.getId(), status, pageable);
    }

    // ========= Test getIncomeTransactionsWithFilters =========

    @Test
    void getIncomeTransactionsWithFilters_withAllFilters_success() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<IncomeTransaction> page = new PageImpl<>(List.of(incomeTransaction), pageable, 1);

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(incomeTransactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(incomeTransactionMapper.toResponse(incomeTransaction)).thenReturn(incomeTransactionResponse);

        // Act
        PageResponse<IncomeTransactionResponse> result = incomeTransactionService.getIncomeTransactionsWithFilters(
                club.getId(),
                "Nguyen",
                TransactionStatus.PENDING,
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(150000),
                "fee",
                fee.getId(),
                pageable
        );

        // Assert
        assertNotNull(result);
        verify(clubWalletService).getOrCreateWalletForClub(club.getId());
        verify(incomeTransactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getIncomeTransactionsWithFilters_withSearchFilter_success() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<IncomeTransaction> page = new PageImpl<>(List.of(incomeTransaction), pageable, 1);

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(incomeTransactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(incomeTransactionMapper.toResponse(incomeTransaction)).thenReturn(incomeTransactionResponse);

        // Act
        PageResponse<IncomeTransactionResponse> result = incomeTransactionService.getIncomeTransactionsWithFilters(
                club.getId(),
                "Nguyen Van A",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                pageable
        );

        // Assert
        assertNotNull(result);
        verify(clubWalletService).getOrCreateWalletForClub(club.getId());
        verify(incomeTransactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    // ========= Test getIncomeTransactionById =========

    @Test
    void getIncomeTransactionById_success() throws AppException {
        // Arrange
        when(incomeTransactionRepository.findById(incomeTransaction.getId())).thenReturn(Optional.of(incomeTransaction));
        when(incomeTransactionMapper.toResponse(incomeTransaction)).thenReturn(incomeTransactionResponse);

        // Act
        IncomeTransactionResponse result = incomeTransactionService.getIncomeTransactionById(incomeTransaction.getId());

        // Assert
        assertNotNull(result);
        assertEquals(incomeTransactionResponse, result);

        verify(incomeTransactionRepository).findById(incomeTransaction.getId());
        verify(incomeTransactionMapper).toResponse(incomeTransaction);
    }

    @Test
    void getIncomeTransactionById_notFound_throwsException() throws AppException {
        // Arrange
        when(incomeTransactionRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            incomeTransactionService.getIncomeTransactionById(999L);
        });

        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
        verify(incomeTransactionRepository).findById(999L);
    }

    // ========= Test createIncomeTransaction =========

    @Test
    void createIncomeTransaction_byClubOfficer_autoApproved() throws AppException {
        // Arrange
        CreateIncomeTransactionRequest request = new CreateIncomeTransactionRequest();
        request.setAmount(BigDecimal.valueOf(100000));
        request.setDescription("Member fee payment");
        request.setTransactionDate(LocalDateTime.now());
        request.setSource("fee");
        request.setFeeId(fee.getId());
        request.setUserId(user.getId());

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(userService.getCurrentUserId()).thenReturn(createdBy.getId());
        when(userRepository.findById(createdBy.getId())).thenReturn(Optional.of(createdBy));
        when(incomeTransactionRepository.existsByUser_IdAndFee_IdAndStatus(user.getId(), fee.getId(), TransactionStatus.SUCCESS))
                .thenReturn(false);
        when(roleMemberShipRepository.existsClubAdmin(createdBy.getId(), club.getId())).thenReturn(true);
        when(feeRepository.findById(fee.getId())).thenReturn(Optional.of(fee));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(incomeTransactionRepository.existsByReference(anyString())).thenReturn(false);
        when(incomeTransactionRepository.save(any(IncomeTransaction.class))).thenAnswer(invocation -> {
            IncomeTransaction saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(incomeTransactionMapper.toResponse(any(IncomeTransaction.class))).thenReturn(incomeTransactionResponse);

        // Act
        IncomeTransactionResponse result = incomeTransactionService.createIncomeTransaction(club.getId(), request);

        // Assert
        assertNotNull(result);
        verify(clubWalletService).getOrCreateWalletForClub(club.getId());
        verify(roleMemberShipRepository).existsClubAdmin(createdBy.getId(), club.getId());
        verify(incomeTransactionRepository).save(any(IncomeTransaction.class));
        verify(clubWalletService).processIncomeTransaction(any(IncomeTransaction.class), isNull());
    }

    @Test
    void createIncomeTransaction_byTreasurer_pending() throws AppException {
        // Arrange
        CreateIncomeTransactionRequest request = new CreateIncomeTransactionRequest();
        request.setAmount(BigDecimal.valueOf(100000));
        request.setDescription("Member fee payment");
        request.setTransactionDate(LocalDateTime.now());
        request.setSource("fee");
        request.setFeeId(fee.getId());
        request.setUserId(user.getId());

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(userService.getCurrentUserId()).thenReturn(createdBy.getId());
        when(userRepository.findById(createdBy.getId())).thenReturn(Optional.of(createdBy));
        when(incomeTransactionRepository.existsByUser_IdAndFee_IdAndStatus(user.getId(), fee.getId(), TransactionStatus.SUCCESS))
                .thenReturn(false);
        when(roleMemberShipRepository.existsClubAdmin(createdBy.getId(), club.getId())).thenReturn(false);
        when(feeRepository.findById(fee.getId())).thenReturn(Optional.of(fee));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(incomeTransactionRepository.existsByReference(anyString())).thenReturn(false);
        when(incomeTransactionRepository.save(any(IncomeTransaction.class))).thenAnswer(invocation -> {
            IncomeTransaction saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(incomeTransactionMapper.toResponse(any(IncomeTransaction.class))).thenReturn(incomeTransactionResponse);

        // Act
        IncomeTransactionResponse result = incomeTransactionService.createIncomeTransaction(club.getId(), request);

        // Assert
        assertNotNull(result);
        verify(clubWalletService).getOrCreateWalletForClub(club.getId());
        verify(roleMemberShipRepository).existsClubAdmin(createdBy.getId(), club.getId());
        verify(incomeTransactionRepository).save(any(IncomeTransaction.class));
        verify(clubWalletService).processIncomeTransaction(any(IncomeTransaction.class), isNull());
    }

    @Test
    void createIncomeTransaction_duplicatePayment_throwsException() throws AppException {
        // Arrange
        CreateIncomeTransactionRequest request = new CreateIncomeTransactionRequest();
        request.setAmount(BigDecimal.valueOf(100000));
        request.setFeeId(fee.getId());
        request.setUserId(user.getId());

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(userService.getCurrentUserId()).thenReturn(createdBy.getId());
        when(userRepository.findById(createdBy.getId())).thenReturn(Optional.of(createdBy));
        when(incomeTransactionRepository.existsByUser_IdAndFee_IdAndStatus(user.getId(), fee.getId(), TransactionStatus.SUCCESS))
                .thenReturn(true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(feeRepository.findById(fee.getId())).thenReturn(Optional.of(fee));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            incomeTransactionService.createIncomeTransaction(club.getId(), request);
        });

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(incomeTransactionRepository, never()).save(any(IncomeTransaction.class));
    }

    @Test
    void createIncomeTransaction_feeNotFound_throwsException() throws AppException {
        // Arrange
        CreateIncomeTransactionRequest request = new CreateIncomeTransactionRequest();
        request.setAmount(BigDecimal.valueOf(100000));
        request.setFeeId(999L);

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(userService.getCurrentUserId()).thenReturn(createdBy.getId());
        when(userRepository.findById(createdBy.getId())).thenReturn(Optional.of(createdBy));
        when(roleMemberShipRepository.existsClubAdmin(createdBy.getId(), club.getId())).thenReturn(false);
        when(incomeTransactionRepository.existsByReference(anyString())).thenReturn(false);
        when(feeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            incomeTransactionService.createIncomeTransaction(club.getId(), request);
        });

        assertEquals(ErrorCode.FEE_NOT_FOUND, exception.getErrorCode());
        verify(incomeTransactionRepository, never()).save(any(IncomeTransaction.class));
    }

    @Test
    void createIncomeTransaction_userNotFound_throwsException() throws AppException {
        // Arrange
        CreateIncomeTransactionRequest request = new CreateIncomeTransactionRequest();
        request.setAmount(BigDecimal.valueOf(100000));
        request.setUserId(999L);

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(userService.getCurrentUserId()).thenReturn(createdBy.getId());
        when(userRepository.findById(createdBy.getId())).thenReturn(Optional.of(createdBy));
        when(roleMemberShipRepository.existsClubAdmin(createdBy.getId(), club.getId())).thenReturn(false);
        when(incomeTransactionRepository.existsByReference(anyString())).thenReturn(false);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            incomeTransactionService.createIncomeTransaction(club.getId(), request);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(incomeTransactionRepository, never()).save(any(IncomeTransaction.class));
    }

    // ========= Test updateIncomeTransaction =========

    @Test
    void updateIncomeTransaction_success() throws AppException {
        // Arrange
        UpdateIncomeTransactionRequest request = new UpdateIncomeTransactionRequest();
        request.setAmount(BigDecimal.valueOf(150000));
        request.setDescription("Updated description");
        request.setTransactionDate(LocalDateTime.now());
        request.setSource("donation");

        when(incomeTransactionRepository.findById(incomeTransaction.getId())).thenReturn(Optional.of(incomeTransaction));
        when(incomeTransactionRepository.save(any(IncomeTransaction.class))).thenReturn(incomeTransaction);
        when(incomeTransactionMapper.toResponse(incomeTransaction)).thenReturn(incomeTransactionResponse);

        // Act
        IncomeTransactionResponse result = incomeTransactionService.updateIncomeTransaction(incomeTransaction.getId(), request);

        // Assert
        assertNotNull(result);
        verify(incomeTransactionRepository).findById(incomeTransaction.getId());
        verify(incomeTransactionRepository).save(any(IncomeTransaction.class));
        verify(incomeTransactionMapper).toResponse(incomeTransaction);
    }

    @Test
    void updateIncomeTransaction_notPending_throwsException() throws AppException {
        // Arrange
        incomeTransaction.setStatus(TransactionStatus.SUCCESS);
        UpdateIncomeTransactionRequest request = new UpdateIncomeTransactionRequest();
        request.setAmount(BigDecimal.valueOf(150000));

        when(incomeTransactionRepository.findById(incomeTransaction.getId())).thenReturn(Optional.of(incomeTransaction));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            incomeTransactionService.updateIncomeTransaction(incomeTransaction.getId(), request);
        });

        assertEquals(ErrorCode.TRANSACTION_CANNOT_BE_UPDATED, exception.getErrorCode());
        verify(incomeTransactionRepository, never()).save(any(IncomeTransaction.class));
    }

    @Test
    void updateIncomeTransaction_notFound_throwsException() throws AppException {
        // Arrange
        UpdateIncomeTransactionRequest request = new UpdateIncomeTransactionRequest();
        request.setAmount(BigDecimal.valueOf(150000));

        when(incomeTransactionRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            incomeTransactionService.updateIncomeTransaction(999L, request);
        });

        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
        verify(incomeTransactionRepository, never()).save(any(IncomeTransaction.class));
    }

    // ========= Test approveIncomeTransaction =========

    @Test
    void approveIncomeTransaction_success() throws AppException {
        // Arrange
        when(incomeTransactionRepository.findById(incomeTransaction.getId())).thenReturn(Optional.of(incomeTransaction));
        when(incomeTransactionRepository.save(any(IncomeTransaction.class))).thenAnswer(invocation -> {
            IncomeTransaction saved = invocation.getArgument(0);
            saved.setStatus(TransactionStatus.SUCCESS);
            return saved;
        });
        when(incomeTransactionMapper.toResponse(any(IncomeTransaction.class))).thenReturn(incomeTransactionResponse);

        // Act
        IncomeTransactionResponse result = incomeTransactionService.approveIncomeTransaction(incomeTransaction.getId());

        // Assert
        assertNotNull(result);
        verify(incomeTransactionRepository).findById(incomeTransaction.getId());
        verify(incomeTransactionRepository).save(any(IncomeTransaction.class));
        verify(clubWalletService).processIncomeTransaction(any(IncomeTransaction.class), any(IncomeTransaction.class));
    }

    @Test
    void approveIncomeTransaction_notPending_throwsException() throws AppException {
        // Arrange
        incomeTransaction.setStatus(TransactionStatus.SUCCESS);

        when(incomeTransactionRepository.findById(incomeTransaction.getId())).thenReturn(Optional.of(incomeTransaction));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            incomeTransactionService.approveIncomeTransaction(incomeTransaction.getId());
        });

        assertEquals(ErrorCode.TRANSACTION_ALREADY_PROCESSED, exception.getErrorCode());
        verify(clubWalletService, never()).processIncomeTransaction(any(), any());
    }

    @Test
    void approveIncomeTransaction_notFound_throwsException() throws AppException {
        // Arrange
        when(incomeTransactionRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            incomeTransactionService.approveIncomeTransaction(999L);
        });

        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    // ========= Test rejectIncomeTransaction =========

    @Test
    void rejectIncomeTransaction_success() throws AppException {
        // Arrange
        when(incomeTransactionRepository.findById(incomeTransaction.getId())).thenReturn(Optional.of(incomeTransaction));
        when(incomeTransactionRepository.save(any(IncomeTransaction.class))).thenAnswer(invocation -> {
            IncomeTransaction saved = invocation.getArgument(0);
            saved.setStatus(TransactionStatus.CANCELLED);
            return saved;
        });
        when(incomeTransactionMapper.toResponse(any(IncomeTransaction.class))).thenReturn(incomeTransactionResponse);

        // Act
        IncomeTransactionResponse result = incomeTransactionService.rejectIncomeTransaction(incomeTransaction.getId());

        // Assert
        assertNotNull(result);
        verify(incomeTransactionRepository).findById(incomeTransaction.getId());
        verify(incomeTransactionRepository).save(any(IncomeTransaction.class));
    }

    @Test
    void rejectIncomeTransaction_notPending_throwsException() throws AppException {
        // Arrange
        incomeTransaction.setStatus(TransactionStatus.SUCCESS);

        when(incomeTransactionRepository.findById(incomeTransaction.getId())).thenReturn(Optional.of(incomeTransaction));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            incomeTransactionService.rejectIncomeTransaction(incomeTransaction.getId());
        });

        assertEquals(ErrorCode.TRANSACTION_ALREADY_PROCESSED, exception.getErrorCode());
    }

    // ========= Test deleteIncomeTransaction =========

    @Test
    void deleteIncomeTransaction_pending_success() throws AppException {
        // Arrange
        incomeTransaction.setStatus(TransactionStatus.PENDING);

        when(incomeTransactionRepository.findById(incomeTransaction.getId())).thenReturn(Optional.of(incomeTransaction));
        doNothing().when(incomeTransactionRepository).delete(incomeTransaction);

        // Act
        incomeTransactionService.deleteIncomeTransaction(incomeTransaction.getId());

        // Assert
        verify(incomeTransactionRepository).findById(incomeTransaction.getId());
        verify(incomeTransactionRepository).delete(incomeTransaction);
    }

    @Test
    void deleteIncomeTransaction_cancelled_success() throws AppException {
        // Arrange
        incomeTransaction.setStatus(TransactionStatus.CANCELLED);

        when(incomeTransactionRepository.findById(incomeTransaction.getId())).thenReturn(Optional.of(incomeTransaction));
        doNothing().when(incomeTransactionRepository).delete(any(IncomeTransaction.class));

        // Act
        incomeTransactionService.deleteIncomeTransaction(incomeTransaction.getId());

        // Assert
        verify(incomeTransactionRepository).findById(incomeTransaction.getId());
        verify(incomeTransactionRepository).delete(any(IncomeTransaction.class));
    }

    @Test
    void deleteIncomeTransaction_success_throwsException() throws AppException {
        // Arrange
        incomeTransaction.setStatus(TransactionStatus.SUCCESS);

        when(incomeTransactionRepository.findById(incomeTransaction.getId())).thenReturn(Optional.of(incomeTransaction));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            incomeTransactionService.deleteIncomeTransaction(incomeTransaction.getId());
        });

        assertEquals(ErrorCode.TRANSACTION_CANNOT_BE_DELETED, exception.getErrorCode());
        verify(incomeTransactionRepository, never()).delete(any(IncomeTransaction.class));
    }

    @Test
    void deleteIncomeTransaction_processing_throwsException() throws AppException {
        // Arrange
        incomeTransaction.setStatus(TransactionStatus.PROCESSING);

        when(incomeTransactionRepository.findById(incomeTransaction.getId())).thenReturn(Optional.of(incomeTransaction));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            incomeTransactionService.deleteIncomeTransaction(incomeTransaction.getId());
        });

        assertEquals(ErrorCode.TRANSACTION_CANNOT_BE_DELETED, exception.getErrorCode());
        verify(incomeTransactionRepository, never()).delete(any(IncomeTransaction.class));
    }

    @Test
    void deleteIncomeTransaction_notFound_throwsException() throws AppException {
        // Arrange
        when(incomeTransactionRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            incomeTransactionService.deleteIncomeTransaction(999L);
        });

        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
        verify(incomeTransactionRepository, never()).delete(any(IncomeTransaction.class));
    }
}

