package com.sep490.backendclubmanagement.service.club.club;

import com.sep490.backendclubmanagement.repository.RoleMemberShipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClubAccessService {
    private final RoleMemberShipRepository roleMembershipRepository;

    public boolean amOfficer(Long userId, Long clubId) {
        return roleMembershipRepository.isUserClubOfficer(userId, clubId);
    }
}