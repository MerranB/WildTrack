package com.wildtrack.util;

public class SanitizationUtils {

        private static final int MAX_STRING_LENGTH = 255;
        private static final int MAX_PROMPT_LENGTH = 500;

        private SanitizationUtils() {}

        public static String sanitizeString(String input) {
            if (input == null) return null;
            String sanitized = stripHtml(input.trim());
            return sanitized.length() > MAX_STRING_LENGTH
                    ? sanitized.substring(0, MAX_STRING_LENGTH)
                    : sanitized;
        }

        public static String sanitizeUserPrompt(String input) {
            if (input == null) return null;
            String sanitized = stripHtml(input.trim());
            return sanitized.length() > MAX_PROMPT_LENGTH
                    ? sanitized.substring(0, MAX_PROMPT_LENGTH)
                    : sanitized;
        }

        private static String stripHtml(String input) {
            return input.replaceAll("<[^>]*>", "");
        }
}
