package com.sindantechnologies.gharhisab.ui.screens.transaction

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sindantechnologies.gharhisab.R
import com.sindantechnologies.gharhisab.data.local.room.entity.CategoryEntity
import com.sindantechnologies.gharhisab.domain.model.PaymentMethod
import com.sindantechnologies.gharhisab.ui.theme.Dimens
import com.sindantechnologies.gharhisab.util.IconMapper

/** Wrap labels and reduce columns when accessibility text needs more space. */
@Composable
internal fun TransactionCategories(
    categories: List<CategoryEntity>,
    selectedId: Long?,
    onSelect: (CategoryEntity) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = Dimens.SpacingLg)) {
        val cellWidth = 80.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val columns = (maxWidth / cellWidth).toInt().coerceIn(1, 4)
        Column(Modifier.selectableGroup()) {
            categories.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { category ->
                        val selected = selectedId == category.id
                        Column(
                            Modifier.weight(1f)
                                .selectable(selected, role = Role.RadioButton, onClick = { onSelect(category) })
                                .padding(Dimens.SpacingSm),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(Dimens.RadiusMd),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(52.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(IconMapper.getIcon(category.iconKey), contentDescription = null)
                                }
                            }
                            Spacer(Modifier.height(Dimens.SpacingXs))
                            Text(category.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@StringRes
internal fun paymentMethodLabel(method: PaymentMethod): Int = when (method) {
    PaymentMethod.CASH -> R.string.payment_cash
    PaymentMethod.BANK -> R.string.payment_bank
    PaymentMethod.CARD -> R.string.payment_card
    PaymentMethod.MOBILE_WALLET -> R.string.payment_mobile_wallet
    PaymentMethod.OTHER -> R.string.payment_other
}
