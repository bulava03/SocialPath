package com.socialpath.sms;

/**
 * Sends text messages to phones. The app talks to the operator only through
 * this interface; the one implementation is {@link DemoSmsGateway}, which
 * stands in for a real operator.
 */
public interface SmsGateway {

    /**
     * Sends a message.
     * @param phone the number in E.164
     * @param text the message text
     */
    void send(String phone, String text);
}
