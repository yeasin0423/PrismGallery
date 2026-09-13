package com.example.amoledgallery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoledgallery.R
import com.example.amoledgallery.ui.design.ExpressiveCookieShape
import com.example.amoledgallery.ui.design.ThemedButton
import com.example.amoledgallery.ui.design.ThemedCard
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.destructive

/**
 * Centered permission prompt, only shown when something is actually missing after the system dialogs
 * have been answered - it names each missing permission and warns what breaks without it.
 */
@Composable
fun PermissionScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    missingMedia: Boolean = true,
    missingNotifications: Boolean = false,
    onOpenSettings: () -> Unit = {}
) {
    val theme = LocalAppTheme.current
    val palette = theme.palette

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(if (theme.language == DesignLanguage.GOOGLE_EXPRESSIVE) ExpressiveCookieShape else CircleShape)
                    .background(palette.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PhotoLibrary, null, tint = palette.accent, modifier = Modifier.size(44.dp))
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.permission_title),
                color = palette.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.permission_description),
                color = palette.textMuted,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            ThemedCard {
                Text(
                    text = stringResource(R.string.permission_warning),
                    color = palette.destructive,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 19.sp
                )
                if (missingMedia) {
                    Spacer(modifier = Modifier.height(10.dp))
                    MissingPermissionRow(stringResource(R.string.permission_missing_media))
                }
                if (missingNotifications) {
                    Spacer(modifier = Modifier.height(10.dp))
                    MissingPermissionRow(stringResource(R.string.permission_missing_notifications))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            ThemedButton(
                text = stringResource(R.string.permission_grant_button),
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fallback for a permanently denied permission, where the system dialog no longer appears.
            Text(
                text = stringResource(R.string.permission_open_settings),
                color = palette.textMuted,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun MissingPermissionRow(text: String) {
    val palette = LocalAppTheme.current.palette
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(palette.destructive)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, color = palette.onBackground, fontSize = 13.sp, lineHeight = 18.sp)
    }
}
