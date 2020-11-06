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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MergeIdenticalMessageHandlerTest {

    private static final String MESSAGE = "message";
    private static final String MERGED_MESSAGE = "message (2)";

    private static final LineNotification ORIGINAL_NOTIFICATION =
            LineNotification.builder()
                    .message(MESSAGE)
                    .build();
    private static final LineNotification MERGED_NOTIFICATION =
            LineNotification.builder()
                    .message(MERGED_MESSAGE)
                    .build();

    @Mock
    private IdenticalMessageEvaluator evaluator;

    private MergeIdenticalMessageHandler classUnderTest;

    @Before
    public void setUp() {
        classUnderTest = new MergeIdenticalMessageHandler(evaluator);
    }

    @Test
    public void testHandleNoDuplicate() {
        when(evaluator.evaluate(any(LineNotification.class), anyInt())).thenReturn(IdenticalMessageEvaluator.EvaluationResult.noDuplicate());

        Optional<Pair<LineNotification, Integer>> result = classUnderTest.handle(ORIGINAL_NOTIFICATION, 1);
        assertEquals(ORIGINAL_NOTIFICATION, result.get().getLeft());
        assertEquals(1, result.get().getRight().intValue());
    }

    @Test
    public void testHandleIsDuplicate() {
        when(evaluator.evaluate(any(LineNotification.class), anyInt()))
                .thenReturn(IdenticalMessageEvaluator.EvaluationResult.withDuplicate(ORIGINAL_NOTIFICATION, 1, 2));

        Optional<Pair<LineNotification, Integer>> result = classUnderTest.handle(ORIGINAL_NOTIFICATION, 2);
        assertTrue(result.isPresent());
        assertEquals(MERGED_NOTIFICATION, result.get().getKey());
        assertEquals(1, result.get().getRight().intValue());
    }

}