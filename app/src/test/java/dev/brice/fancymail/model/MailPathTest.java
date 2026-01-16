package dev.brice.fancymail.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class MailPathTest {

    @Test
    void fromUrl_validHttpsUrl_parsesCorrectly() {
        String url = "https://mail.openjdk.org/pipermail/amber-spec-experts/2026-January/004307.html";

        MailPath mailPath = MailPath.fromUrl(url);

        assertThat(mailPath.list()).isEqualTo("amber-spec-experts");
        assertThat(mailPath.yearMonth()).isEqualTo("2026-January");
        assertThat(mailPath.id()).isEqualTo("004307");
    }

    @Test
    void fromUrl_validHttpUrl_parsesCorrectly() {
        String url = "http://mail.openjdk.org/pipermail/core-libs-dev/2025-December/123456.html";

        MailPath mailPath = MailPath.fromUrl(url);

        assertThat(mailPath.list()).isEqualTo("core-libs-dev");
        assertThat(mailPath.yearMonth()).isEqualTo("2025-December");
        assertThat(mailPath.id()).isEqualTo("123456");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com/something",
            "https://mail.openjdk.org/pipermail/",
            "https://mail.openjdk.org/pipermail/list/2026-January/",
            "not-a-url",
            ""
    })
    void fromUrl_invalidUrl_throwsException(String invalidUrl) {
        assertThatThrownBy(() -> MailPath.fromUrl(invalidUrl))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromPath_validPath_parsesCorrectly() {
        String path = "amber-spec-experts/2026-January/004307.html";

        MailPath mailPath = MailPath.fromPath(path);

        assertThat(mailPath.list()).isEqualTo("amber-spec-experts");
        assertThat(mailPath.yearMonth()).isEqualTo("2026-January");
        assertThat(mailPath.id()).isEqualTo("004307");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "invalid/path",
            "list/month/notanumber.html",
            "just-a-string"
    })
    void fromPath_invalidPath_throwsException(String invalidPath) {
        assertThatThrownBy(() -> MailPath.fromPath(invalidPath))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toOriginalUrl_returnsCorrectUrl() {
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004307");

        String url = mailPath.toOriginalUrl();

        assertThat(url).isEqualTo("https://mail.openjdk.org/pipermail/amber-spec-experts/2026-January/004307.html");
    }

    @Test
    void toRenderedPath_returnsCorrectPath() {
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004307");

        String path = mailPath.toRenderedPath();

        assertThat(path).isEqualTo("/rendered/amber-spec-experts/2026-January/004307.html");
    }

    @Test
    void toCacheKey_returnsCorrectKey() {
        MailPath mailPath = new MailPath("amber-spec-experts", "2026-January", "004307");

        String key = mailPath.toCacheKey();

        assertThat(key).isEqualTo("amber-spec-experts/2026-January/004307");
    }

    @Test
    void roundTrip_fromUrlToOriginalUrl_preservesUrl() {
        String originalUrl = "https://mail.openjdk.org/pipermail/valhalla-dev/2025-March/000123.html";

        MailPath mailPath = MailPath.fromUrl(originalUrl);
        String reconstructedUrl = mailPath.toOriginalUrl();

        assertThat(reconstructedUrl).isEqualTo(originalUrl);
    }
}
