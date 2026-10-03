package com.budgetapp.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.app.data.model.EnvelopeBalance
import com.budgetapp.app.ui.theme.MockMaterialSurface
import com.budgetapp.app.ui.theme.MockNeutral
import com.budgetapp.app.ui.theme.MockPrimaryDeep
import com.budgetapp.app.ui.theme.MockPrimaryWash
import com.budgetapp.app.ui.theme.MockSurfaceLightAlt
import com.budgetapp.app.ui.theme.MockWhiteSoft

private val MaxVisibleRows = 4
private val RowHeight = 42.dp
private val RowSpacing = 6.dp

/**
 * Envelope choice list for the sheets that need one. Shows four rows at a time and scrolls for the
 * rest — the previous fixed `take(4)`/`take(6)` versions silently hid every envelope past the
 * fourth, so a budget with more than four envelopes could not receive a transaction at all.
 */
@Composable
fun EnvelopePickerList(
    envelopes: List<EnvelopeBalance>,
    selectedId: Long?,
    onSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    selectedTextColor: Color = MockPrimaryDeep,
    unselectedTextColor: Color = MockMaterialSurface,
    emptyText: String = "Create an envelope first",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MockWhiteSoft)
            .border(1.dp, MockSurfaceLightAlt, RoundedCornerShape(16.dp))
            .padding(6.dp),
    ) {
        if (envelopes.isEmpty()) {
            Text(
                text = emptyText,
                color = MockNeutral,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(12.dp),
            )
            return
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    max = RowHeight * MaxVisibleRows + RowSpacing * (MaxVisibleRows - 1),
                )
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(RowSpacing),
        ) {
            envelopes.forEach { envelope ->
                val isSelected = envelope.id == selectedId
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RowHeight)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MockPrimaryWash else MockWhiteSoft)
                        // selectable, not clickable: TalkBack (and any UI test) can then tell which
                        // envelope is currently chosen instead of guessing from the background.
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelected(envelope.id) },
                        )
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = envelope.name,
                        color = if (isSelected) selectedTextColor else unselectedTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
