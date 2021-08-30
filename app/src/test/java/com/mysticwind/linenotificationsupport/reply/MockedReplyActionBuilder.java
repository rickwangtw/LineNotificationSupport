package com.mysticwind.linenotificationsupport.reply;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.os.Bundle;
import android.os.Parcelable;

import org.mockito.Mockito;

public class MockedReplyActionBuilder implements ReplyActionBuilder {

    private static final String KEY = "key";

    @Override
    public Notification.Action buildReplyAction(String chatId, Notification.Action originalLineReplyAction) {
        final Notification.Action action = Mockito.mock(Notification.Action.class);
        // an indirect way to test if it is storing the original LINE action
        final Bundle bundle = Mockito.mock(Bundle.class);
        when(bundle.getParcelable(KEY)).thenReturn(originalLineReplyAction);
        when(action.getExtras()).thenReturn(bundle);
        return action;
    }

    public static void validateAction(final Notification.Action expectedAction, final Notification.Action actualAction) {
        final Parcelable parcelable = actualAction.getExtras().getParcelable(KEY);
        assertEquals(expectedAction, parcelable);
    }

}
