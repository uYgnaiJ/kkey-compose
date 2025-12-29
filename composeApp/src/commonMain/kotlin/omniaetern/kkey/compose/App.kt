package omniaetern.kkey.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import omniaetern.kkey.models.PasswordEntry
import omniaetern.kkey.models.SecureRequest
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import kotlin.time.Clock

@Composable
@Preview
fun App() {
    MaterialTheme {
        var address by remember { mutableStateOf("localhost:7347") }
        var key by remember { mutableStateOf("") }
        var serverList by remember { mutableStateOf("") }
        var fetchedData by remember { mutableStateOf<SecureRequest?>(null) }
        var decryptedData by remember { mutableStateOf<String?>(null) }
        var passwordEntries by remember { mutableStateOf<List<PasswordEntry>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isConnected by remember { mutableStateOf(false) }
        var showAddDialog by remember { mutableStateOf(false) }
        var editingEntry by remember { mutableStateOf<PasswordEntry?>(null) }

        val scope = rememberCoroutineScope()
        val client = remember {
            HttpClient {
                install(ContentNegotiation) {
                    json()
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (!isConnected) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "kkey",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Server Address") },
                        placeholder = { Text("e.g. localhost:7347") },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        label = { Text("Encryption Key") },
                        placeholder = { Text("Enter your password") },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (errorMessage != null) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    // 1. Clean up address
                                    var cleanAddress = address.trim().removeSuffix("/")

                                    // 2. Ensure address has protocol
                                    var fullAddress =
                                        if (cleanAddress.startsWith("http")) cleanAddress else "http://$cleanAddress"

                                    // 3. Default to 7347 if no port specified
                                    if (!fullAddress.substringAfter("://").contains(":")) {
                                        fullAddress += ":7347"
                                    }

                                    val targetUrl = "$fullAddress/fetch/server-list"
                                    val serverListResponse = client.get(targetUrl)

                                    if (serverListResponse.status.value in 200..299) {
                                        serverList = serverListResponse.bodyAsText()

                                        // Also fetch actual data
                                        try {
                                            val dataUrl = "$fullAddress/fetch/data"
                                            val dataResponse = client.get(dataUrl)
                                            if (dataResponse.status.value in 200..299) {
                                                fetchedData = dataResponse.body<SecureRequest>()
                                                if (key.isNotBlank()) {
                                                    val decrypted = getPlatform().decrypt(
                                                        fetchedData!!.encryptedData,
                                                        fetchedData!!.iv,
                                                        key
                                                    )
                                                    if (!decrypted.startsWith("Decryption Error")) {
                                                        decryptedData = decrypted
                                                        passwordEntries =
                                                            Json.decodeFromString<List<PasswordEntry>>(decrypted)
                                                    } else {
                                                        errorMessage = decrypted
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Data fetch error: ${e.message}"
                                        }

                                        isConnected = true
                                    } else {
                                        errorMessage = "Error ${serverListResponse.status} at $targetUrl"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Connection failed: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Connect")
                        }
                    }
                }
            } else {
                // Function to sync data with server
                fun syncData() {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val json = Json.encodeToString(passwordEntries)
                            val (encrypted, iv) = getPlatform().encrypt(json, key)
                            val request = SecureRequest(encryptedData = encrypted, iv = iv)

                            var cleanAddress = address.trim().removeSuffix("/")
                            var fullAddress =
                                if (cleanAddress.startsWith("http")) cleanAddress else "http://$cleanAddress"
                            if (!fullAddress.substringAfter("://").contains(":")) fullAddress += ":7347"

                            val response = client.post("$fullAddress/update") {
                                contentType(io.ktor.http.ContentType.Application.Json)
                                setBody(request)
                            }

                            if (response.status.value in 200..299) {
                                // Refresh list after sync if needed, though we already have it locally
                            } else {
                                errorMessage = "Sync failed: ${response.status}"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Sync error: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                }

                val activeEntries = passwordEntries.filter { !it.deleted }

                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    // Header Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "kkey Entries",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            IconButton(onClick = { syncData() }, enabled = !isLoading) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync")
                            }
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add New")
                            }
                            Button(
                                onClick = { isConnected = false },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Disconnect")
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // List Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Name",
                            modifier = Modifier.weight(1.2f),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            "URL",
                            modifier = Modifier.weight(1.2f),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            "Password",
                            modifier = Modifier.weight(1.2f),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            "Description",
                            modifier = Modifier.weight(1.5f),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            "Tools",
                            modifier = Modifier.width(80.dp),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }

                    // List Content
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        items(activeEntries) { entry ->
                            var showPassword by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { editingEntry = entry }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    entry.name,
                                    modifier = Modifier.weight(1.2f),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    entry.url,
                                    modifier = Modifier.weight(1.2f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1
                                )
                                Text(
                                    if (showPassword) entry.password else "••••••••",
                                    modifier = Modifier.weight(1.2f).clickable { showPassword = !showPassword },
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    color = if (showPassword) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    entry.description,
                                    modifier = Modifier.weight(1.5f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Row(modifier = Modifier.width(80.dp), horizontalArrangement = Arrangement.End) {
                                    IconButton(modifier = Modifier.size(24.dp), onClick = { editingEntry = entry }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    IconButton(modifier = Modifier.size(24.dp), onClick = {
                                        passwordEntries = passwordEntries.map {
                                            if (it.id == entry.id) it.copy(
                                                deleted = true,
                                                lastModified = Clock.System.now().toEpochMilliseconds()
                                            )
                                            else it
                                        }
                                        syncData()
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Text(
                        "Connected to: $address | ${activeEntries.size} entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Add/Edit Dialog
                if (showAddDialog || editingEntry != null) {
                    var name by remember { mutableStateOf(editingEntry?.name ?: "") }
                    var password by remember { mutableStateOf(editingEntry?.password ?: "") }
                    var url by remember { mutableStateOf(editingEntry?.url ?: "") }
                    var description by remember { mutableStateOf(editingEntry?.description ?: "") }

                    AlertDialog(
                        onDismissRequest = {
                            showAddDialog = false
                            editingEntry = null
                        },
                        title = { Text(if (showAddDialog) "Add Entry" else "Edit Entry") },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Name") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = url,
                                    onValueChange = { url = it },
                                    label = { Text("URL") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Password") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    label = { Text("Description") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (showAddDialog) {
                                    val newEntry = PasswordEntry(
                                        id = getPlatform().name.hashCode().toString() + "-" + Clock.System.now()
                                            .toEpochMilliseconds(), // Fake UUID for now
                                        name = name,
                                        password = password,
                                        url = url,
                                        description = description,
                                        lastModified = Clock.System.now().toEpochMilliseconds(),
                                        deleted = false,
                                        version = 1
                                    )
                                    passwordEntries = passwordEntries + newEntry
                                } else if (editingEntry != null) {
                                    passwordEntries = passwordEntries.map {
                                        if (it.id == editingEntry!!.id) {
                                            it.copy(
                                                name = name,
                                                password = password,
                                                url = url,
                                                description = description,
                                                lastModified = Clock.System.now().toEpochMilliseconds()
                                            )
                                        } else it
                                    }
                                }
                                showAddDialog = false
                                editingEntry = null
                                syncData()
                            }) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showAddDialog = false
                                editingEntry = null
                            }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}