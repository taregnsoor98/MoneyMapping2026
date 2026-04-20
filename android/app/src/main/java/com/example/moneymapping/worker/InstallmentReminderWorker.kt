package com.example.moneymapping.worker

import android.app.NotificationChannel                                // used to create the notification channel on Android 8+
import android.app.NotificationManager                                // the system service used to send notifications
import android.content.Context                                        // needed to access system services and build notifications
import androidx.core.app.NotificationCompat                           // builds notifications in a backwards-compatible way
import androidx.work.CoroutineWorker                                  // base class for workers that use coroutines
import androidx.work.WorkerParameters                                 // provides configuration passed to the worker by WorkManager
import com.example.moneymapping.R                                    // needed to reference drawable resources like the app icon
import com.example.moneymapping.data.TokenManager                    // handles reading the access token from DataStore
import com.example.moneymapping.network.RetrofitClient               // our configured Retrofit instance for making API calls
import java.time.LocalDate                                            // used to get today's date and calculate tomorrow

// unique IDs and names for the notification channel used by this worker
private const val CHANNEL_ID = "installment_reminder_channel"        // unique string ID for the installment reminder channel
private const val CHANNEL_NAME = "Payment Reminders"                 // human-readable name shown in Android notification settings

// worker that checks all payment plans and sends a notification for any installment due tomorrow
class InstallmentReminderWorker(
    private val context: Context,                                     // app context needed to access system services
    workerParams: WorkerParameters                                    // configuration passed in by WorkManager
) : CoroutineWorker(context, workerParams) {                          // CoroutineWorker lets us use suspend functions inside doWork

    override suspend fun doWork(): Result {                           // called by WorkManager when it's time to run the worker
        return try {
            createNotificationChannel()                               // ensures the channel exists before sending any notification

            val tokenManager = TokenManager(context)                  // creates TokenManager to read the stored access token
            val token = tokenManager.getAccessToken()                 // reads the access token from DataStore
                ?: return Result.failure()                            // exits if no token — user is not logged in

            val tomorrow = LocalDate.now().plusDays(1).toString()     // calculates tomorrow's date as "yyyy-MM-dd" string

            val groups = RetrofitClient.create(context)
                .getGroups("Bearer $token")                           // fetches all groups the user belongs to

            groups.forEach { group ->
                val plans = RetrofitClient.create(context)
                    .getGroupPaymentPlans("Bearer $token", group.id)  // fetches all payment plans for this group

                plans.forEach { plan ->
                    plan.installments
                        .filter { !it.isPaid && it.dueDate == tomorrow } // keeps only unpaid installments due tomorrow
                        .forEach { installment ->
                            sendNotification(                         // sends a reminder notification for each matching installment
                                title = "Payment due tomorrow 💳",
                                message = "You have a payment of ${String.format("%.2f", installment.amount)} due tomorrow in group ${group.name}."
                            )
                        }
                }
            }

            Result.success()                                          // tells WorkManager the work completed successfully
        } catch (e: Exception) {
            Result.retry()                                            // tells WorkManager to retry if something went wrong
        }
    }

    // creates the notification channel — safe to call multiple times, Android ignores duplicates
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return // channels only exist on API 26+
        val channel = NotificationChannel(
            CHANNEL_ID,                                               // unique ID used when building notifications
            CHANNEL_NAME,                                             // shown in Android system notification settings
            NotificationManager.IMPORTANCE_DEFAULT                    // shows in the notification shade with sound
        ).apply {
            description = "Reminds you when a payment plan installment is due tomorrow" // shown in system settings
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)                    // registers the channel with the system
    }

    // builds and sends a single notification
    private fun sendNotification(title: String, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // on Android 13+, check if the user granted POST_NOTIFICATIONS permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS        // permission required to post notifications on Android 13+
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED  // true only if the user explicitly granted it
            if (!granted) return                                        // exits silently if permission was not granted
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)           // small icon shown in the status bar
            .setContentTitle(title)                                    // bold title line of the notification
            .setContentText(message)                                   // body text shown below the title
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)         // standard priority — shows in shade with sound
            .setAutoCancel(true)                                       // dismisses the notification when the user taps it
            .build()

        manager.notify(                                                // sends the notification to the system
            System.currentTimeMillis().toInt(),                        // uses current time as ID so multiple notifications don't replace each other
            notification
        )
    }
}