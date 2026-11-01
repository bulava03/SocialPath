package com.socialpath.sms;

import com.socialpath.validation.PhoneNumbers;

import java.time.LocalDateTime;

/**
 * A message the demo operator "delivered".
 * @param phone the number in E.164
 * @param text the message text
 * @param sentAt when it was sent
 */
public record SmsMessage(String phone, String text, LocalDateTime sentAt) {

    /**
     * The number as the inbox page shows it.
     * @return e.g. "+1 201-555-0123"
     */
    public String phoneForDisplay() {
        return PhoneNumbers.formatInternational(phone);
    }
}
