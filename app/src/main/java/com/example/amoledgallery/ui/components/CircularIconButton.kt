package com.example.amoledgallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import com.example.amoledgallery.ui.theme.LocalAppTheme

/**
 * One UI 8/9 style circular icon button: a larger, fully round tap target with a soft
 * translucent background, replacing the plain borderless [IconButton] used pre-refresh.
 * Size follows the active design language's [com.example.amoledgallery.ui.theme.AppShapeTokens.iconButtonSize].
 */
@Composable
fun CircularIconButton(
    onClick: () -> Unit,
    contentDescription: String?,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = LocalAppTheme.current.palette.onBackground,
    backgroundColor: Color = LocalAppTheme.current.palette.surfaceVariant.copy(alpha = 0.6f)
) {
    val size = LocalAppTheme.current.shapes.iconButtonSize
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(size)) {
            CompositionLocalProvider(LocalContentColor provides tint) {
                Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
            }
        }
    }
}
