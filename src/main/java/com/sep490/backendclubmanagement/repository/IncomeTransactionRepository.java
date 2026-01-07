package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.IncomeTransaction;
import com.sep490.backendclubmanagement.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IncomeTransactionRepository extends JpaRepository<IncomeTransaction, Long>, JpaSpecificationExecutor<IncomeTransaction> {
    
    /**
     * Find income transaction by reference (transaction code)
     */
    Optional<IncomeTransaction> findByReference(String reference);
    
    /**
     * Check if reference already exists
     */
    boolean existsByReference(String reference);
    boolean existsByUser_IdAndFee_IdAndStatus(Long userId, Long feeId, TransactionStatus status);

    /**
     * Find all income transactions by club wallet
     */
    @Query("SELECT i FROM IncomeTransaction i WHERE i.clubWallet.id = :clubWalletId")
    Page<IncomeTransaction> findByClubWalletId(@Param("clubWalletId") Long clubWalletId, Pageable pageable);

    /**
     * Find income transactions by club wallet and status
     */
    @Query("SELECT i FROM IncomeTransaction i WHERE i.clubWallet.id = :clubWalletId AND i.status = :status")
    Page<IncomeTransaction> findByClubWalletIdAndStatus(
            @Param("clubWalletId") Long clubWalletId,
            @Param("status") TransactionStatus status,
            Pageable pageable);

    /**
     * Find all income transactions by fee and status
     */
    @Query("SELECT i FROM IncomeTransaction i " +
           "LEFT JOIN FETCH i.user " +
           "WHERE i.fee.id = :feeId AND i.status = :status")
    java.util.List<IncomeTransaction> findByFee_IdAndStatus(
            @Param("feeId") Long feeId,
            @Param("status") TransactionStatus status);
}

