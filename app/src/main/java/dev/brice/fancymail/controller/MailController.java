package dev.brice.fancymail.controller;

import dev.brice.fancymail.model.MailPath;
import dev.brice.fancymail.model.ParsedMail;
import dev.brice.fancymail.service.MailService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Body;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.views.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;

/**
 * Controller for the web UI and rendered mail views.
 */
@Controller
@ExecuteOn(TaskExecutors.BLOCKING)
public class MailController {

    private static final Logger LOG = LoggerFactory.getLogger(MailController.class);

    private final MailService mailService;

    public MailController(MailService mailService) {
        this.mailService = mailService;
    }

    /**
     * Home page with URL input form.
     */
    @Get("/")
    @View("index")
    public Map<String, Object> index() {
        return Map.of(
                "title", "Fancy Mail - OpenJDK Mailing List Beautifier"
        );
    }

    /**
     * Handle form submission - redirect to rendered view.
     */
    @Post("/go")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public HttpResponse<?> go(@Body Map<String, String> form) {
        String url = form.get("url");
        if (url == null || url.isBlank()) {
            return HttpResponse.badRequest("URL is required");
        }

        try {
            MailPath mailPath = MailPath.fromUrl(url.trim());
            return HttpResponse.redirect(URI.create(mailPath.toRenderedPath()));
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid URL submitted: {}", url);
            return HttpResponse.badRequest("Invalid OpenJDK mailing list URL");
        }
    }

    /**
     * Render a mail by its path components.
     */
    @Get("/rendered/{list}/{yearMonth}/{id}.html")
    @View("rendered")
    public Map<String, Object> rendered(
            @PathVariable String list,
            @PathVariable String yearMonth,
            @PathVariable String id) {

        LOG.info("Rendering mail: {}/{}/{}", list, yearMonth, id);

        try {
            ParsedMail mail = mailService.getMail(list, yearMonth, id);
            return Map.of(
                    "title", mail.subject(),
                    "mail", mail,
                    "list", list,
                    "yearMonth", yearMonth,
                    "id", id
            );
        } catch (Exception e) {
            LOG.error("Error rendering mail: {}/{}/{}", list, yearMonth, id, e);
            return Map.of(
                    "title", "Error",
                    "error", "Failed to fetch or parse mail: " + e.getMessage()
            );
        }
    }

    /**
     * Get raw markdown for a mail.
     */
    @Get("/markdown/{list}/{yearMonth}/{id}.md")
    @Produces(MediaType.TEXT_PLAIN)
    public String markdown(
            @PathVariable String list,
            @PathVariable String yearMonth,
            @PathVariable String id) {

        MailPath mailPath = new MailPath(list, yearMonth, id);
        return mailService.getMailAsMarkdown(mailPath);
    }
}
