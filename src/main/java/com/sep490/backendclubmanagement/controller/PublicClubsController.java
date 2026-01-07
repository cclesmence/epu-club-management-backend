package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.response.PageResp;
import com.sep490.backendclubmanagement.dto.response.PublicClubCardDTO;
import com.sep490.backendclubmanagement.dto.response.PublicClubDetailDTO;
import com.sep490.backendclubmanagement.service.club.club.PublicClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/public/clubs")
@RequiredArgsConstructor
public class PublicClubsController {

    private final PublicClubService publicClubService;

    @GetMapping
    public ApiResponse<PageResp<PublicClubCardDTO>> getClubs(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        PageResp<PublicClubCardDTO> data =
                publicClubService.list(q, campusId, categoryId, page, size);
        return ApiResponse.success(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<PublicClubDetailDTO> getClubDetail(
            @PathVariable Long id,
            @RequestParam(value = "expand", required = false) String expand) {
        return ApiResponse.success(publicClubService.detail(id, expand));
    }

}
