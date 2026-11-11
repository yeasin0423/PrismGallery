package com.gallery.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.ui.theme.LocalAppTheme

/**
 * The title at the top of the Photos / Albums tabs: a compact One UI bar with the title on the left
 * and the action icons on the right.
 */
@Composable
fun LargeHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    collapseFraction: () -> Float = { 0f },
    actions: @Composable RowScope.() -> Unit = {}
) {
    val palette = LocalAppTheme.current.palette

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(64.dp)
            .padding(start = 20.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = title,
                color = palette.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(text = subtitle, color = palette.textMuted, fontSize = 14.sp, maxLines = 1)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}
