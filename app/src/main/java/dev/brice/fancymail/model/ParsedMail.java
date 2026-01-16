package dev.brice.fancymail.model;

/**
 * Represents a parsed email from the OpenJDK mailing list.
 */
public record ParsedMail(
        String subject,
        String from,
        String email,
        String date,
        String list,
        String bodyMarkdown,
        String bodyHtml,
        String originalUrl
) {
}
