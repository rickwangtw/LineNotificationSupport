package com.mysticwind.linenotificationsupport.identicalmessage;

import com.mysticwind.linenotificationsupport.model.LineNotification;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IgnoreIdenticalMessageHandlerTest {

    private static final LineNotification NOTIFICATION_1 = LineNotification.builder().build();
    private static final LineNotification NOTIFICATION_2 = LineNotification.builder().build();

    @Mock
    private IdenticalMessageEvaluator evaluator;

    private IgnoreIdenticalMessageHandler classUnderTest;

    @Before
    public void setUp() {
        classUnderTest = new IgnoreIdenticalMessageHandler(evaluator);
    }

    @Test
    public void testHandleNoDuplicate() {
        when(evaluator.evaluate(any(LineNotification.class), anyInt())).thenReturn(IdenticalMessageEvaluator.EvaluationResult.noDuplicate());

        Optional<Pair<LineNotification, Integer>> result = classUnderTest.handle(NOTIFICATION_2, 1);
        assertEquals(NOTIFICATION_2, result.get().getLeft());
        assertEquals(1, result.get().getRight().intValue());
    }

    @Test
    public void testHandleIsDuplicate() {
        when(evaluator.evaluate(any(LineNotification.class), anyInt()))
                .thenReturn(IdenticalMessageEvaluator.EvaluationResult.withDuplicate(NOTIFICATION_1, 1, 2));

        Optional<Pair<LineNotification, Integer>> result = classUnderTest.handle(NOTIFICATION_2, 2);
        assertFalse(result.isPresent());
    }

}