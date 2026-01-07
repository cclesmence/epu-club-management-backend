package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.OutcomeTransaction;
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
public interface OutcomeTransactionRepository extends JpaRepository<OutcomeTransaction, Long>, JpaSpecificationExecutor<OutcomeTransaction> {

    /**
     * Find outcome transaction by transaction code
     */
    Optional<OutcomeTransaction> findByTransactionCode(String transactionCode);

    /**
     * Check if transaction code already exists
     */
    boolean existsByTransactionCode(String transactionCode);

    /**
     * Find all outcome transactions by club wallet
     */
    @Query("SELECT o FROM OutcomeTransaction o WHERE o.clubWallet.id = :clubWalletId")
    Page<OutcomeTransaction> findByClubWalletId(@Param("clubWalletId") Long clubWalletId, Pageable pageable);

    /**
     * Find outcome transactions by club wallet and status
     */
    @Query("SELECT o FROM OutcomeTransaction o WHERE o.clubWallet.id = :clubWalletId AND o.status = :status")
    Page<OutcomeTransaction> findByClubWalletIdAndStatus(
            @Param("clubWalletId") Long clubWalletId,
            @Param("status") TransactionStatus status,
            Pageable pageable);
}

