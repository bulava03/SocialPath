package com.socialpath;

import com.socialpath.exception.ResourceNotFoundException;
import com.socialpath.sms.DemoSmsGateway;
import com.socialpath.sms.SmsMessage;
import com.socialpath.web.DemoSmsController;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The demo phone page: which messages it shows, and that it exists only when switched on. */
class DemoSmsControllerTest {

    private final DemoSmsGateway gateway = new DemoSmsGateway(true);
    private final DemoSmsController controller = new DemoSmsController(gateway);

    @Test
    void inbox_ShowsOnlyTheChosenNumber_EvenWhenThePlusArrivedAsASpace() {
        gateway.send("+12015550123", "SocialPath code: 123456.");
        gateway.send("+12015550199", "SocialPath code: 654321.");

        // An unencoded + in the address reaches the server as a space.
        for (String typed : List.of("+12015550123", " 12015550123", "+1 201-555-0123")) {
            ExtendedModelMap model = new ExtendedModelMap();

            assertEquals("dev/smsInbox", controller.inbox(typed, model));

            assertEquals("+1 201-555-0123", model.get("phoneShown"), typed);
            @SuppressWarnings("unchecked")
            List<SmsMessage> messages = (List<SmsMessage>) model.get("messages");
            assertEquals(List.of("SocialPath code: 123456."), messages.stream().map(SmsMessage::text).toList());
        }
    }

    @Test
    void inbox_SomethingThatIsNotANumber_ShowsItAsTypedWithNoMessages() {
        gateway.send("+12015550123", "SocialPath code: 123456.");
        ExtendedModelMap model = new ExtendedModelMap();

        controller.inbox("abc", model);

        assertEquals("abc", model.get("phoneShown"));
        assertEquals(List.of(), model.get("messages"));
    }

    @Test
    void inbox_Off_IsNotFound() {
        DemoSmsController off = new DemoSmsController(new DemoSmsGateway(false));

        assertThrows(ResourceNotFoundException.class, () -> off.inbox(null, new ExtendedModelMap()));
        assertThrows(ResourceNotFoundException.class, () -> off.messages(null));
    }
}
