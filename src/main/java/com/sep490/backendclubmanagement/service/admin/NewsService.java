package com.sep490.backendclubmanagement.service.admin;

import com.sep490.backendclubmanagement.dto.request.NewsRequest;
import com.sep490.backendclubmanagement.dto.response.NewsData;
import com.sep490.backendclubmanagement.dto.response.NewsResponse;
import com.sep490.backendclubmanagement.entity.News;
import com.sep490.backendclubmanagement.exception.NotFoundException;
import com.sep490.backendclubmanagement.mapper.NewsMapper;
import com.sep490.backendclubmanagement.repository.NewsRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@Service
@AllArgsConstructor
public class NewsService {
    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;


    public NewsResponse getAllNewsByFilter(NewsRequest request) {
        final List<String> keywords = (request.getKeyword() != null && !request.getKeyword().isBlank())
                ? Arrays.stream(request.getKeyword().split(","))
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .toList()
                : List.of();

        // 2️⃣ Lấy dữ liệu từ repository (lọc theo các điều kiện cơ bản)
        Page<News> page = this.newsRepository.getAllNewsByFilter(request, request.getPageable());
        List<News> newsList = page.getContent();

        // 3️⃣ Nếu có keyword thì lọc tiếp ở tầng Java
        if (!keywords.isEmpty()) {
            newsList = newsList.stream()
                    .filter(news -> {
                        String title = normalizeVietnamese(news.getTitle() != null ? news.getTitle() : "");
                        String content = normalizeVietnamese(news.getContent() != null ? news.getContent() : "");
                        String type = normalizeVietnamese(news.getNewsType() != null ? news.getNewsType() : "");

                        // Ít nhất một keyword khớp với title, content hoặc news_type (normalize keyword trước khi so sánh)
                        return keywords.stream().anyMatch(kw -> {
                            String normalizedKw = normalizeVietnamese(kw);
                            return title.contains(normalizedKw) ||
                                    content.contains(normalizedKw) ||
                                    type.contains(normalizedKw);
                        });
                    })
                    .toList();
        }

        // 4️⃣ Map sang DTO
        List<NewsData> list = newsList.stream()
                .map(newsItem -> {
                    NewsData dto = newsMapper.toDto(newsItem);
                    dto.setClubId(newsItem.getClub() != null ? newsItem.getClub().getId() : null);
                    return dto;
                })
                .toList();

        // 5️⃣ Trả về kết quả
        return NewsResponse.builder()
                .total(page.getTotalElements())  // tổng số trong DB (chưa lọc keyword)
                .count(list.size())              // số kết quả sau khi lọc keyword
                .data(list)
                .build();
    }

    public NewsData getNewsById(Long id) {
        Optional<News> news  = this.newsRepository.findById(id);
        if(news.isEmpty()){
            throw new NotFoundException("News not found");
        }
        return newsMapper.toDto(news.get());
    }

    /**
     * Lấy danh sách tin tức đã được publish của một câu lạc bộ với phân trang và tìm kiếm
     */
    public com.sep490.backendclubmanagement.dto.response.PagedResponse<NewsData> getPublishedNewsByClubId(
            Long clubId, String keyword, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<News> newsPage = newsRepository.findPublishedNewsByClubId(clubId, keyword, pageable);

        org.springframework.data.domain.Page<NewsData> dataPage = newsPage.map(news -> {
            NewsData dto = newsMapper.toDto(news);
            dto.setClubId(news.getClub() != null ? news.getClub().getId() : null);
            return dto;
        });

        return com.sep490.backendclubmanagement.dto.response.PagedResponse.of(dataPage);
    }

    /**
     * Normalize Vietnamese text by removing diacritics (accents)
     * Example: "đọc sách" -> "doc sach"
     */
    private String normalizeVietnamese(String text) {
        if (text == null || text.isBlank()) return "";
        String normalized = text.replace("đ", "d").replace("Đ", "d");
        normalized = java.text.Normalizer.normalize(normalized, java.text.Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase();
    }
}


