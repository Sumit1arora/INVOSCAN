package com.example.myapplication

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shashank.sony.fancytoastlib.FancyToast
import java.io.File
import java.io.OutputStreamWriter
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val storageDir = File(getExternalFilesDir(null), "Barcodes")
            if (!storageDir.exists()) storageDir.mkdirs()

            BarcodeAppWithBottomBar(storageDir)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeAppWithBottomBar(storageDir: File) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Save Barcode", "View Saved Data")

    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("BarcodeAppPrefs", Context.MODE_PRIVATE)

    // Load data from SharedPreferences
    val (barcodeData, totalQuantity) = remember {
        loadData(sharedPreferences)
    }

    var barcodeDataState by remember { mutableStateOf(barcodeData) }
    var totalQuantityState by remember { mutableStateOf(totalQuantity) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Save data whenever it changes
    LaunchedEffect(barcodeDataState, totalQuantityState) {
        saveData(barcodeDataState, totalQuantityState, sharedPreferences)
    }

    // Get current date
    val currentDate = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Options") },
            text = { Text("Choose how you want to export the data") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExportDialog = false
                        exportCombinedFile(context, barcodeDataState)
                    }
                ) {
                    Text("Export All Data")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportDialog = false
                        exportSeparateInvoices(context, barcodeDataState)
                    }
                ) {
                    Text("Export By Invoice")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scan Master",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SHIVAM ERP SOFTWARE SOLUTIONS \nMOBILE NO- 08585969065",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    tabs.forEachIndexed { index, title ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            label = { Text(title, fontSize = 10.sp) },
                            icon = {
                                when (index) {
                                    0 -> Icon(Icons.Filled.AddCircle, contentDescription = null)
                                    1 -> Icon(Icons.Filled.List, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> BarcodeSaverApp(
                    storageDir,
                    totalQuantityState,
                    { totalQuantityState = it },
                    barcodeDataState,
                    { barcodeDataState = it },
                    { showExportDialog = true }
                )
               1-> ViewSavedData(
                    barcodeData = barcodeDataState,              // Pass the barcode data state
                    totalQuantity = totalQuantityState,          // Pass the total quantity state
                    onExportClick = { showExportDialog = true }, // Handle export dialog
                    onBarcodeDataChange = { updatedData ->
                        barcodeDataState = updatedData          // Update barcode data state when it changes
                    },
                    onQuantityChange = { updatedQuantity ->
                        totalQuantityState = updatedQuantity    // Update total quantity when it changes
                    }
                )

            }
        }
    }
}@Composable
fun BarcodeSaverApp(
    storageDir: File,
    totalQuantity: Int,
    onQuantityChange: (Int) -> Unit,
    barcodeData: MutableMap<String, MutableList<String>>,
    onBarcodeDataChange: (MutableMap<String, MutableList<String>>) -> Unit,
    onExportClick: () -> Unit
) {

    val context = LocalContext.current
    var invoiceId by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) } // State for showing delete dialog
    var currentInvoiceCount by remember { mutableStateOf(0) }
    LaunchedEffect(invoiceId, barcodeData) {
        currentInvoiceCount = barcodeData[invoiceId]?.size ?: 0
    }

    // Show delete confirmation dialog
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                // When confirmed, clear all barcodes and update states
                onBarcodeDataChange(mutableMapOf()) // Clears all barcode data
                onQuantityChange(0) // Resets total quantity
                showDeleteDialog = false // Close the dialog
            },
            onDismiss = {
                showDeleteDialog = false // Close the dialog on dismiss
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sales Order Number",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = invoiceId,
            onValueChange = { invoiceId = it },
            label = { Text("Invoice ID") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        )

        OutlinedTextField(
            value = barcode,
            onValueChange = { barcode = it },
            label = { Text("Barcode Number") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    saveBarcodeData(
                        context,
                        invoiceId,
                        barcode,
                        barcodeData,
                        onBarcodeDataChange,
                        onQuantityChange,
                        totalQuantity
                    )
                    barcode = ""
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        Button(
            onClick = {
                saveBarcodeData(
                    context,
                    invoiceId,
                    barcode,
                    barcodeData,
                    onBarcodeDataChange,
                    onQuantityChange,
                    totalQuantity
                )
                barcode = ""
                // Update the currentInvoiceCount immediately
                currentInvoiceCount = barcodeData[invoiceId]?.size ?: 0
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Save")
        }

        Button(
            onClick = { onExportClick() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text("Export Data", color = Color.White)
        }



        Text(
            text = "Barcodes in Invoice $invoiceId: $currentInvoiceCount",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ViewSavedData(
    barcodeData: MutableMap<String, MutableList<String>>,
    totalQuantity: Int,
    onExportClick: () -> Unit,
    onBarcodeDataChange: (MutableMap<String, MutableList<String>>) -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                // When confirmed, clear all barcodes and update states
                onBarcodeDataChange(mutableMapOf()) // Clears all barcode data
                onQuantityChange(0) // Resets total quantity
                showDeleteDialog = false // Close the dialog
                FancyToast.makeText(context, "Deleted successfully!", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS,true).show()

            },
            onDismiss = {
                showDeleteDialog = false // Close the dialog on dismiss
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {


        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            barcodeData.forEach { (invoiceId, barcodes) ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Invoice ID: $invoiceId",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Total Barcodes: ${barcodes.size}",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            barcodes.forEach { barcode ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = barcode,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )

                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { showDeleteDialog = true },  // Trigger delete confirmation dialog
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red
                                )
                            ) {
                                Text("Delete All Barcodes", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { onExportClick() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Export Data")
        }
    }
}
fun saveBarcodeData(
    context: Context,
    invoiceId: String,
    barcode: String,
    barcodeData: MutableMap<String, MutableList<String>>,
    onBarcodeDataChange: (MutableMap<String, MutableList<String>>) -> Unit,
    onQuantityChange: (Int) -> Unit,
    totalQuantity: Int
) {

    if (invoiceId.isBlank() || barcode.isBlank()) {
        FancyToast.makeText(context, "Both Fields Required", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show()
        return
    }


    // Check if the barcode already exists for the current invoice only
    val existingBarcodesForInvoice = barcodeData[invoiceId]
    if (existingBarcodesForInvoice != null && existingBarcodesForInvoice.contains(barcode)) {

        FancyToast.makeText(context, "Duplicate Barcode Entry!", FancyToast.LENGTH_SHORT, FancyToast.ERROR, false).show()
        return
    }

    // If no duplication, update the barcode data for the given invoice
    val updatedData = barcodeData.toMutableMap()
    val currentBarcodes = updatedData[invoiceId]?.toMutableList() ?: mutableListOf()
    currentBarcodes.add(barcode)
    updatedData[invoiceId] = currentBarcodes
    onBarcodeDataChange(updatedData)
    onQuantityChange(totalQuantity + 1)

    FancyToast.makeText(context, "Saved successfully!", FancyToast.LENGTH_SHORT, FancyToast.SUCCESS,true).show()

}


fun saveData(
    barcodeData: MutableMap<String, MutableList<String>>,
    totalQuantity: Int,
    sharedPreferences: SharedPreferences
) {
    try {
        val gson = Gson()
        val barcodeDataJson = gson.toJson(barcodeData)

        sharedPreferences.edit().apply {
            putString("barcodeData", barcodeDataJson)
            putInt("totalQuantity", totalQuantity)
            apply()
        }
    } catch (e: Exception) {
        Log.e("BarcodeApp", "Error saving data: ${e.message}")
    }
}

fun loadData(sharedPreferences: SharedPreferences): Pair<MutableMap<String, MutableList<String>>, Int> {
    val gson = Gson()
    val barcodeDataJson = sharedPreferences.getString("barcodeData", null)
    val barcodeData: MutableMap<String, MutableList<String>> = if (barcodeDataJson != null) {
        gson.fromJson(barcodeDataJson, object : TypeToken<MutableMap<String, MutableList<String>>>() {}.type)
    } else {
        mutableMapOf()
    }

    val totalQuantity = sharedPreferences.getInt("totalQuantity", 0)
    return Pair(barcodeData, totalQuantity)
}

private fun exportCombinedFile(context: Context, barcodeData: Map<String, List<String>>) {
    if (barcodeData.isEmpty()) {
        Toast.makeText(context, "No data to export", Toast.LENGTH_SHORT).show()
        return
    }

    val exportActivityLauncher: ActivityResultLauncher<Intent> = (context as ComponentActivity).activityResultRegistry.register(
        "exportCombinedFile", ActivityResultContracts.StartActivityForResult()
    ) { result ->if (result.resultCode == RESULT_OK) {
        val uri: Uri? = result.data?.data
        uri?.let { exportFile(context, it, barcodeData) }
    } else {
        Toast.makeText(context, "Export canceled", Toast.LENGTH_SHORT).show()
    }
    }

    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "text/plain"
        putExtra(Intent.EXTRA_TITLE, "all_invoices_${getCurrentDateTime()}.txt")
    }

    exportActivityLauncher.launch(intent)
}

private fun exportSeparateInvoices(context: Context, barcodeData: Map<String, List<String>>) {
    if (barcodeData.isEmpty()) {
        Toast.makeText(context, "No data to export", Toast.LENGTH_SHORT).show()
        return
    }

    barcodeData.forEach { (invoiceId, barcodes) ->
        val exportActivityLauncher: ActivityResultLauncher<Intent> = (context as ComponentActivity).activityResultRegistry.register(
            "export_$invoiceId", ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri: Uri? = result.data?.data
                uri?.let {
                    exportSingleInvoice(context, it, invoiceId, barcodes)
                }
            }
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "invoice_${invoiceId}_${getCurrentDateTime()}.txt")
        }

        exportActivityLauncher.launch(intent)
    }
}

private fun exportFile(context: Context, uri: Uri, barcodeData: Map<String, List<String>>) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val writer = OutputStreamWriter(outputStream)

            // Write header with total statistics

            writer.write("TOTAL INVOICES: ${barcodeData.size}\n")
            writer.write("TOTAL BARCODES: ${barcodeData.values.sumOf { it.size }}\n\n")

            // Write data for each invoice
            barcodeData.forEach { (invoiceId, barcodes) ->
                writer.write("=========================================\n")
                writer.write("INVOICE ID: $invoiceId\n")
                writer.write("TOTAL BARCODES IN INVOICE: ${barcodes.size}\n")
                writer.write("-----------------------------------------\n")
                barcodes.forEachIndexed { index, barcode ->
                    writer.write("$barcode\n")
                }
                writer.write("\n")
            }

            writer.flush()
            writer.close()
            Toast.makeText(context, "All data exported successfully", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error exporting data: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun exportSingleInvoice(context: Context, uri: Uri, invoiceId: String, barcodes: List<String>) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val writer = OutputStreamWriter(outputStream)

            // Write header

            writer.write("INVOICE ID: $invoiceId\n")
            writer.write("TOTAL BARCODES: ${barcodes.size}\n")
            writer.write("-----------------------------------------\n\n")

            // Write barcodes
            barcodes.forEachIndexed { index, barcode ->
                writer.write("$barcode\n")
            }

            writer.flush()
            writer.close()
            Toast.makeText(context, "Invoice $invoiceId exported successfully", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error exporting invoice $invoiceId: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun getCurrentDateTime(): String {
    return SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
}

@Composable
fun CustomToast(message: String, isSuccess: Boolean) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .background(
                if (isSuccess) Color.Green else Color.Red,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp)
    ) {
        Text(
            text = message,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
fun deleteSingleBarcode(
    invoiceId: String,
    barcode: String,
    barcodeData: MutableMap<String, MutableList<String>>,
    onBarcodeDataChange: (MutableMap<String, MutableList<String>>) -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    val updatedData = barcodeData.toMutableMap()
    val barcodes = updatedData[invoiceId]
    if (barcodes != null) {
        barcodes.remove(barcode)
        if (barcodes.isEmpty()) {
            updatedData.remove(invoiceId)
        }
        onBarcodeDataChange(updatedData)
        onQuantityChange(updatedData.values.sumOf { it.size })
    }
}

fun deleteAllBarcodesForInvoice(
    invoiceId: String,
    barcodeData: MutableMap<String, MutableList<String>>,
    onBarcodeDataChange: (MutableMap<String, MutableList<String>>) -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    val updatedData = barcodeData.toMutableMap()
    val barcodes = updatedData.remove(invoiceId)
    if (barcodes != null) {
        onBarcodeDataChange(updatedData)
        onQuantityChange(updatedData.values.sumOf { it.size })
    }
}
@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Deletion") },
        text = { Text("Are you sure you want to delete all barcodes? This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}



