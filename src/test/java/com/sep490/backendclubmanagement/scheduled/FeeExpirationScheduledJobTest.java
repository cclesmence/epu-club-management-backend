//package com.sep490.backendclubmanagement.scheduled;
//
//import com.sep490.backendclubmanagement.entity.fee.Fee;
//import com.sep490.backendclubmanagement.entity.fee.FeeType;
//import com.sep490.backendclubmanagement.repository.FeeRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.Arrays;
//import java.util.Collections;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class FeeExpirationScheduledJobTest {
//
//    @Mock
//    private FeeRepository feeRepository;
//
//    @InjectMocks
//    private FeeExpirationScheduledJob scheduledJob;
//
//    private Fee createTestFee(Long id, String title, LocalDate dueDate) {
//        return Fee.builder()
//                .id(id)
//                .title(title)
//                .amount(BigDecimal.valueOf(100000))
//                .feeType(FeeType.MEMBERSHIP)
//                .dueDate(dueDate)
//                .isDraft(false)
//                .hasEverExpired(false)
//                .isMandatory(true)
//                .build();
//    }
//
//    @BeforeEach
//    void setUp() {
//        // Reset mocks before each test
//        reset(feeRepository);
//    }
//
//    @Test
//    void testLockFees_withExpiredFeesOnly_shouldLockThem() {
//        // Given
//        LocalDate today = LocalDate.now();
//        Fee expiredFee1 = createTestFee(1L, "Expired Fee 1", today.minusDays(1));
//        Fee expiredFee2 = createTestFee(2L, "Expired Fee 2", today.minusDays(5));
//
//        when(feeRepository.findByDueDateBeforeAndHasEverExpiredFalseAndIsDraftFalse(any()))
//                .thenReturn(Arrays.asList(expiredFee1, expiredFee2));
//        when(feeRepository.findFeesWithSuccessfulPaymentsButNotLocked())
//                .thenReturn(Collections.emptyList());
//        when(feeRepository.saveAll(any()))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        // When
//        scheduledJob.lockFees();
//
//        // Then
//        verify(feeRepository, times(1)).findByDueDateBeforeAndHasEverExpiredFalseAndIsDraftFalse(any());
//        verify(feeRepository, times(1)).findFeesWithSuccessfulPaymentsButNotLocked();
//        verify(feeRepository, times(1)).saveAll(any());
//
//        // Verify fees were marked as locked
//        assert expiredFee1.getHasEverExpired();
//        assert expiredFee2.getHasEverExpired();
//    }
//
//    @Test
//    void testLockFees_withFeesWithPaymentsOnly_shouldLockThem() {
//        // Given
//        LocalDate futureDate = LocalDate.now().plusDays(30);
//        Fee feeWithPayment1 = createTestFee(3L, "Fee With Payment 1", futureDate);
//        Fee feeWithPayment2 = createTestFee(4L, "Fee With Payment 2", futureDate);
//
//        when(feeRepository.findByDueDateBeforeAndHasEverExpiredFalseAndIsDraftFalse(any()))
//                .thenReturn(Collections.emptyList());
//        when(feeRepository.findFeesWithSuccessfulPaymentsButNotLocked())
//                .thenReturn(Arrays.asList(feeWithPayment1, feeWithPayment2));
//        when(feeRepository.saveAll(any()))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        // When
//        scheduledJob.lockFees();
//
//        // Then
//        verify(feeRepository, times(1)).findByDueDateBeforeAndHasEverExpiredFalseAndIsDraftFalse(any());
//        verify(feeRepository, times(1)).findFeesWithSuccessfulPaymentsButNotLocked();
//        verify(feeRepository, times(1)).saveAll(any());
//
//        // Verify fees were marked as locked
//        assert feeWithPayment1.getHasEverExpired();
//        assert feeWithPayment2.getHasEverExpired();
//    }
//
//    @Test
//    void testLockFees_withBothExpiredAndPaidFees_shouldLockAll() {
//        // Given
//        LocalDate today = LocalDate.now();
//        Fee expiredFee = createTestFee(5L, "Expired Fee", today.minusDays(1));
//        Fee feeWithPayment = createTestFee(6L, "Fee With Payment", today.plusDays(30));
//
//        when(feeRepository.findByDueDateBeforeAndHasEverExpiredFalseAndIsDraftFalse(any()))
//                .thenReturn(Collections.singletonList(expiredFee));
//        when(feeRepository.findFeesWithSuccessfulPaymentsButNotLocked())
//                .thenReturn(Collections.singletonList(feeWithPayment));
//        when(feeRepository.saveAll(any()))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        // When
//        scheduledJob.lockFees();
//
//        // Then
//        verify(feeRepository, times(1)).findByDueDateBeforeAndHasEverExpiredFalseAndIsDraftFalse(any());
//        verify(feeRepository, times(1)).findFeesWithSuccessfulPaymentsButNotLocked();
//        verify(feeRepository, times(1)).saveAll(any());
//
//        // Verify both fees were marked as locked
//        assert expiredFee.getHasEverExpired();
//        assert feeWithPayment.getHasEverExpired();
//    }
//
//    @Test
//    void testLockFees_withSameFeeInBothLists_shouldLockOnlyOnce() {
//        // Given: A fee that is both expired AND has payments
//        LocalDate today = LocalDate.now();
//        Fee feeExpiredAndPaid = createTestFee(7L, "Expired and Paid Fee", today.minusDays(1));
//
//        when(feeRepository.findByDueDateBeforeAndHasEverExpiredFalseAndIsDraftFalse(any()))
//                .thenReturn(Collections.singletonList(feeExpiredAndPaid));
//        when(feeRepository.findFeesWithSuccessfulPaymentsButNotLocked())
//                .thenReturn(Collections.singletonList(feeExpiredAndPaid));
//        when(feeRepository.saveAll(any()))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        // When
//        scheduledJob.lockFees();
//
//        // Then
//        verify(feeRepository, times(1)).findByDueDateBeforeAndHasEverExpiredFalseAndIsDraftFalse(any());
//        verify(feeRepository, times(1)).findFeesWithSuccessfulPaymentsButNotLocked();
//        verify(feeRepository, times(1)).saveAll(any());
//
//        // Verify fee was marked as locked (only once, not twice)
//        assert feeExpiredAndPaid.getHasEverExpired();
//    }
//
//    @Test
//    void testLockFees_withNoFeesToLock_shouldNotSave() {
//        // Given
//        when(feeRepository.findByDueDateBeforeAndHasEverExpiredFalseAndIsDraftFalse(any()))
//                .thenReturn(Collections.emptyList());
//        when(feeRepository.findFeesWithSuccessfulPaymentsButNotLocked())
//                .thenReturn(Collections.emptyList());
//
//        // When
//        scheduledJob.lockFees();
//
//        // Then
//        verify(feeRepository, times(1)).findByDueDateBeforeAndHasEverExpiredFalseAndIsDraftFalse(any());
//        verify(feeRepository, times(1)).findFeesWithSuccessfulPaymentsButNotLocked();
//        verify(feeRepository, never()).saveAll(any()); // Should NOT save anything
//    }
//
//    @Test
//    void testOnApplicationReady_shouldRunLockCheck() {
//        // Given
//        when(feeRepository.findByDueDateBeforeAndHasEverExpiredFalseAndIsDraftFalse(any()))
//                .thenReturn(Collections.emptyList());
//        when(feeRepository.findFeesWithSuccessfulPaymentsButNotLocked())
//                .thenReturn(Collections.emptyList());
//
//        // When
//        scheduledJob.onApplicationReady();
//
//        // Then
//        verify(feeRepository, times(1)).findByDueDateBeforeAndHasEverExpiredFalseAndIsDraftFalse(any());
//        verify(feeRepository, times(1)).findFeesWithSuccessfulPaymentsButNotLocked();
//    }
//}
//
