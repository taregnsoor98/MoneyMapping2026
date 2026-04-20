package com.example.moneymapping.worker

import android.content.Context                                        // needed to access WorkManager
import androidx.work.ExistingPeriodicWorkPolicy                       // controls what happens if the work is already scheduled
import androidx.work.PeriodicWorkRequestBuilder                       // builds a repeating work request
import androidx.work.WorkManager                                      // the system service that manages background work
import java.util.concurrent.TimeUnit                                  // used to specify the repeat interval unit

// unique name for this scheduled work — used to avoid scheduling it multiple times
private const val WORK_NAME = "installment_reminder_work"            // WorkManager uses this to identify and deduplicate the work

// schedules the InstallmentReminderWorker to run once every 24 hours
fun scheduleInstallmentReminders(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<InstallmentReminderWorker>(
        24, TimeUnit.HOURS                                            // runs once every 24 hours
    ).build()                                                         // finalizes the work request

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WORK_NAME,                                                    // unique name prevents duplicate scheduling
        ExistingPeriodicWorkPolicy.KEEP,                              // if already scheduled, keep the existing one — don't reset the timer
        workRequest                                                   // the work request to schedule
    )
}