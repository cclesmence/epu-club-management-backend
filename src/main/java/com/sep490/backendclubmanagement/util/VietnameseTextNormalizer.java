package com.sep490.backendclubmanagement.util;

import java.text.Normalizer;

/**
 * Utility class để normalize text tiếng Việt
 * Loại bỏ dấu để hỗ trợ tìm kiếm không phân biệt dấu
 */
public class VietnameseTextNormalizer {

    // Private constructor to prevent instantiation
    private VietnameseTextNormalizer() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Loại bỏ dấu tiếng Việt khỏi text
     * Ví dụ: "Nguyễn Văn A" -> "nguyen van a"
     *
     * @param text Text cần normalize
     * @return Text không dấu, lowercase
     */
    public static String removeDiacritics(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Normalize Unicode (NFD - Canonical Decomposition)
        // Ví dụ: "ễ" -> "e" + combining tilde
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);

        // Loại bỏ các combining diacritical marks (các dấu)
        // \p{InCombiningDiacriticalMarks} matches các ký tự dấu Unicode
        String withoutDiacritics = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Xử lý các trường hợp đặc biệt của tiếng Việt
        withoutDiacritics = withoutDiacritics
                .replace("Đ", "D")
                .replace("đ", "d");

        // Convert to lowercase để so sánh case-insensitive
        return withoutDiacritics.toLowerCase();
    }

    /**
     * Kiểm tra xem text có chứa searchTerm không (không phân biệt dấu và hoa thường)
     *
     * @param text Text cần kiểm tra
     * @param searchTerm Từ khóa tìm kiếm
     * @return true nếu text chứa searchTerm
     */
    public static boolean containsIgnoreDiacritics(String text, String searchTerm) {
        if (text == null || searchTerm == null) {
            return false;
        }

        String normalizedText = removeDiacritics(text);
        String normalizedSearch = removeDiacritics(searchTerm);

        return normalizedText.contains(normalizedSearch);
    }

    /**
     * Check xem một trong các fields có match với searchTerm không
     *
     * @param searchTerm Từ khóa tìm kiếm
     * @param fields Các fields cần check
     * @return true nếu có ít nhất 1 field match
     */
    public static boolean matchesAny(String searchTerm, String... fields) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return true; // No search term = match all
        }

        for (String field : fields) {
            if (field != null && containsIgnoreDiacritics(field, searchTerm)) {
                return true;
            }
        }

        return false;
    }
}

