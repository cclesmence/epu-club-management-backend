package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.club.ClubRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubRoleRepository extends JpaRepository<ClubRole, Long> {
    List<ClubRole> findByClubId(Long clubId);

   // check xem code role và role nam có trùng nhau k ( trong 1 clb)
    boolean existsByClubIdAndRoleCodeIgnoreCase(Long clubId, String roleCode);

    boolean existsByClubIdAndRoleNameIgnoreCase(Long clubId, String roleName);

    // check xem 2 club cùng code/name role vs nhau khac clubid
    boolean existsByClubIdAndRoleCodeIgnoreCaseAndIdNot(Long clubId, String roleCode, Long id);

    boolean existsByClubIdAndRoleNameIgnoreCaseAndIdNot(Long clubId, String roleName, Long id);
    Optional<ClubRole> findByRoleName(String roleName);
    Optional<ClubRole> findByClubIdAndRoleCode(Long clubId, String roleCode);
    Optional<ClubRole> findByRoleCodeAndClubIsNull(String roleCode);
}

