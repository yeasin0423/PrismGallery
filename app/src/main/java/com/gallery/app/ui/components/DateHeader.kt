package com.gallery.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.ui.theme.LocalAppTheme

/**
 * Date section header above a group of grid tiles. Transparent, so the glass bars' backdrop
 * continues behind it: a big bold day label like Samsung Gallery, with a round check in front of it
 * that selects the whole day while selecting.
 */
@Composable
fun DateHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    /** One UI: whether the whole section is selected (its round check is filled). */
    actionChecked: Boolean = false
) {
    val palette = LocalAppTheme.current.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (actionText != null && onAction != null) {
            OneUiCheckCircle(
                checked = actionChecked,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onAction)
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 20.dp))
        }
        Text(
            text = title,
            color = palette.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
