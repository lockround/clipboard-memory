package com.clipboardmemory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clipboardmemory.network.ChatMessage
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val error by viewModel.error.collectAsState()
    val hasApiKey by viewModel.hasApiKey.collectAsState()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ask About Your Clipboard") },
                actions = {
                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Groq API settings"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .then(modifier)
        ) {
            if (!hasApiKey) {
                SetupPromptCard(onConfigure = { showApiKeyDialog = true })
            }

            error?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }
                if (isSending) {
                    item {
                        Row(Modifier.fillMaxWidth()) {
                            CircularProgressIndicator(modifier = Modifier.width(28.dp))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = viewModel::onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("e.g. What did I copy at 3pm?") },
                    maxLines = 4
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = viewModel::sendMessage,
                    enabled = inputText.isNotBlank() && !isSending
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = viewModel.getApiKey(),
            currentModel = viewModel.getModel(),
            onDismiss = { showApiKeyDialog = false },
            onSave = { key, model ->
                viewModel.setApiKey(key)
                viewModel.setModel(model)
                showApiKeyDialog = false
            }
        )
    }
}

@Composable
private fun SetupPromptCard(onConfigure: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Groq API key required",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "Get a free key at console.groq.com and paste it below. It's stored only on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            TextButton(onClick = onConfigure) { Text("Configure") }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            if (isUser) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .widthIn(max = 320.dp)
                )
            } else {
                AssistantMarkdownContent(message.content)
            }
        }
    }
}

@Composable
private fun AssistantMarkdownContent(content: String) {
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val colors = DefaultMarkdownColors(
        text = scheme.onSurfaceVariant,
        codeText = scheme.onSurfaceVariant,
        inlineCodeText = scheme.primary,
        linkText = scheme.primary,
        codeBackground = scheme.surfaceVariant.copy(alpha = 0.6f),
        inlineCodeBackground = scheme.surfaceVariant.copy(alpha = 0.6f),
        dividerColor = scheme.outlineVariant
    )

    val markdownTypography = DefaultMarkdownTypography(
        h1 = typography.headlineMedium,
        h2 = typography.headlineSmall,
        h3 = typography.titleLarge,
        h4 = typography.titleMedium,
        h5 = typography.titleMedium,
        h6 = typography.titleMedium,
        text = typography.bodyMedium,
        code = typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        ),
        quote = typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
        paragraph = typography.bodyMedium,
        ordered = typography.bodyMedium,
        bullet = typography.bodyMedium,
        list = typography.bodyMedium
    )

    Markdown(
        content = content,
        colors = colors,
        typography = markdownTypography,
        modifier = Modifier
            .widthIn(max = 360.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiKeyDialog(
    currentKey: String,
    currentModel: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var key by remember { mutableStateOf(currentKey) }
    var model by remember { mutableStateOf(currentModel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Groq Settings") },
        text = {
            Column {
                Text(
                    "Your key is stored locally on this device and used only for chat requests.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Start
                )
                Spacer(Modifier.padding(top = 12.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API Key (gsk_...)") },
                    singleLine = true
                )
                Spacer(Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    supportingText = {
                        Text("Check available models at console.groq.com")
                    },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(key.trim(), model.trim()) },
                enabled = key.trim().isNotEmpty() && model.trim().isNotEmpty()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}