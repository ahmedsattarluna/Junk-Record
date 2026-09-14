package com.junklog.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Notifier.createChannel(context);
        Notifier.show(context, "Junk Log", "Anything to log for today?");
        // Alarms are one-shot so they survive doze — queue up tomorrow's.
        Scheduler.rescheduleFromPrefs(context);
    }
}
