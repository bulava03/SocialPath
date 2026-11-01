package com.socialpath.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * A pretend operator: nothing leaves the server. Every message is written to
 * the log and, when the demo inbox is on, kept in memory so that the
 * /dev/sms page can show it the way a phone would. Only the latest messages
 * are kept, and they are gone after a restart, like an operator's delivery
 * that was never read.
 */
@Component
public class DemoSmsGateway implements SmsGateway {

    /** How many messages the inbox keeps; older ones drop off. */
    static final int CAPACITY = 500;

    private static final Logger log = LoggerFactory.getLogger(DemoSmsGateway.class);

    private final boolean inboxEnabled;
    private final Deque<SmsMessage> messages = new ArrayDeque<>();

    public DemoSmsGateway(@Value("${app.sms.demo-inbox:false}") boolean inboxEnabled) {
        this.inboxEnabled = inboxEnabled;
    }

    @Override
    public void send(String phone, String text) {
        log.info("SMS to {}: {}", phone, text);
        if (!inboxEnabled) {
            return;
        }
        synchronized (messages) {
            messages.addFirst(new SmsMessage(phone, text, LocalDateTime.now()));
            while (messages.size() > CAPACITY) {
                messages.removeLast();
            }
        }
    }

    /**
     * Whether the inbox page is available.
     * @return the app.sms.demo-inbox setting
     */
    public boolean isInboxEnabled() {
        return inboxEnabled;
    }

    /**
     * The kept messages, newest first.
     * @param phone only messages to this number (E.164), or null for all
     * @return the messages
     */
    public List<SmsMessage> inbox(String phone) {
        synchronized (messages) {
            return messages.stream()
                    .filter(message -> phone == null || message.phone().equals(phone))
                    .toList();
        }
    }
}
