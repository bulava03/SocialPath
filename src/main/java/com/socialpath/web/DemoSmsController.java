package com.socialpath.web;

import com.socialpath.exception.ResourceNotFoundException;
import com.socialpath.sms.DemoSmsGateway;
import com.socialpath.sms.SmsMessage;
import com.socialpath.validation.PhoneNumbers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The demo operator's inbox: every SMS the app "sent", as a phone would show
 * it, since no real message is sent. Open to anyone and so only for demo
 * and test setups: it exists only with app.sms.demo-inbox=true and answers
 * 404 otherwise.
 */
@Controller
@RequestMapping("/dev/sms")
@RequiredArgsConstructor
public class DemoSmsController {

    private final DemoSmsGateway gateway;

    /**
     * The inbox page.
     * @param phone only messages to this number, as typed (with +), or none for all
     * @param model the model for the page
     * @return the inbox view
     */
    @GetMapping
    public String inbox(@RequestParam(required = false) String phone, Model model) {
        String e164 = selected(phone);
        model.addAttribute("phone", phone);
        model.addAttribute("phoneShown", e164 == null ? null
                : PhoneNumbers.toE164(null, e164).map(PhoneNumbers::formatInternational).orElse(e164));
        model.addAttribute("messages", gateway.inbox(e164));
        return "dev/smsInbox";
    }

    /**
     * The same messages as JSON, for the browser tests.
     * @param phone only messages to this number, or none for all
     * @return the messages, newest first
     */
    @GetMapping("/messages")
    @ResponseBody
    public List<SmsMessage> messages(@RequestParam(required = false) String phone) {
        return gateway.inbox(selected(phone));
    }

    /**
     * The inbox filtered to one number.
     * @param e164 the number
     * @return the address of the page
     */
    static String inboxLink(String e164) {
        // The + has to be written as %2B: in a query a bare + reads as a space.
        return "/dev/sms?phone=" + URLEncoder.encode(e164, StandardCharsets.UTF_8);
    }

    private String selected(String phone) {
        if (!gateway.isInboxEnabled()) {
            throw new ResourceNotFoundException("The demo SMS inbox is off");
        }
        if (phone == null || phone.isBlank()) {
            return null;
        }
        // "+1 201..." pasted into an address arrives as " 1 201...": the + became a space.
        return PhoneNumbers.toE164(null, phone)
                .or(() -> PhoneNumbers.toE164(null, "+" + phone.strip()))
                .orElse(phone.strip());
    }
}
