package com.mysticwind.linenotificationsupport.identicalmessage;

import com.mysticwind.linenotificationsupport.model.LineNotification;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class AsIsIdenticalMessageHandlerTest {

    private static final LineNotification NOTIFICATION_1 = LineNotification.builder().build();
    private static final LineNotification NOTIFICATION_2 = LineNotification.builder().build();

    private static final AsIsIdenticalMessageHandler CLASS_UNDER_TEST = new AsIsIdenticalMessageHandler();

    @Test
    public void testHandle() {
        Optional<Pair<LineNotification, Integer>> result1 = CLASS_UNDER_TEST.handle(NOTIFICATION_1, 1);
        assertEquals(NOTIFICATION_1, result1.get().getLeft());
        assertEquals(1, result1.get().getRight().intValue());

        Optional<Pair<LineNotification, Integer>> result2 = CLASS_UNDER_TEST.handle(NOTIFICATION_1, 2);
        assertEquals(NOTIFICATION_1, result2.get().getLeft());
        assertEquals(2, result2.get().getRight().intValue());

        Optional<Pair<LineNotification, Integer>> result3 = CLASS_UNDER_TEST.handle(NOTIFICATION_2, 3);
        assertEquals(NOTIFICATION_2, result3.get().getLeft());
        assertEquals(3, result3.get().getRight().intValue());
    }

}