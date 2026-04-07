package com.example.hobbymasterytracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Show the notification
        NotificationHelper.showReminder(context);
        // Reschedule for next day
        ReminderScheduler.scheduleDaily(context);
    }
}
