package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.club.ClubWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClubWalletRepository extends JpaRepository<ClubWallet, Long> {
    Optional<ClubWallet> findByClub_Id(Long clubId);

    boolean existsByClub_Id(Long clubId);

    @Query("SELECT COUNT(cw) FROM ClubWallet cw WHERE cw.deletedAt IS NULL")
    long countActiveWallets();
}



