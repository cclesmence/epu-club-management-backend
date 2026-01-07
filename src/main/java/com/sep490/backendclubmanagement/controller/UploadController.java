// src/main/java/.../controller/UploadController.java
package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.service.file.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping("/image")
    public ApiResponse<CloudinaryService.UploadResult> uploadImage(@RequestParam("file") MultipartFile file) {
        var result = cloudinaryService.uploadImage(file);
        return ApiResponse.success(result);
    }

    @PostMapping("/file")
    public ApiResponse<CloudinaryService.UploadResult> uploadFile(@RequestParam("file") MultipartFile file) {
        var result = cloudinaryService.uploadFile(file);
        return ApiResponse.success(result);
    }
}
