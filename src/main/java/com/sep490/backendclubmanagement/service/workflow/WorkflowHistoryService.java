package com.sep490.backendclubmanagement.service.workflow;

import com.sep490.backendclubmanagement.entity.club.ClubCreationStep;
import com.sep490.backendclubmanagement.entity.club.ClubCreationWorkFlowHistory;
import com.sep490.backendclubmanagement.entity.RequestEstablishment;
import com.sep490.backendclubmanagement.entity.User;
import com.sep490.backendclubmanagement.repository.ClubCreationStepRepository;
import com.sep490.backendclubmanagement.repository.ClubCreationWorkFlowHistoryRepository;
import com.sep490.backendclubmanagement.repository.RequestEstablishmentRepository;
import com.sep490.backendclubmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowHistoryService {

    private final RequestEstablishmentRepository requestEstablishmentRepository;
    private final UserRepository userRepository;
    private final ClubCreationStepRepository clubCreationStepRepository;
    private final ClubCreationWorkFlowHistoryRepository workflowHistoryRepository;

    public void createWorkflowHistory(Long requestId, Long userId, String stepCode, String comments) {

        try {
            RequestEstablishment requestEntity = requestEstablishmentRepository.getReferenceById(requestId);

            ClubCreationStep step = clubCreationStepRepository.findByCode(stepCode)
                    .orElseThrow(() -> {
                        return new RuntimeException("ClubCreationStep not found: " + stepCode);
                    });

            User actedBy = userRepository.getReferenceById(userId);

            ClubCreationWorkFlowHistory history = ClubCreationWorkFlowHistory.builder()
                    .requestEstablishment(requestEntity)
                    .clubCreationStep(step)
                    .actedBy(actedBy)
                    .actionDate(LocalDateTime.now())
                    .comments(comments)
                    .build();

            ClubCreationWorkFlowHistory savedHistory = workflowHistoryRepository.save(history);
            log.info("Successfully created workflow history with id: {} for request {} with step {}",
                    savedHistory.getId(), requestId, stepCode);
        } catch (Exception e) {
            log.error("Error creating workflow history: {}", e.getMessage(), e);
            throw e;
        }
    }
}

