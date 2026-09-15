package com.example.amoledgallery.ui.components

import androidx.compose.foundation.background
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
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme

/**
 * Date section header above a group of grid tiles. Transparent, so Material's tonal glow and the
 * glass bars' backdrop continue behind it:
 * - One UI: big bold "Heute" like Samsung Gallery.
 * - Material: medium-weight label.
 * - iOS: bold 20pt section title.
 * - HyperOS: medium label; while selecting, a "Select all" pill for the section on the right ([actionText]).
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
    val theme = LocalAppTheme.current
    val palette = theme.palette
    when (theme.language) {
        // One UI: big bold day; while selecting, a round check in front of it selects the whole day.
        DesignLanguage.SAMSUNG_ONEUI -> Row(
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
        DesignLanguage.GOOGLE_EXPRESSIVE -> Text(
            text = title,
            color = palette.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 12.dp)
        )
        DesignLanguage.IOS_LIQUID_GLASS -> Text(
            text = title,
            color = palette.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
        )
        DesignLanguage.XIAOMI_HYPEROS -> Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = 28.dp,
                    end = 16.dp,
                    top = if (actionText != null) 8.dp else 14.dp,
                    bottom = if (actionText != null) 8.dp else 14.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = palette.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (actionText != null && onAction != null) {
                Text(
                    text = actionText,
                    color = palette.onBackground,
                    fontSize = 16.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.onBackground.copy(alpha = 0.12f))
                        .clickable(onClick = onAction)
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
        }
    }
}
