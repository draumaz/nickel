package io.github.draumaz.nickel.ui

import androidx.compose.ui.platform.LocalConfiguration
import io.github.draumaz.nickel.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.draumaz.nickel.data.Money
import java.util.Currency
import java.util.Locale

/** "US Dollar (USD)", in the user's language. */
internal fun currencyLabel(currency: Currency): String =
    "${currency.getDisplayName(Locale.getDefault())} (${currency.currencyCode})"

/**
 * Picks the currency Nickel shows amounts in. "System default" (a `null` code) follows the
 * phone's locale. Amounts aren't converted; only the symbol changes.
 */
@Composable
internal fun CurrencyDialog(
    selectedCode: String?,
    onDismiss: () -> Unit,
    onSelect: (code: String?) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val locale = LocalConfiguration.current.locales[0]
    // Only currencies some country uses today: going by locales drops historic ones
    // (Afghan Afghani 1927–2002) and accounting units (XUA, XBA) that the full list includes.
    val all = remember {
        Locale.getAvailableLocales()
            .mapNotNullTo(HashSet()) { l -> l.country.takeIf { it.isNotEmpty() }?.let { runCatching { Currency.getInstance(l) }.getOrNull() } }
            .sortedBy { it.getDisplayName(locale).lowercase(locale) }
    }
    val q = query.trim()
    val matches = if (q.isEmpty()) all else all.filter {
        it.currencyCode.contains(q, ignoreCase = true) ||
            it.getDisplayName(locale).contains(q, ignoreCase = true) ||
            it.getSymbol(locale).contains(q, ignoreCase = true)
    }
    val system = Money.systemCurrency

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.currency)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.currency_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(30) },
                    placeholder = { Text(stringResource(R.string.search)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.fillMaxWidth().height(320.dp)) {
                    if (q.isEmpty()) {
                        item(key = "system") {
                            CurrencyOption(
                                symbol = system.getSymbol(locale),
                                title = stringResource(R.string.system_default),
                                subtitle = currencyLabel(system),
                                selected = selectedCode == null,
                                onClick = { onSelect(null) },
                            )
                        }
                    }
                    items(matches, key = { it.currencyCode }) { c ->
                        CurrencyOption(
                            symbol = c.getSymbol(locale),
                            title = c.getDisplayName(locale),
                            subtitle = c.currencyCode,
                            selected = selectedCode == c.currencyCode,
                            onClick = { onSelect(c.currencyCode) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun CurrencyOption(
    symbol: String,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            symbol,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.width(48.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = stringResource(R.string.currency_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
