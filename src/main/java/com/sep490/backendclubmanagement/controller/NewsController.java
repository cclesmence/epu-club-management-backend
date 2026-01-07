package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.NewsRequest;
import com.sep490.backendclubmanagement.dto.response.NewsData;
import com.sep490.backendclubmanagement.dto.response.NewsResponse;
import com.sep490.backendclubmanagement.dto.response.PagedResponse;
import com.sep490.backendclubmanagement.service.admin.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/news")
@RequiredArgsConstructor
public class NewsController {
    private final NewsService newsService;

    @PostMapping("/get-all-by-filter")
    public ApiResponse<NewsResponse> getAllEventsByFilter(@RequestBody NewsRequest request) {
          return ApiResponse.success(newsService.getAllNewsByFilter(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<NewsData> getNewsById(@PathVariable Long id) {
        return ApiResponse.success(newsService.getNewsById(id));
    }

    /**
     * Lấy danh sách tin tức đã được publish của một câu lạc bộ với phân trang và tìm kiếm
     */
    @GetMapping("/clubs/{clubId}/published")
    public ApiResponse<PagedResponse<NewsData>> getPublishedNewsByClubId(
            @PathVariable Long clubId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = createPageable(page, size, sort);
        return ApiResponse.success(newsService.getPublishedNewsByClubId(clubId, keyword, pageable));
    }

    /**
     * Helper method to create Pageable from sort string
     */
    private Pageable createPageable(int page, int size, String sortStr) {
        String[] sortParams = sortStr.split(",");
        String property = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}
