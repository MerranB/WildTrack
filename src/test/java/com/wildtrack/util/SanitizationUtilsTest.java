package com.wildtrack.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SanitizationUtilsTest {

    @Test
    void sanitizeString_returnsNull_whenInputIsNull() {
        assertThat(SanitizationUtils.sanitizeString(null)).isNull();
    }

    @Test
    void sanitizeString_returnsTrimmedString_whenUnderLimit() {
        assertThat(SanitizationUtils.sanitizeString("  hello  ")).isEqualTo("hello");
    }

    @Test
    void sanitizeString_truncates_whenOverLimit() {
        String input = "a".repeat(300);
        assertThat(SanitizationUtils.sanitizeString(input)).hasSize(255);
    }

    @Test
    void sanitizeString_stripsHtml() {
        assertThat(SanitizationUtils.sanitizeString("<b>bold</b>")).isEqualTo("bold");
    }

    @Test
    void sanitizeUserPrompt_returnsNull_whenInputIsNull() {
        assertThat(SanitizationUtils.sanitizeUserPrompt(null)).isNull();
    }

    @Test
    void sanitizeUserPrompt_returnsTrimmedString_whenUnderLimit() {
        assertThat(SanitizationUtils.sanitizeUserPrompt("  hello  ")).isEqualTo("hello");
    }

    @Test
    void sanitizeUserPrompt_truncates_whenOverLimit() {
        String input = "a".repeat(600);
        assertThat(SanitizationUtils.sanitizeUserPrompt(input)).hasSize(500);
    }

    @Test
    void sanitizeUserPrompt_stripsHtml() {
        assertThat(SanitizationUtils.sanitizeUserPrompt("<script>alert('x')</script>hello")).isEqualTo("alert('x')hello");
    }
}
