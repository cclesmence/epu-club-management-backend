package com.sep490.backendclubmanagement.service.admin;

import com.sep490.backendclubmanagement.dto.request.StaffUpdateNewsRequest;
import com.sep490.backendclubmanagement.dto.response.NewsData;

public interface StaffNewsAdminService {
    NewsData updateNews(Long id, StaffUpdateNewsRequest req);
    void hide(Long id);
    void unhide(Long id);
    void softDelete(Long id);
    void restore(Long id);
}
