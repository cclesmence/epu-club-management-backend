package com.sep490.backendclubmanagement.service.file;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class CloudinaryService {
    private final Cloudinary cloudinary;



    public UploadResult uploadImage(MultipartFile file) {
        try {
            var result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "club/posts",
                            "resource_type", "image",
                            "overwrite", false
                    )
            );
            return new UploadResult(
                    (String) result.get("secure_url"),
                    (String) result.get("public_id"),
                    (String) result.get("format"),
                    ((Number) result.get("bytes")).longValue()
            );
        } catch (Exception e) {
            throw new RuntimeException("Cloudinary upload fail: " + e.getMessage(), e);
        }
    }

    /**
     * Upload BẤT ĐỒNG BỘ: chạy trên thread pool 'uploadExecutor'
     * - Dùng cho upload song song nhiều ảnh trong PostService.
     * - Trả về CompletableFuture để caller .join()/.allOf() quản lý đồng bộ cuối cùng.
     */
    @Async("uploadExecutor")
    public CompletableFuture<UploadResult> uploadImageAsync(MultipartFile file) {
        try {
            // Tái sử dụng logic đồng bộ cho nhất quán
            UploadResult res = uploadImage(file);
            return CompletableFuture.completedFuture(res);
        } catch (Exception e) {
            // Đẩy lỗi ra future để phía gọi tự quyết định fail toàn bộ hay bỏ qua file lỗi
            return CompletableFuture.failedFuture(e);
        }
    }


    public UploadResult uploadImage(MultipartFile file, String folder) {
        try {
            var result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "overwrite", false
                    )
            );
            return new UploadResult(
                    (String) result.get("secure_url"),
                    (String) result.get("public_id"),
                    (String) result.get("format"),
                    ((Number) result.get("bytes")).longValue()
            );
        } catch (Exception e) {
            throw new RuntimeException("Cloudinary upload fail: " + e.getMessage(), e);
        }
    }

    /**
     * Upload file (PDF, DOC, DOCX, etc.) to Cloudinary
     * Uses default folder: club/recruitment/
     * @param file MultipartFile to upload
     * @return UploadResult with file URL and metadata
     */
    public UploadResult uploadFile(MultipartFile file) {
        return uploadFile(file, "club/recruitment");
    }

    /**
     * Upload file (PDF, DOC, DOCX, etc.) to Cloudinary with specified folder
     * @param file MultipartFile to upload
     * @param folder Folder path in Cloudinary (e.g., "club/reports", "club/recruitment")
     * @return UploadResult with file URL and metadata
     */
    public UploadResult uploadFile(MultipartFile file, String folder) {
        try {
            // Get original filename to preserve file extension
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new IllegalArgumentException("File must have a valid filename");
            }

            // Extract file extension to ensure proper format preservation
            String fileExtension = "";
            int lastDotIndex = originalFilename.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
                fileExtension = "." + originalFilename.substring(lastDotIndex + 1).toLowerCase();
            }

            // Generate a clean filename (remove special chars, keep extension)
            String baseFilename = originalFilename;
            if (lastDotIndex > 0) {
                baseFilename = originalFilename.substring(0, lastDotIndex);
            }
            // Clean filename: only allow alphanumeric, dash, underscore
            String cleanFilename = baseFilename.replaceAll("[^a-zA-Z0-9_-]", "_");

            // Generate timestamp to make filename unique
            long timestamp = System.currentTimeMillis();
            String publicId = folder + "/" + cleanFilename + "_" + timestamp + fileExtension;

            // Build upload parameters
            Map<String, Object> uploadParams = new HashMap<>();
            uploadParams.put("public_id", publicId); // Set explicit public_id with extension
            uploadParams.put("resource_type", "raw"); // Use 'raw' for non-image files
            uploadParams.put("overwrite", false);

            // Add content type if available to help Cloudinary identify file type
            String contentType = file.getContentType();
            if (contentType != null && !contentType.isEmpty()) {
                uploadParams.put("context", "content_type=" + contentType);
            }

            // Upload using byte array (safe for all file types)
            var result = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            return new UploadResult(
                    (String) result.get("secure_url"),
                    (String) result.get("public_id"),
                    (String) result.get("format"),
                    ((Number) result.get("bytes")).longValue()
            );
        } catch (Exception e) {
            throw new RuntimeException("Cloudinary upload fail: " + e.getMessage(), e);
        }
    }

    /**
     * Upload video file to Cloudinary
     * @param file MultipartFile video to upload
     * @param folder Folder path in Cloudinary
     * @return UploadResult with video URL and metadata
     */
    public UploadResult uploadVideo(MultipartFile file, String folder) {
        try {
            var result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "video",
                            "overwrite", false
                    )
            );
            return new UploadResult(
                    (String) result.get("secure_url"),
                    (String) result.get("public_id"),
                    (String) result.get("format"),
                    ((Number) result.get("bytes")).longValue());

        } catch (Exception e) {
            throw new RuntimeException("Cloudinary video upload fail: " + e.getMessage(), e);
        }
    }

    /**
     * Upload video file to Cloudinary (default folder)
     * @param file MultipartFile video to upload
     * @return UploadResult with video URL and metadata
     */
    public UploadResult uploadVideo(MultipartFile file) {
        return uploadVideo(file, "club/events");
    }

    public record UploadResult(String url, String publicId, String format, long bytes) {}
}
