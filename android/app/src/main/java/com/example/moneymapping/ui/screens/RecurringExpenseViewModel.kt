package com.example.moneymapping.ui.screens

import android.app.Application                                        // needed to get the app context for TokenManager and notifications
import android.app.NotificationManager                                // the system service used to send notifications
import android.content.Context                                        // needed to access system services and SharedPreferences
import androidx.core.app.NotificationCompat                           // builds notifications in a backwards-compatible way
import androidx.lifecycle.AndroidViewModel                            // base class that gives us access to the app context
import androidx.lifecycle.viewModelScope                              // coroutine scope that cancels automatically when the ViewModel is destroyed
import com.example.moneymapping.R                                    // needed to reference drawable resources like the app icon
import com.example.moneymapping.data.TokenManager                    // handles reading the access token from DataStore
import com.example.moneymapping.network.CreateExpenseRequest         // the request body used to auto-create a personal expense on the due date
import com.example.moneymapping.network.CreateRecurringExpenseRequest // the request body sent when creating a recurring expense
import com.example.moneymapping.network.RecurringExpenseResponse     // the response model for a recurring expense
import com.example.moneymapping.network.RetrofitClient               // our configured Retrofit instance for making API calls
import com.example.moneymapping.network.UpdateRecurringExpenseRequest // the request body sent when editing a recurring expense
import kotlinx.coroutines.flow.MutableStateFlow                      // a flow that holds a value and emits updates whenever it changes
import kotlinx.coroutines.flow.StateFlow                             // the read-only version exposed to the UI
import kotlinx.coroutines.launch                                     // launches a coroutine for background work
import java.text.SimpleDateFormat                                     // formats dates as strings
import java.util.Calendar                                            // used to calculate due dates and day comparisons
import java.util.Locale                                              // used when creating the date formatter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

// ─── Notification constants ────────────────────────────────────────────────────

private const val RECURRING_CHANNEL_ID   = "recurring_expense_channel"  // unique channel ID for recurring expense notifications
private const val RECURRING_CHANNEL_NAME = "Recurring Expense Reminders" // human-readable name shown in Android system settings
private const val PREFS_NAME             = "recurring_executions"        // SharedPreferences file name — used to track which expenses have been auto-executed today

// ─── ViewModel ────────────────────────────────────────────────────────────────

class RecurringExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)              // creates TokenManager using app context to read the access token
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // formats dates as "yyyy-MM-dd" — same format used everywhere in the app
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) // reads and writes execution tracking data to local storage

    private val _recurringExpenses = MutableStateFlow<List<RecurringExpenseResponse>>(emptyList()) // holds the full list of recurring expenses
    val recurringExpenses: StateFlow<List<RecurringExpenseResponse>> = _recurringExpenses           // read-only version exposed to the UI

    private val _upcomingExpenses = MutableStateFlow<List<RecurringExpenseResponse>>(emptyList()) // holds only the expenses due in the next 7 days
    val upcomingExpenses: StateFlow<List<RecurringExpenseResponse>> = _upcomingExpenses            // read-only version exposed to the HomeScreen card

    private val _errorMessage = MutableStateFlow<String?>(null)      // holds an error message to show in the UI — null means no error
    val errorMessage: StateFlow<String?> = _errorMessage             // read-only version exposed to the UI

    private val notificationsSent = mutableSetOf<Long>()             // tracks which recurring expense IDs have already triggered a notification this session
    private val _expensesNeedRefresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expensesNeedRefresh: SharedFlow<Unit> = _expensesNeedRefresh

    init {
        createNotificationChannel()                                   // registers the notification channel before any notification is sent
        fetchRecurringExpenses()                                      // loads recurring expenses as soon as the ViewModel is created
    }

    // fetches all recurring expenses, filters upcoming ones, auto-executes due ones, and schedules notifications
    fun fetchRecurringExpenses() {
        viewModelScope.launch {                                        // launches a background coroutine so the UI doesn't freeze
            try {
                val accessToken = tokenManager.getAccessToken()       // reads the stored access token from DataStore
                    ?: return@launch                                   // exits silently if no token
                val items = RetrofitClient.create(getApplication())
                    .getRecurringExpenses("Bearer $accessToken")      // calls GET /recurring-expenses
                _recurringExpenses.value = items                      // updates the full list state
                _upcomingExpenses.value = filterUpcoming(items)       // filters to only those due in the next 7 days
                autoExecuteDueExpenses(items, accessToken)            // auto-creates personal expenses for any that are due today
                scheduleNotifications(items)                          // checks if any notifications need to be sent
            } catch (e: Exception) {
                _errorMessage.value = "Could not load recurring expenses: ${e.message}" // shows the error in the UI
            }
        }
    }

    // saves a new recurring expense via POST /recurring-expenses
    fun addRecurringExpense(
        name: String,               // the name e.g. "Netflix"
        amount: Double,             // the amount per period e.g. 9.99
        currency: String,           // the currency e.g. "USD"
        category: String,           // the category e.g. "Entertainment"
        frequency: String,          // "DAILY", "WEEKLY", or "MONTHLY"
        dayOfMonth: Int? = null,    // only provided when frequency is MONTHLY
        dayOfWeek: Int? = null      // only provided when frequency is WEEKLY — 1=Monday 7=Sunday
    ) {
        viewModelScope.launch {                                        // launches a background coroutine
            try {
                val accessToken = tokenManager.getAccessToken()       // reads the stored access token
                    ?: return@launch                                   // exits silently if no token
                RetrofitClient.create(getApplication())
                    .createRecurringExpense(                          // calls POST /recurring-expenses
                        "Bearer $accessToken",
                        CreateRecurringExpenseRequest(
                            name = name,
                            amount = amount,
                            currency = currency,
                            category = category,
                            frequency = frequency,
                            dayOfMonth = dayOfMonth,                  // null if not MONTHLY
                            dayOfWeek = dayOfWeek                     // null if not WEEKLY
                        )
                    )
                fetchRecurringExpenses()                              // refreshes the list so the new item appears immediately
            } catch (e: Exception) {
                _errorMessage.value = "Could not add recurring expense: ${e.message}"
            }
        }
    }

    // updates an existing recurring expense via PUT /recurring-expenses/{id}
    fun updateRecurringExpense(
        id: Long,                   // the ID of the expense to update
        name: String,               // updated name
        amount: Double,             // updated amount
        currency: String,           // updated currency
        category: String,           // updated category
        frequency: String,          // updated frequency
        dayOfMonth: Int? = null,    // updated day of month — null if not MONTHLY
        dayOfWeek: Int? = null      // updated day of week — null if not WEEKLY
    ) {
        viewModelScope.launch {                                        // launches a background coroutine
            try {
                val accessToken = tokenManager.getAccessToken()       // reads the stored access token
                    ?: return@launch                                   // exits silently if no token
                RetrofitClient.create(getApplication())
                    .updateRecurringExpense(                          // calls PUT /recurring-expenses/{id}
                        "Bearer $accessToken",
                        id,
                        UpdateRecurringExpenseRequest(
                            name = name,
                            amount = amount,
                            currency = currency,
                            category = category,
                            frequency = frequency,
                            dayOfMonth = dayOfMonth,
                            dayOfWeek = dayOfWeek
                        )
                    )
                fetchRecurringExpenses()                              // refreshes the list so the updated item reflects immediately
            } catch (e: Exception) {
                _errorMessage.value = "Could not update recurring expense: ${e.message}"
            }
        }
    }

    // deletes a recurring expense by ID via DELETE /recurring-expenses/{id}
    fun deleteRecurringExpense(id: Long) {
        viewModelScope.launch {                                        // launches a background coroutine
            try {
                val accessToken = tokenManager.getAccessToken()       // reads the stored access token
                    ?: return@launch                                   // exits silently if no token
                RetrofitClient.create(getApplication())
                    .deleteRecurringExpense("Bearer $accessToken", id) // calls DELETE /recurring-expenses/{id}
                fetchRecurringExpenses()                              // refreshes the list so the deleted item disappears
            } catch (e: Exception) {
                _errorMessage.value = "Could not delete recurring expense: ${e.message}"
            }
        }
    }

    // clears the current error message — called after the UI has shown it
    fun clearError() {
        _errorMessage.value = null                                    // resets the error state so the message disappears
    }

    // ─── Auto-execution ───────────────────────────────────────────────────────

    // checks each recurring expense — if due today and not yet executed, creates a personal expense automatically
    private suspend fun autoExecuteDueExpenses(items: List<RecurringExpenseResponse>, accessToken: String) {
        val today = Calendar.getInstance()                            // gets today's date
        val todayStr = dateFormat.format(today.time)                  // formats today as "yyyy-MM-dd" e.g. "2025-04-19"

        items.forEach { expense ->
            val executionKey = "executed_${expense.id}_$todayStr"    // unique key per expense per day — prevents double execution
            if (prefs.getBoolean(executionKey, false)) return@forEach // already executed today — skip this one

            if (!isDueToday(expense, today)) return@forEach          // not due today based on frequency — skip

            try {
                RetrofitClient.create(getApplication())
                    .createExpense(                                    // calls POST /expenses to create a real personal expense
                        "Bearer $accessToken",
                        CreateExpenseRequest(
                            amount = expense.amount,                  // uses the recurring expense amount
                            currency = expense.currency,              // uses the recurring expense currency
                            description = expense.name,              // uses the recurring expense name as the description
                            category = expense.category,              // uses the recurring expense category
                            date = todayStr,                          // sets today as the expense date
                            groupId = null,                           // always personal — never linked to a group
                            isOneTimeSplit = false                    // not a split — this is a solo personal expense
                        )
                    )
                prefs.edit().putBoolean(executionKey, true).apply()  // marks as executed today so it won't run again until tomorrow
                _expensesNeedRefresh.tryEmit(Unit)                   // signals HomeScreen to re-fetch expenses
            } catch (e: Exception) {
                // silently fails — will retry the next time the app opens today
            }
        }
    }

    // returns true if the given recurring expense is due today based on its frequency
    private fun isDueToday(expense: RecurringExpenseResponse, today: Calendar): Boolean {
        return when (expense.frequency) {
            "DAILY"   -> true                                         // daily expenses are always due today
            "WEEKLY"  -> {
                val myDow = expense.dayOfWeek ?: return false        // skips if dayOfWeek is not set
                val calDow = if (myDow == 7) Calendar.SUNDAY else myDow + 1 // converts 1=Monday...7=Sunday to Java Calendar convention
                today.get(Calendar.DAY_OF_WEEK) == calDow            // true if today matches the due day of the week
            }
            "MONTHLY" -> {
                val dom = expense.dayOfMonth ?: return false         // skips if dayOfMonth is not set
                today.get(Calendar.DAY_OF_MONTH) == dom              // true if today's date matches the due day of the month
            }
            else -> false                                             // unknown frequency — never auto-execute
        }
    }

    // ─── Upcoming filter ──────────────────────────────────────────────────────

    // returns only recurring expenses due within the next 7 days — used by the HomeScreen card
    private fun filterUpcoming(items: List<RecurringExpenseResponse>): List<RecurringExpenseResponse> {
        val today = Calendar.getInstance()
        val todayDow = today.get(Calendar.DAY_OF_WEEK)               // today's day of week in Calendar convention
        val todayDom = today.get(Calendar.DAY_OF_MONTH)              // today's day of month e.g. 14
        val daysInMonth = today.getActualMaximum(Calendar.DAY_OF_MONTH) // total days in the current month e.g. 30

        return items.filter { expense ->
            when (expense.frequency) {
                "DAILY"   -> true                                     // daily expenses are always upcoming
                "WEEKLY"  -> {
                    val myDow = expense.dayOfWeek ?: return@filter false
                    val calDow = if (myDow == 7) Calendar.SUNDAY else myDow + 1 // converts to Calendar convention
                    val daysUntil = (calDow - todayDow + 7) % 7      // calculates how many days until the next occurrence
                    daysUntil in 0..7                                 // keeps only those due within 7 days
                }
                "MONTHLY" -> {
                    val dom = expense.dayOfMonth ?: return@filter false
                    val daysUntil = if (dom >= todayDom) dom - todayDom // due later this month
                    else (daysInMonth - todayDom) + dom               // due next month — wraps around
                    daysUntil in 0..7                                 // keeps only those due within 7 days
                }
                else -> false                                         // unknown frequency — never show as upcoming
            }
        }
    }

    // ─── Notifications ────────────────────────────────────────────────────────

    // registers the notification channel with Android — must be done before sending any notification
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return // channels only exist on API 26+
        val channel = android.app.NotificationChannel(
            RECURRING_CHANNEL_ID,
            RECURRING_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT                    // shows in the notification shade with sound
        ).apply {
            description = "Reminds you one day before a recurring expense is due"
        }
        val manager = getApplication<Application>()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)                    // safe to call multiple times — Android ignores duplicates
    }

    // sends a notification for any recurring expense due exactly tomorrow
    private fun scheduleNotifications(items: List<RecurringExpenseResponse>) {
        val today = Calendar.getInstance()
        val todayDow = today.get(Calendar.DAY_OF_WEEK)               // today's day of week in Calendar convention
        val todayDom = today.get(Calendar.DAY_OF_MONTH)              // today's day of month
        val daysInMonth = today.getActualMaximum(Calendar.DAY_OF_MONTH) // total days in the current month

        items.forEach { expense ->
            if (notificationsSent.contains(expense.id)) return@forEach // already sent this session — skip

            val isDueTomorrow = when (expense.frequency) {
                "DAILY"   -> true                                     // daily — always remind the day before (which is every day)
                "WEEKLY"  -> {
                    val myDow = expense.dayOfWeek ?: return@forEach
                    val calDow = if (myDow == 7) Calendar.SUNDAY else myDow + 1
                    val daysUntil = (calDow - todayDow + 7) % 7
                    daysUntil == 1                                    // true only if due exactly tomorrow
                }
                "MONTHLY" -> {
                    val dom = expense.dayOfMonth ?: return@forEach
                    val daysUntil = if (dom >= todayDom) dom - todayDom
                    else (daysInMonth - todayDom) + dom
                    daysUntil == 1                                    // true only if due exactly tomorrow
                }
                else -> false
            }

            if (isDueTomorrow) {
                sendNotification(expense)                             // sends the reminder notification
                notificationsSent.add(expense.id)                    // marks as sent so it doesn't fire again this session
            }
        }
    }

    // sends a single reminder notification for the given recurring expense
    @androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    private fun sendNotification(expense: RecurringExpenseResponse) {
        val context = getApplication<Application>()                   // gets the app context

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS       // checks if the user granted notification permission on Android 13+
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return                                      // exits silently if permission was not granted
        }

        val notification = NotificationCompat.Builder(context, RECURRING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)          // the small icon shown in the status bar
            .setContentTitle("Upcoming: ${expense.name}")            // e.g. "Upcoming: Netflix"
            .setContentText("${expense.currency} ${"%.2f".format(expense.amount)} is due tomorrow") // e.g. "USD 9.99 is due tomorrow"
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)         // standard priority — shows in shade with sound
            .setAutoCancel(true)                                      // dismisses the notification when the user taps it
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(expense.id.toInt(), notification)              // uses the expense ID as the notification ID so each expense gets its own notification
    }
}