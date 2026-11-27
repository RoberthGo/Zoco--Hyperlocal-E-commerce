package com.example.zoco.ui.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.zoco.R
import com.example.zoco.domain.model.NotificationItem
import java.text.DateFormat
import java.util.Date

/**
 * Ventana con las notificaciones de la cuenta. Tocar una la marca como leída;
 * "Marcar todas" las marca todas de una vez.
 */
@Composable
fun NotificationsDialog(
    state: NotificationsUiState,
    onMarkRead: (Long) -> Unit,
    onMarkAllRead: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notifications_title)) },
        text = {
            when {
                state.isLoading && state.items.isEmpty() -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.items.isEmpty() -> Text(stringResource(R.string.notifications_empty))
                else -> LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        NotificationCard(item, onClick = { onMarkRead(item.id) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.notifications_close)) }
        },
        dismissButton = {
            if (state.unreadCount > 0) {
                TextButton(onClick = onMarkAllRead) { Text(stringResource(R.string.notifications_mark_all)) }
            }
        }
    )
}

/** Una notificación: punto azul si no se ha leído, título, mensaje y fecha. */
@Composable
private fun NotificationCard(item: NotificationItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            if (!item.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, end = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold
                )
                Text(text = item.message, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
