package com.socialpath.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The test mail inbox (Mailpit's web page) that the Security page links to,
 * where letters go when no real mailbox is configured. Unset in a real
 * deployment, which hides the link.
 */
@Component
public class MailInbox {

    private final String url;

    public MailInbox(@Value("${app.mail.inbox-url:}") String url) {
        this.url = url == null || url.isBlank() ? null : url;
    }

    /**
     * Where the inbox is.
     * @return its address, or null when there is none to show
     */
    public String getUrl() {
        return url;
    }
}
