package com.example.moneymapping.ui.expense

// Step 4 of the Add Expense wizard
// Shows the list of items in the expense. Each item has a name, unit price, quantity, and total price.
// If the user chose to scan in Step 1, they can take a photo or pick from gallery here.
// The image is converted to base64 and sent to the backend, where Claude reads it and extracts the items.
// The user can add, edit or delete items before moving to Step 5.

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.moneymapping.data.TokenManager
import com.example.moneymapping.network.RetrofitClient
import com.example.moneymapping.network.ScanReceiptRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun StepFour(viewModel: ExpenseViewModel) { // receives the shared ViewModel

    val context = LocalContext.current                        // needed for camera and file access
    val items by viewModel.items.collectAsState()             // observes the current items list
    val isScanning by viewModel.isScanning.collectAsState()   // true if user chose to scan in Step 1

    // Tracks whether the add item form is visible
    var showAddForm by remember { mutableStateOf(false) }

    // Tracks the fields in the add item form
    var newItemName by remember { mutableStateOf("") }
    var newItemUnitPrice by remember { mutableStateOf("") }
    var newItemQuantity by remember { mutableStateOf("1") }

    // Tracks the scan status message
    var scanStatus by remember { mutableStateOf("") }

    // Holds the URI of the photo taken by the camera
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Converts the image URI to base64 and sends it to the backend for Claude to process
    fun processReceiptImage(uri: Uri) {
        scanStatus = "Sending receipt to Claude..." // shows processing status
        MainScope().launch {
            try {
                // Reads the image bytes from the URI
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: run {
                        scanStatus = "Could not open image. Please try again." // failed to open image
                        return@launch
                    }
                val imageBytes = inputStream.readBytes() // reads all image bytes
                inputStream.close()                      // closes the stream

                // Converts the image bytes to a base64 string
                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP) // encodes to base64

                // Determines the media type from the URI
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg" // defaults to jpeg if unknown

                val accessToken = TokenManager(context).getAccessToken() // gets the stored token
                    ?: run {
                        scanStatus = "Session expired. Please log in again." // no token found
                        return@launch
                    }

                // Sends the base64 image to the backend
                val jsonString = RetrofitClient.authApi.scanReceipt(
                    "Bearer $accessToken",
                    ScanReceiptRequest(
                        base64Image = base64Image, // the base64 encoded image
                        mediaType = mimeType       // the image mime type
                    )
                )

                // Parses the JSON array returned by the backend into a list of ScannedItem objects
                val type = object : TypeToken<List<ScannedItem>>() {}.type
                val scannedItems: List<ScannedItem> = Gson().fromJson(jsonString, type) ?: emptyList()

                if (scannedItems.isNotEmpty()) {
                    scannedItems.forEach { scanned ->
                        viewModel.addItem(
                            ExpenseItem(
                                name = scanned.name,            // item name from Claude
                                unitPrice = scanned.unitPrice,  // unit price from Claude
                                quantity = scanned.quantity.toInt().coerceAtLeast(1),    // converts to int, minimum 1
                                totalPrice = scanned.totalPrice // total price from Claude
                            )
                        )
                    }
                    scanStatus = "Found ${scannedItems.size} item(s)! Please review and edit below." // success
                } else {
                    scanStatus = "Could not find any items. Please add them manually." // no items found
                }

                viewModel.addReceiptImages(listOf(uri.toString())) // saves the receipt image URI

            } catch (e: Exception) {
                scanStatus = "Failed to process receipt: ${e.message}" // shows error if something goes wrong
            }
        }
    }

    // Launcher for picking an image from the gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent() // opens the gallery picker
    ) { uri ->
        if (uri != null) {
            processReceiptImage(uri) // processes the picked image
        }
    }

    // Launcher for taking a photo with the camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture() // opens the camera
    ) { success ->
        if (success && cameraImageUri != null) {
            processReceiptImage(cameraImageUri!!) // processes the captured photo
        }
    }

    // Launcher for requesting camera permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission() // requests a single permission
    ) { granted ->
        if (granted) {
            val photoFile = File.createTempFile("receipt_", ".jpg", context.cacheDir) // creates temp file for photo
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
            cameraImageUri = uri        // saves the URI
            cameraLauncher.launch(uri) // opens the camera
        } else {
            scanStatus = "Camera permission denied. Please use gallery instead." // permission was denied
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // allows scrolling if list is long
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) // spacing between elements
    ) {

        Text(
            text = "Expense Items",
            style = MaterialTheme.typography.titleLarge // large title style
        )

        // Shows scan buttons only if user chose to scan in Step 1
        if (isScanning) {

            Text(
                text = "Scan your receipt and Claude will fill the items automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant // lighter description color
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp) // spacing between buttons
            ) {

                // Button to take a photo with the camera
                Button(
                    onClick = {
                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED // checks if permission is already granted

                        if (hasCameraPermission) {
                            val photoFile = File.createTempFile("receipt_", ".jpg", context.cacheDir) // creates temp file
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
                            cameraImageUri = uri        // saves the URI
                            cameraLauncher.launch(uri) // opens the camera
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA) // requests camera permission
                        }
                    },
                    modifier = Modifier.weight(1f) // takes half the row width
                ) {
                    Text("📷 Take Photo") // camera button label
                }

                // Button to pick an image from the gallery
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") }, // opens gallery for any image type
                    modifier = Modifier.weight(1f) // takes half the row width
                ) {
                    Text("🖼️ Pick from Gallery") // gallery button label
                }
            }

            // Shows the scan status message
            if (scanStatus.isNotEmpty()) {
                Text(
                    text = scanStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (scanStatus.startsWith("Could not") || scanStatus.startsWith("Camera permission") || scanStatus.startsWith("Failed") || scanStatus.startsWith("Session"))
                        MaterialTheme.colorScheme.error  // red for errors
                    else
                        MaterialTheme.colorScheme.primary // primary color for success
                )
            }
        }

        // Shows each item in the list as a card
        items.forEachIndexed { index, item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // subtle shadow
            ) {
                Column(modifier = Modifier.padding(12.dp)) {

                    // Item name field — editable
                    OutlinedTextField(
                        value = item.name,
                        onValueChange = { viewModel.updateItem(index, item.copy(name = it)) }, // updates name in ViewModel
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                        // Unit price field — editable
                        OutlinedTextField(
                            value = item.unitPrice.toString(),
                            onValueChange = {
                                val price = it.toDoubleOrNull() ?: 0.0 // converts to double safely
                                val total = price * item.quantity       // recalculates total
                                viewModel.updateItem(index, item.copy(unitPrice = price, totalPrice = total))
                            },
                            label = { Text("Unit Price") },
                            modifier = Modifier.weight(1f) // takes half the row width
                        )

                        // Quantity field — editable
                        OutlinedTextField(
                            value = item.quantity.toString(),
                            onValueChange = {
                                val qty = it.toIntOrNull() ?: 1       // converts to int safely
                                val total = item.unitPrice * qty       // recalculates total
                                viewModel.updateItem(index, item.copy(quantity = qty, totalPrice = total))
                            },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f) // takes half the row width
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shows the calculated total price for this item
                        Text(
                            text = "Total: ${item.totalPrice}",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        // Button to delete this item from the list
                        IconButton(onClick = { viewModel.removeItem(index) }) {
                            Text("🗑️") // trash icon for delete
                        }
                    }
                }
            }
        }

        // Shows the add item form when the "+" button is tapped
        if (showAddForm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {

                    // New item name field
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Item Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                        // New item unit price field
                        OutlinedTextField(
                            value = newItemUnitPrice,
                            onValueChange = { newItemUnitPrice = it },
                            label = { Text("Unit Price") },
                            modifier = Modifier.weight(1f)
                        )

                        // New item quantity field
                        OutlinedTextField(
                            value = newItemQuantity,
                            onValueChange = { newItemQuantity = it },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                        // Button to confirm adding the new item
                        Button(
                            onClick = {
                                val price = newItemUnitPrice.toDoubleOrNull() ?: 0.0 // converts price safely
                                val qty = newItemQuantity.toIntOrNull() ?: 1          // converts quantity safely
                                viewModel.addItem(
                                    ExpenseItem(
                                        name = newItemName,
                                        unitPrice = price,
                                        quantity = qty,
                                        totalPrice = price * qty // calculates total automatically
                                    )
                                )
                                newItemName = ""       // resets name field
                                newItemUnitPrice = ""  // resets price field
                                newItemQuantity = "1"  // resets quantity field
                                showAddForm = false    // hides the form after adding
                            },
                            modifier = Modifier.weight(1f),
                            enabled = newItemName.isNotEmpty() && newItemUnitPrice.isNotEmpty() // only enabled if fields are filled
                        ) {
                            Text("Add") // confirm add button label
                        }

                        // Button to cancel adding a new item
                        OutlinedButton(
                            onClick = { showAddForm = false }, // hides the form without adding
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel") // cancel button label
                        }
                    }
                }
            }
        }

        // Button to show the add item form
        OutlinedButton(
            onClick = { showAddForm = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Add Item") // label for add item button
        }

        // Next button to move to Step 5
        Button(
            onClick = { viewModel.nextStep() },
            modifier = Modifier.fillMaxWidth(),
            enabled = items.isNotEmpty() // only enabled if at least one item is added
        ) {
            Text("Next")
        }
    }
}

// Represents a single item returned by the backend after Claude reads the receipt
data class ScannedItem(
    val name: String,        // item name from Claude
    val quantity: Double,       // quantity from Claude - double to support weight-based items like 0.265 kg
    val unitPrice: Double,   // unit price from Claude
    val totalPrice: Double   // total price from Claude
)