package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.request.CreateOutcomeTransactionRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateOutcomeTransactionRequest;
import com.sep490.backendclubmanagement.dto.response.OutcomeTransactionResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubWallet;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.mapper.OutcomeTransactionMapper;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.service.club.club.ClubWalletService;
import com.sep490.backendclubmanagement.service.transaction.outcome.OutcomeTransactionServiceImpl;
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
 * Unit test cho OutcomeTransactionServiceImpl (JUnit5 + Mockito)
 */
@ExtendWith(MockitoExtension.class)
class OutcomeTransactionServiceImplTest {

    @Mock
    private OutcomeTransactionRepository outcomeTransactionRepository;

    @Mock
    private ClubWalletRepository clubWalletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OutcomeTransactionMapper outcomeTransactionMapper;

    @Mock
    private UserService userService;

    @Mock
    private RoleMemberShipRepository roleMemberShipRepository;

    @Mock
    private ClubWalletService clubWalletService;

    @InjectMocks
    private OutcomeTransactionServiceImpl outcomeTransactionService;

    private Club club;
    private ClubWallet clubWallet;
    private User createdBy;
    private OutcomeTransaction outcomeTransaction;
    private OutcomeTransactionResponse outcomeTransactionResponse;

    @BeforeEach
    void setUp() {
        club = new Club();
        club.setId(1L);
        club.setClubName("Test Club");

        clubWallet = new ClubWallet();
        clubWallet.setId(100L);
        clubWallet.setClub(club);
        clubWallet.setBalance(BigDecimal.valueOf(1000000));

        createdBy = new User();
        createdBy.setId(20L);
        createdBy.setFullName("Nguyễn Văn B");
        createdBy.setEmail("b@example.com");

        outcomeTransaction = new OutcomeTransaction();
        outcomeTransaction.setId(1L);
        outcomeTransaction.setTransactionCode("OUT-20240101-ABC123");
        outcomeTransaction.setAmount(BigDecimal.valueOf(50000));
        outcomeTransaction.setDescription("Purchase supplies");
        outcomeTransaction.setTransactionDate(LocalDateTime.now());
        outcomeTransaction.setRecipient("ABC Store");
        outcomeTransaction.setPurpose("Event supplies");
        outcomeTransaction.setStatus(TransactionStatus.PENDING);
        outcomeTransaction.setClubWallet(clubWallet);
        outcomeTransaction.setCreatedBy(createdBy);

        outcomeTransactionResponse = new OutcomeTransactionResponse();
        outcomeTransactionResponse.setId(1L);
        outcomeTransactionResponse.setTransactionCode("OUT-20240101-ABC123");
        outcomeTransactionResponse.setAmount(BigDecimal.valueOf(50000));
        outcomeTransactionResponse.setStatus(TransactionStatus.PENDING);
    }

    // ========= Test getOutcomeTransactions =========

    @Test
    void getOutcomeTransactions_success() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<OutcomeTransaction> page = new PageImpl<>(List.of(outcomeTransaction), pageable, 1);

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(outcomeTransactionRepository.findByClubWalletId(clubWallet.getId(), pageable)).thenReturn(page);
        when(outcomeTransactionMapper.toResponse(outcomeTransaction)).thenReturn(outcomeTransactionResponse);

        // Act
        PageResponse<OutcomeTransactionResponse> result = outcomeTransactionService.getOutcomeTransactions(club.getId(), pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(outcomeTransactionResponse, result.getContent().get(0));

        verify(clubWalletService).getOrCreateWalletForClub(club.getId());
        verify(outcomeTransactionRepository).findByClubWalletId(clubWallet.getId(), pageable);
        verify(outcomeTransactionMapper).toResponse(outcomeTransaction);
    }

    @Test
    void getOutcomeTransactions_emptyList() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<OutcomeTransaction> page = new PageImpl<>(List.of(), pageable, 0);

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(outcomeTransactionRepository.findByClubWalletId(clubWallet.getId(), pageable)).thenReturn(page);

        // Act
        PageResponse<OutcomeTransactionResponse> result = outcomeTransactionService.getOutcomeTransactions(club.getId(), pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getTotalElements());

        verify(clubWalletService).getOrCreateWalletForClub(club.getId());
        verify(outcomeTransactionRepository).findByClubWalletId(clubWallet.getId(), pageable);
    }

    // ========= Test getOutcomeTransactionsByStatus =========

    @Test
    void getOutcomeTransactionsByStatus_success() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        TransactionStatus status = TransactionStatus.PENDING;
        Page<OutcomeTransaction> page = new PageImpl<>(List.of(outcomeTransaction), pageable, 1);

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(outcomeTransactionRepository.findByClubWalletIdAndStatus(clubWallet.getId(), status, pageable)).thenReturn(page);
        when(outcomeTransactionMapper.toResponse(outcomeTransaction)).thenReturn(outcomeTransactionResponse);

        // Act
        PageResponse<OutcomeTransactionResponse> result = outcomeTransactionService.getOutcomeTransactionsByStatus(club.getId(), status, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(outcomeTransactionResponse, result.getContent().get(0));

        verify(clubWalletService).getOrCreateWalletForClub(club.getId());
        verify(outcomeTransactionRepository).findByClubWalletIdAndStatus(clubWallet.getId(), status, pageable);
    }

    // ========= Test getOutcomeTransactionsWithFilters =========

    @Test
    void getOutcomeTransactionsWithFilters_withAllFilters_success() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<OutcomeTransaction> page = new PageImpl<>(List.of(outcomeTransaction), pageable, 1);

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(outcomeTransactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(outcomeTransactionMapper.toResponse(outcomeTransaction)).thenReturn(outcomeTransactionResponse);

        // Act
        PageResponse<OutcomeTransactionResponse> result = outcomeTransactionService.getOutcomeTransactionsWithFilters(
                club.getId(),
                "ABC",
                TransactionStatus.PENDING,
                LocalDate.now().minusDays(7),
                LocalDate.now(),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(100000),
                "event",
                pageable
        );

        // Assert
        assertNotNull(result);
        verify(clubWalletService).getOrCreateWalletForClub(club.getId());
        verify(outcomeTransactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getOutcomeTransactionsWithFilters_withSearchFilter_success() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<OutcomeTransaction> page = new PageImpl<>(List.of(outcomeTransaction), pageable, 1);

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(outcomeTransactionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(outcomeTransactionMapper.toResponse(outcomeTransaction)).thenReturn(outcomeTransactionResponse);

        // Act
        PageResponse<OutcomeTransactionResponse> result = outcomeTransactionService.getOutcomeTransactionsWithFilters(
                club.getId(),
                "ABC Store",
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
        verify(outcomeTransactionRepository).findAll(any(Specification.class), eq(pageable));
    }

    // ========= Test getOutcomeTransactionById =========

    @Test
    void getOutcomeTransactionById_success() throws AppException {
        // Arrange
        when(outcomeTransactionRepository.findById(outcomeTransaction.getId())).thenReturn(Optional.of(outcomeTransaction));
        when(outcomeTransactionMapper.toResponse(outcomeTransaction)).thenReturn(outcomeTransactionResponse);

        // Act
        OutcomeTransactionResponse result = outcomeTransactionService.getOutcomeTransactionById(outcomeTransaction.getId());

        // Assert
        assertNotNull(result);
        assertEquals(outcomeTransactionResponse, result);

        verify(outcomeTransactionRepository).findById(outcomeTransaction.getId());
        verify(outcomeTransactionMapper).toResponse(outcomeTransaction);
    }

    @Test
    void getOutcomeTransactionById_notFound_throwsException() throws AppException {
        // Arrange
        when(outcomeTransactionRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            outcomeTransactionService.getOutcomeTransactionById(999L);
        });

        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
        verify(outcomeTransactionRepository).findById(999L);
    }

    // ========= Test createOutcomeTransaction =========

    @Test
    void createOutcomeTransaction_byClubOfficer_autoApproved() throws AppException {
        // Arrange
        CreateOutcomeTransactionRequest request = new CreateOutcomeTransactionRequest();
        request.setAmount(BigDecimal.valueOf(50000));
        request.setDescription("Purchase supplies");
        request.setTransactionDate(LocalDateTime.now());
        request.setRecipient("ABC Store");
        request.setPurpose("Event supplies");

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(userService.getCurrentUserId()).thenReturn(createdBy.getId());
        when(userRepository.findById(createdBy.getId())).thenReturn(Optional.of(createdBy));
        when(roleMemberShipRepository.existsClubAdmin(createdBy.getId(), club.getId())).thenReturn(true);
        when(outcomeTransactionRepository.existsByTransactionCode(anyString())).thenReturn(false);
        when(outcomeTransactionRepository.save(any(OutcomeTransaction.class))).thenAnswer(invocation -> {
            OutcomeTransaction saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(outcomeTransactionMapper.toResponse(any(OutcomeTransaction.class))).thenReturn(outcomeTransactionResponse);

        // Act
        OutcomeTransactionResponse result = outcomeTransactionService.createOutcomeTransaction(club.getId(), request);

        // Assert
        assertNotNull(result);
        verify(clubWalletService).getOrCreateWalletForClub(club.getId());
        verify(roleMemberShipRepository).existsClubAdmin(createdBy.getId(), club.getId());
        verify(outcomeTransactionRepository).save(any(OutcomeTransaction.class));
        verify(clubWalletService).processOutcomeTransaction(any(OutcomeTransaction.class), isNull());
    }

    @Test
    void createOutcomeTransaction_byTreasurer_pending() throws AppException {
        // Arrange
        CreateOutcomeTransactionRequest request = new CreateOutcomeTransactionRequest();
        request.setAmount(BigDecimal.valueOf(50000));
        request.setDescription("Purchase supplies");
        request.setTransactionDate(LocalDateTime.now());
        request.setRecipient("ABC Store");
        request.setPurpose("Event supplies");

        when(clubWalletService.getOrCreateWalletForClub(club.getId())).thenReturn(clubWallet);
        when(userService.getCurrentUserId()).thenReturn(createdBy.getId());
        when(userRepository.findById(createdBy.getId())).thenReturn(Optional.of(createdBy));
        when(roleMemberShipRepository.existsClubAdmin(createdBy.getId(), club.getId())).thenReturn(false);
        when(outcomeTransactionRepository.existsByTransactionCode(anyString())).thenReturn(false);
        when(outcomeTransactionRepository.save(any(OutcomeTransaction.class))).thenAnswer(invocation -> {
            OutcomeTransaction saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(outcomeTransactionMapper.toResponse(any(OutcomeTransaction.class))).thenReturn(outcomeTransactionResponse);

        // Act
        OutcomeTransactionResponse result = outcomeTransactionService.createOutcomeTransaction(club.getId(), request);

        // Assert
        assertNotNull(result);
        verify(clubWalletService).getOrCreateWalletForClub(club.getId());
        verify(roleMemberShipRepository).existsClubAdmin(createdBy.getId(), club.getId());
        verify(outcomeTransactionRepository).save(any(OutcomeTransaction.class));
        verify(clubWalletService).processOutcomeTransaction(any(OutcomeTransaction.class), isNull());
    }

    // ========= Test updateOutcomeTransaction =========

    @Test
    void updateOutcomeTransaction_success() throws AppException {
        // Arrange
        UpdateOutcomeTransactionRequest request = new UpdateOutcomeTransactionRequest();
        request.setAmount(BigDecimal.valueOf(80000));
        request.setDescription("Updated description");
        request.setTransactionDate(LocalDateTime.now());
        request.setRecipient("XYZ Store");
        request.setPurpose("Updated purpose");

        when(outcomeTransactionRepository.findById(outcomeTransaction.getId())).thenReturn(Optional.of(outcomeTransaction));
        when(outcomeTransactionRepository.save(any(OutcomeTransaction.class))).thenReturn(outcomeTransaction);
        when(outcomeTransactionMapper.toResponse(outcomeTransaction)).thenReturn(outcomeTransactionResponse);

        // Act
        OutcomeTransactionResponse result = outcomeTransactionService.updateOutcomeTransaction(outcomeTransaction.getId(), request);

        // Assert
        assertNotNull(result);
        verify(outcomeTransactionRepository).findById(outcomeTransaction.getId());
        verify(outcomeTransactionRepository).save(any(OutcomeTransaction.class));
        verify(outcomeTransactionMapper).toResponse(outcomeTransaction);
    }

    @Test
    void updateOutcomeTransaction_notPending_throwsException() throws AppException {
        // Arrange
        outcomeTransaction.setStatus(TransactionStatus.SUCCESS);
        UpdateOutcomeTransactionRequest request = new UpdateOutcomeTransactionRequest();
        request.setAmount(BigDecimal.valueOf(80000));

        when(outcomeTransactionRepository.findById(outcomeTransaction.getId())).thenReturn(Optional.of(outcomeTransaction));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            outcomeTransactionService.updateOutcomeTransaction(outcomeTransaction.getId(), request);
        });

        assertEquals(ErrorCode.TRANSACTION_CANNOT_BE_UPDATED, exception.getErrorCode());
        verify(outcomeTransactionRepository, never()).save(any(OutcomeTransaction.class));
    }

    @Test
    void updateOutcomeTransaction_notFound_throwsException() throws AppException {
        // Arrange
        UpdateOutcomeTransactionRequest request = new UpdateOutcomeTransactionRequest();
        request.setAmount(BigDecimal.valueOf(80000));

        when(outcomeTransactionRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            outcomeTransactionService.updateOutcomeTransaction(999L, request);
        });

        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
        verify(outcomeTransactionRepository, never()).save(any(OutcomeTransaction.class));
    }

    // ========= Test approveOutcomeTransaction =========

    @Test
    void approveOutcomeTransaction_success() throws AppException {
        // Arrange
        when(outcomeTransactionRepository.findById(outcomeTransaction.getId())).thenReturn(Optional.of(outcomeTransaction));
        when(outcomeTransactionRepository.save(any(OutcomeTransaction.class))).thenAnswer(invocation -> {
            OutcomeTransaction saved = invocation.getArgument(0);
            saved.setStatus(TransactionStatus.SUCCESS);
            return saved;
        });
        when(outcomeTransactionMapper.toResponse(any(OutcomeTransaction.class))).thenReturn(outcomeTransactionResponse);

        // Act
        OutcomeTransactionResponse result = outcomeTransactionService.approveOutcomeTransaction(outcomeTransaction.getId());

        // Assert
        assertNotNull(result);
        verify(outcomeTransactionRepository).findById(outcomeTransaction.getId());
        verify(outcomeTransactionRepository).save(any(OutcomeTransaction.class));
        verify(clubWalletService).processOutcomeTransaction(any(OutcomeTransaction.class), any(OutcomeTransaction.class));
    }

    @Test
    void approveOutcomeTransaction_notPending_throwsException() throws AppException {
        // Arrange
        outcomeTransaction.setStatus(TransactionStatus.SUCCESS);

        when(outcomeTransactionRepository.findById(outcomeTransaction.getId())).thenReturn(Optional.of(outcomeTransaction));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            outcomeTransactionService.approveOutcomeTransaction(outcomeTransaction.getId());
        });

        assertEquals(ErrorCode.TRANSACTION_ALREADY_PROCESSED, exception.getErrorCode());
        verify(clubWalletService, never()).processOutcomeTransaction(any(), any());
    }

    @Test
    void approveOutcomeTransaction_notFound_throwsException() throws AppException {
        // Arrange
        when(outcomeTransactionRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            outcomeTransactionService.approveOutcomeTransaction(999L);
        });

        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    // ========= Test rejectOutcomeTransaction =========

    @Test
    void rejectOutcomeTransaction_success() throws AppException {
        // Arrange
        when(outcomeTransactionRepository.findById(outcomeTransaction.getId())).thenReturn(Optional.of(outcomeTransaction));
        when(outcomeTransactionRepository.save(any(OutcomeTransaction.class))).thenAnswer(invocation -> {
            OutcomeTransaction saved = invocation.getArgument(0);
            saved.setStatus(TransactionStatus.CANCELLED);
            return saved;
        });
        when(outcomeTransactionMapper.toResponse(any(OutcomeTransaction.class))).thenReturn(outcomeTransactionResponse);

        // Act
        OutcomeTransactionResponse result = outcomeTransactionService.rejectOutcomeTransaction(outcomeTransaction.getId());

        // Assert
        assertNotNull(result);
        verify(outcomeTransactionRepository).findById(outcomeTransaction.getId());
        verify(outcomeTransactionRepository).save(any(OutcomeTransaction.class));
    }

    @Test
    void rejectOutcomeTransaction_notPending_throwsException() throws AppException {
        // Arrange
        outcomeTransaction.setStatus(TransactionStatus.SUCCESS);

        when(outcomeTransactionRepository.findById(outcomeTransaction.getId())).thenReturn(Optional.of(outcomeTransaction));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            outcomeTransactionService.rejectOutcomeTransaction(outcomeTransaction.getId());
        });

        assertEquals(ErrorCode.TRANSACTION_ALREADY_PROCESSED, exception.getErrorCode());
    }

    // ========= Test deleteOutcomeTransaction =========

    @Test
    void deleteOutcomeTransaction_pending_success() throws AppException {
        // Arrange
        outcomeTransaction.setStatus(TransactionStatus.PENDING);

        when(outcomeTransactionRepository.findById(outcomeTransaction.getId())).thenReturn(Optional.of(outcomeTransaction));
        doNothing().when(outcomeTransactionRepository).delete(any(OutcomeTransaction.class));

        // Act
        outcomeTransactionService.deleteOutcomeTransaction(outcomeTransaction.getId());

        // Assert
        verify(outcomeTransactionRepository).findById(outcomeTransaction.getId());
        verify(outcomeTransactionRepository).delete(any(OutcomeTransaction.class));
    }

    @Test
    void deleteOutcomeTransaction_cancelled_success() throws AppException {
        // Arrange
        outcomeTransaction.setStatus(TransactionStatus.CANCELLED);

        when(outcomeTransactionRepository.findById(outcomeTransaction.getId())).thenReturn(Optional.of(outcomeTransaction));
        doNothing().when(outcomeTransactionRepository).delete(any(OutcomeTransaction.class));

        // Act
        outcomeTransactionService.deleteOutcomeTransaction(outcomeTransaction.getId());

        // Assert
        verify(outcomeTransactionRepository).findById(outcomeTransaction.getId());
        verify(outcomeTransactionRepository).delete(any(OutcomeTransaction.class));
    }

    @Test
    void deleteOutcomeTransaction_success_throwsException() throws AppException {
        // Arrange
        outcomeTransaction.setStatus(TransactionStatus.SUCCESS);

        when(outcomeTransactionRepository.findById(outcomeTransaction.getId())).thenReturn(Optional.of(outcomeTransaction));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            outcomeTransactionService.deleteOutcomeTransaction(outcomeTransaction.getId());
        });

        assertEquals(ErrorCode.TRANSACTION_CANNOT_BE_DELETED, exception.getErrorCode());
        verify(outcomeTransactionRepository, never()).delete(any(OutcomeTransaction.class));
    }

    @Test
    void deleteOutcomeTransaction_processing_throwsException() throws AppException {
        // Arrange
        outcomeTransaction.setStatus(TransactionStatus.PROCESSING);

        when(outcomeTransactionRepository.findById(outcomeTransaction.getId())).thenReturn(Optional.of(outcomeTransaction));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            outcomeTransactionService.deleteOutcomeTransaction(outcomeTransaction.getId());
        });

        assertEquals(ErrorCode.TRANSACTION_CANNOT_BE_DELETED, exception.getErrorCode());
        verify(outcomeTransactionRepository, never()).delete(any(OutcomeTransaction.class));
    }

    @Test
    void deleteOutcomeTransaction_notFound_throwsException() throws AppException {
        // Arrange
        when(outcomeTransactionRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            outcomeTransactionService.deleteOutcomeTransaction(999L);
        });

        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
        verify(outcomeTransactionRepository, never()).delete(any(OutcomeTransaction.class));
    }
}

