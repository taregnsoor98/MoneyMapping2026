package com.example.moneymapping.ui.screens

import android.annotation.SuppressLint                               // suppresses lint warnings where we handle the logic manually
import android.app.Application                                        // needed to get the app context for TokenManager and notifications
import android.app.NotificationManager                                // the system service used to send notifications directly — avoids lint issues from NotificationManagerCompat
import android.content.Context                                        // needed to access system services like NotificationManager
import androidx.core.app.NotificationCompat                           // builds notifications in a backwards-compatible way across Android versions
import androidx.lifecycle.AndroidViewModel                            // base class that gives us access to the app context
import androidx.lifecycle.viewModelScope                              // coroutine scope that cancels automatically when the ViewModel is destroyed
import com.example.moneymapping.R                                    // needed to reference drawable resources like the app icon
import com.example.moneymapping.data.CurrencyPreferenceManager       // manages the user's home and local currency preferences
import com.example.moneymapping.data.TokenManager                    // handles reading the access token from DataStore
import com.example.moneymapping.network.ExpenseResponse              // the expense data model returned by the backend
import com.example.moneymapping.network.RetrofitClient               // our configured Retrofit instance for making API calls
import com.example.moneymapping.network.SetLimitRequest              // the request body we send when saving a limit
import com.example.moneymapping.network.SpendingLimitResponse        // the limit data model returned by the backend
import kotlinx.coroutines.flow.MutableStateFlow                      // a flow that holds a value and emits updates whenever it changes
import kotlinx.coroutines.flow.SharingStarted                        // controls when the flow starts collecting
import kotlinx.coroutines.flow.StateFlow                             // the read-only version of MutableStateFlow — exposed to the UI
import kotlinx.coroutines.flow.stateIn                               // converts a Flow into a StateFlow so the UI can observe it
import kotlinx.coroutines.launch                                     // launches a coroutine for background work without blocking the UI
import com.example.moneymapping.network.AddCreditRequest             // the request body sent when adding a new credit
import com.example.moneymapping.network.CreditResponse               // the response model for a credit entry

// ─── Notification constants ────────────────────────────────────────────────────

private const val CHANNEL_ID = "spending_limit_channel"   // unique string ID for our notification channel — used when building notifications
private const val CHANNEL_NAME = "Spending Limit Alerts"  // human-readable name shown to the user in Android system notification settings
private const val NOTIF_ID_75 = 1001                      // unique integer ID for the 75% warning notification — prevents it from replacing other notifications
private const val NOTIF_ID_90 = 1002                      // unique integer ID for the 90% warning notification — separate ID so both can show at the same time

// ─── ViewModel ────────────────────────────────────────────────────────────────

class HomeViewModel(application: Application) : AndroidViewModel(application) { // AndroidViewModel gives us app context without leaking Activity

    private val tokenManager = TokenManager(application)             // creates TokenManager using app context so it can read from DataStore
    private val currencyManager = CurrencyPreferenceManager(application) // reads the user's saved currency preferences from DataStore

    private val _expensesState = MutableStateFlow<ExpensesState>(ExpensesState.Loading) // starts in Loading state until the first fetch completes
    val expensesState: StateFlow<ExpensesState> = _expensesState                        // read-only version exposed to the UI — UI observes this

    private val _limitState = MutableStateFlow<LimitState>(LimitState.Loading) // starts in Loading state — changes to NotSet or Set after fetch
    val limitState: StateFlow<LimitState> = _limitState                        // read-only version exposed to the UI

    private val _spentAmount = MutableStateFlow(0.0)                 // holds the total amount spent in the current period — recalculated whenever expenses load
    val spentAmount: StateFlow<Double> = _spentAmount                // read-only version exposed to the UI for display in the card

    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap()) // holds the latest exchange rates — empty until first fetch
    val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates              // read-only version exposed to the UI

    private val _groupShareExpenses = MutableStateFlow<List<ExpenseResponse>>(emptyList()) // holds the user's share from each group expense — kept separate from personal expenses so the UI lists stay clean
    private val _credits = MutableStateFlow<List<CreditResponse>>(emptyList()) // holds the list of credits fetched from the backend
    val credits: StateFlow<List<CreditResponse>> = _credits                     // read-only version exposed to the UI

    // observes the home currency from DataStore — updates automatically when the user changes it in Profile
    val homeCurrency: StateFlow<String> = currencyManager.homeCurrencyFlow
        .stateIn(
            scope = viewModelScope,                                   // tied to this ViewModel's lifecycle
            started = SharingStarted.WhileSubscribed(5000),          // keeps collecting while the UI is active
            initialValue = CurrencyPreferenceManager.DEFAULT_HOME_CURRENCY // shown immediately before DataStore responds
        )

    // observes the local currency from DataStore — updates automatically when the user changes it in Profile
    val localCurrency: StateFlow<String> = currencyManager.localCurrencyFlow
        .stateIn(
            scope = viewModelScope,                                   // tied to this ViewModel's lifecycle
            started = SharingStarted.WhileSubscribed(5000),          // keeps collecting while the UI is active
            initialValue = CurrencyPreferenceManager.DEFAULT_LOCAL_CURRENCY // shown immediately before DataStore responds
        )

    private val notificationsSent = mutableSetOf<Int>()             // tracks which notification IDs have already been sent — prevents duplicate pings in the same session

    init {
        createNotificationChannel()                                  // registers the notification channel with the system — must happen before any notification is sent
        fetchAll()                                                   // loads expenses, limit, and exchange rates as soon as the ViewModel is created
    }

    // fetches expenses, the personal limit, and exchange rates — called on init and whenever the screen is revisited
    fun fetchAll() {
        fetchExpenses()                                              // starts the expense fetch coroutine
        fetchPersonalLimit()                                         // starts the limit fetch coroutine
        fetchExchangeRates()                                         // starts the exchange rate fetch coroutine
        fetchCredits()                                                       // starts the credits fetch coroutine
    }

    // ─── Expenses ─────────────────────────────────────────────────────────────

    // fetches personal expenses and the user's paid share of group expenses, then recalculates the spent total
    fun fetchExpenses() {
        viewModelScope.launch {                                                        // launches a background coroutine so the UI doesn't freeze
            _expensesState.value = ExpensesState.Loading                              // shows the loading spinner while the request is in flight
            try {
                val accessToken = tokenManager.getAccessToken()                       // reads the stored access token from DataStore
                    ?: run {
                        _expensesState.value = ExpensesState.Error("Session expired. Please log in again.") // no token means the session has ended
                        return@launch                                                  // exits the coroutine early — nothing more to do
                    }
                val api = RetrofitClient.create(getApplication())                     // creates one API instance reused for all calls below

                val personalExpenses = api.getExpenses("Bearer $accessToken")        // calls GET /expenses — returns only the user's own personal expenses
                _expensesState.value = ExpensesState.Success(personalExpenses)       // updates the UI with personal expenses right away

                val username = api.getMe("Bearer $accessToken").username             // calls GET /account/me to get the logged-in user's username for payer matching

                val groups = api.getGroups("Bearer $accessToken")                    // calls GET /groups — returns all groups the user belongs to

                val groupShares = mutableListOf<ExpenseResponse>()                   // will hold one synthetic entry per group expense the user personally paid into

                for (group in groups) {                                               // loops through every group the user is a member of
                    val groupExpenses = api.getGroupExpenses(                        // calls GET /expenses/group/{groupId} for this specific group
                        "Bearer $accessToken",
                        group.id
                    )
                    for (expense in groupExpenses) {                                  // loops through every expense recorded in this group
                        val myShare = expense.payers                                  // looks at the payers list on this expense
                            .find { it.payerName == username }                       // finds the entry where the payer name matches the logged-in user
                            ?.amountPaid                                              // reads how much the user personally paid toward this expense
                            ?: continue                                               // skips this expense entirely if the user did not pay into it
                        groupShares.add(expense.copy(amount = myShare))              // adds a copy of the expense with the full amount replaced by just the user's share
                    }
                }

                _groupShareExpenses.value = groupShares                              // stores the collected group shares so fetchCredits() can also use them when recalculating
                recalculateSpent(personalExpenses + groupShares)                     // recalculates the spent total using personal expenses and group shares combined
            } catch (e: Exception) {
                _expensesState.value = ExpensesState.Error("Could not load expenses: ${e.message}") // shows the error in the UI
            }
        }
    }

    // deletes a single expense by its ID then refreshes the list
    fun deleteExpense(id: String) {
        viewModelScope.launch {                                               // launches a background coroutine
            try {
                val accessToken = tokenManager.getAccessToken()              // reads the stored access token
                    ?: return@launch                                          // exits silently if no token — user will see stale data
                RetrofitClient.create(getApplication())
                    .deleteExpense("Bearer $accessToken", id)                // calls DELETE /expenses/{id}
                fetchExpenses()                                               // refreshes the list so the deleted item disappears
            } catch (e: Exception) {
                _expensesState.value = ExpensesState.Error("Could not delete expense: ${e.message}") // shows the error in the UI
            }
        }
    }

    // ─── Personal Limit ───────────────────────────────────────────────────────

    // fetches the personal spending limit from GET /limits/personal
    fun fetchPersonalLimit() {
        viewModelScope.launch {                                               // launches a background coroutine
            _limitState.value = LimitState.Loading                           // shows loading state while the request is in flight
            try {
                val accessToken = tokenManager.getAccessToken()              // reads the stored access token
                    ?: run {
                        _limitState.value = LimitState.Error("Session expired.") // no token — session has ended
                        return@launch                                         // exits the coroutine early
                    }
                val limit = RetrofitClient.create(getApplication())
                    .getPersonalLimit("Bearer $accessToken")                 // calls GET /limits/personal
                _limitState.value = LimitState.Set(limit)                   // a limit exists — store it and show the card
                checkAndNotify(limit, _spentAmount.value)                    // checks if we need to fire a warning notification
            } catch (e: Exception) {
                _limitState.value = LimitState.NotSet                       // a 404 or any error here means no limit is set yet — normal for new users
            }
        }
    }

    // saves a new or updated personal limit via POST /limits/personal
    fun savePersonalLimit(amount: Double, period: String) {
        viewModelScope.launch {                                               // launches a background coroutine
            try {
                val accessToken = tokenManager.getAccessToken()              // reads the stored access token
                    ?: return@launch                                          // exits silently if no token
                val request = SetLimitRequest(amount = amount, period = period, currency = currencyManager.getHomeCurrency()) // builds the request body with the user's home currency
                RetrofitClient.create(getApplication())
                    .setPersonalLimit("Bearer $accessToken", request)        // calls POST /limits/personal
                notificationsSent.clear()                                    // clears sent notifications so thresholds can trigger again with the new limit
                fetchPersonalLimit()                                         // refreshes the limit state so the card updates immediately
            } catch (e: Exception) {
                _limitState.value = LimitState.Error("Could not save limit: ${e.message}") // shows the error in the UI
            }
        }
    }

    // ─── Credits ──────────────────────────────────────────────────────────────

    // fetches all credits for the logged-in user from GET /credits
    fun fetchCredits() {
        viewModelScope.launch {                                               // launches a background coroutine so the UI doesn't freeze
            try {
                val accessToken = tokenManager.getAccessToken()              // reads the stored access token from DataStore
                    ?: return@launch                                          // exits silently if no token
                val credits = RetrofitClient.create(getApplication())
                    .getCredits("Bearer $accessToken")                       // calls GET /credits with the auth header
                _credits.value = credits                                     // updates the state with the fetched credits
                recalculateSpent(                                            // recalculates spent now that we have fresh credit data
                    ((_expensesState.value as? ExpensesState.Success)?.expenses ?: emptyList()) + _groupShareExpenses.value // combines personal expenses and group shares so credits are subtracted from the full total
                )
            } catch (e: Exception) {
                // silently fails — credits are optional, the card still works without them
            }
        }
    }

    // saves a new credit entry via POST /credits — called when the user taps "Add to score"
    fun addCredit(amount: Double, comment: String, currency: String) {
        viewModelScope.launch {                                               // launches a background coroutine
            try {
                val accessToken = tokenManager.getAccessToken()              // reads the stored access token from DataStore
                    ?: return@launch                                          // exits silently if no token
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date())                                // formats today's date as "yyyy-MM-dd"
                RetrofitClient.create(getApplication())
                    .addCredit(                                              // calls POST /credits with the new credit data
                        "Bearer $accessToken",
                        AddCreditRequest(amount = amount, comment = comment, date = today, currency = currency)
                    )
                fetchCredits()                                               // refreshes credits so the card updates immediately
            } catch (e: Exception) {
                // silently fails — user can try again
            }
        }
    }

    // ─── Exchange Rates ───────────────────────────────────────────────────────

    // fetches exchange rates from the backend using the user's saved home currency as the base
    fun fetchExchangeRates() {
        viewModelScope.launch {                                               // launches a background coroutine
            try {
                val base = currencyManager.getHomeCurrency()                 // reads the saved home currency e.g. "USD"
                val rates = RetrofitClient.create(getApplication())
                    .getExchangeRates(base)                                  // calls GET /exchange-rates?base=USD
                _exchangeRates.value = rates                                 // updates the state with the fetched rates
            } catch (e: Exception) {
                // silently fails — if rates can't be fetched, the UI will just not show the converted amount
            }
        }
    }

    // ─── Spent calculation ────────────────────────────────────────────────────

    // filters the loaded expenses to the current period and sums their amounts
    private fun recalculateSpent(expenses: List<ExpenseResponse>) {
        val limitSet = (_limitState.value as? LimitState.Set) ?: return
        val period = limitSet.limit.period
        val limitCurrency = limitSet.limit.currency.takeIf { it.isNotEmpty() } ?: homeCurrency.value

        val today = java.util.Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayStr = dateFormat.format(today.time)

        val startOfWeek = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfWeekStr = dateFormat.format(startOfWeek.time)
        val currentMonth = today.get(java.util.Calendar.MONTH)
        val currentYear = today.get(java.util.Calendar.YEAR)

        val rates = _exchangeRates.value

        val spent = expenses.filter { expense ->
            val expenseDateStr = expense.date
            val expenseCalendar = try {
                java.util.Calendar.getInstance().apply {
                    time = dateFormat.parse(expenseDateStr)!!
                }
            } catch (e: Exception) {
                return@filter false
            }
            when (period) {
                "DAILY"   -> expenseDateStr == todayStr
                "WEEKLY"  -> expenseDateStr >= startOfWeekStr && expenseDateStr <= todayStr
                "MONTHLY" -> expenseCalendar.get(java.util.Calendar.MONTH) == currentMonth
                        && expenseCalendar.get(java.util.Calendar.YEAR) == currentYear
                else      -> false
            }
        }.sumOf { expense ->
            convertToLimitCurrency(expense.amount, expense.currency, limitCurrency, rates)
        }

        val totalCredits = _credits.value.sumOf { credit ->
            val creditCurrency = credit.currency.ifEmpty { limitCurrency } // falls back to limit currency if no currency stored
            convertToLimitCurrency(credit.amount, creditCurrency, limitCurrency, rates) // converts credit to limit currency before subtracting
        }

        _spentAmount.value = spent - totalCredits

        (_limitState.value as? LimitState.Set)?.let {
            checkAndNotify(it.limit, spent)
        }
    }

    // converts an amount from one currency to another using the fetched exchange rates
    private fun convertToLimitCurrency(amount: Double, fromCurrency: String, toCurrency: String, rates: Map<String, Double>): Double {
        if (fromCurrency == toCurrency) return amount
        if (rates.isEmpty()) return amount
        // rates are relative to the home currency base
        val fromRate = rates[fromCurrency] ?: return amount
        val toRate = rates[toCurrency] ?: return amount
        return amount / fromRate * toRate
    }

    // ─── Notifications ────────────────────────────────────────────────────────

    // registers the notification channel with Android — must be done before sending any notification
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return // notification channels only exist on API 26+, skip on older devices
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,                                                      // unique ID used when building notifications to target this channel
            CHANNEL_NAME,                                                    // displayed in the Android system notification settings screen
            NotificationManager.IMPORTANCE_DEFAULT                           // shows in the notification shade with sound but no heads-up popup
        ).apply {
            description = "Alerts you when you are approaching your spending limit" // shown in system settings under the channel
        }
        val manager = getApplication<Application>()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager // gets the system NotificationManager
        manager.createNotificationChannel(channel)                           // registers the channel — safe to call multiple times, Android ignores duplicates
    }

    // checks the current usage percentage and triggers notifications at 75% and 90%
    private fun checkAndNotify(limit: SpendingLimitResponse, spent: Double) {
        if (limit.amount <= 0) return                                        // guards against division by zero if the limit was somehow saved as 0
        val usagePercent = (spent / limit.amount) * 100                     // calculates what percentage of the limit has been used e.g. 80.0

        sendThresholdNotification(                                           // checks and potentially sends the 75% warning
            usagePercent = usagePercent,
            threshold = 75,
            notifId = NOTIF_ID_75,
            title = "Heads up! 🟡",
            message = "You've used 75% of your ${limit.period.lowercase()} budget."
        )

        sendThresholdNotification(                                           // checks and potentially sends the 90% warning
            usagePercent = usagePercent,
            threshold = 90,
            notifId = NOTIF_ID_90,
            title = "Almost at your limit! 🔴",
            message = "You've used 90% of your ${limit.period.lowercase()} budget."
        )
    }

    // sends a notification only if the threshold is crossed and it hasn't been sent yet this session
    @SuppressLint("MissingPermission", "NotificationPermission") // permission is requested in MainActivity and checked inside this function before notify is called
    private fun sendThresholdNotification(
        usagePercent: Double, // the current usage as a percentage e.g. 80.0
        threshold: Int,       // the percentage threshold to check against e.g. 75
        notifId: Int,         // unique notification ID — used to identify this specific notification
        title: String,        // the bold title line shown in the notification
        message: String       // the body text shown below the title
    ) {
        if (usagePercent < threshold) return                                 // threshold not reached yet — nothing to send
        if (notificationsSent.contains(notifId)) return                     // already sent this notification this session — don't spam the user

        val context = getApplication<Application>()                          // gets the app context needed to build the notification

        // on Android 13+, check if the user granted POST_NOTIFICATIONS before sending
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission( // checks the runtime permission status
                context,
                android.Manifest.permission.POST_NOTIFICATIONS               // the permission required to post notifications on Android 13+
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED        // true only if the user explicitly granted it
            if (!granted) return                                              // exits silently if permission was not granted — no notification sent
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)  // starts building the notification for our channel
            .setSmallIcon(R.drawable.ic_launcher_foreground)                 // the small icon shown in the status bar
            .setContentTitle(title)                                          // the bold title line
            .setContentText(message)                                         // the body text
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)               // standard priority — shows in shade with sound
            .setAutoCancel(true)                                             // automatically dismisses the notification when the user taps it
            .build()                                                         // finalizes the notification object

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager // gets the system NotificationManager directly — avoids NotificationManagerCompat lint errors
        manager.notify(notifId, notification)                                // sends the notification to the system
        notificationsSent.add(notifId)                                       // marks this notification as sent so we don't send it again this session
    }
}

// ─── State classes ────────────────────────────────────────────────────────────

// represents all possible states of the expenses fetch
sealed class ExpensesState {
    object Loading : ExpensesState()                                          // the fetch is currently in progress
    data class Success(val expenses: List<ExpenseResponse>) : ExpensesState() // expenses loaded successfully
    data class Error(val message: String) : ExpensesState()                   // the fetch failed — message explains why
}

// represents all possible states of the personal spending limit
sealed class LimitState {
    object Loading : LimitState()                                             // the fetch is currently in progress
    object NotSet : LimitState()                                              // the user has not set a limit yet
    data class Set(val limit: SpendingLimitResponse) : LimitState()           // a limit exists and is ready to display
    data class Error(val message: String) : LimitState()                      // the fetch or save failed
}