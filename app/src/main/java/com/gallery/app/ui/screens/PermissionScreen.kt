package com.gallery.app.ui.screens
import com.gallery.app.ui.i18n.strings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gallery.app.ui.design.DialogButtonStyle
import com.gallery.app.ui.design.ThemedButton
import com.gallery.app.ui.design.ThemedCard
import com.gallery.app.ui.theme.LocalAppTheme

/**
 * First-launch permission prompt. It stays on screen after the media permission is granted so the
 * optional all-files access can be turned on too; only the bottom Close button dismisses it, and that
 * button is greyed out until the gallery can actually show the library.
 */
@Composable
fun PermissionScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    missingMedia: Boolean = true,
    onOpenSettings: () -> Unit = {},
    /** Android 11+ only: all-files access is optional and only asked for from an explicit tap. */
    canManageAllFiles: Boolean = false,
    hasAllFilesAccess: Boolean = false,
    /** True once the media permission has been granted; enables Close. */
    mediaGranted: Boolean = false,
    onRequestAllFilesAccess: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    val palette = LocalAppTheme.current.palette
    val scroll = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PhotoLibrary, null, tint = palette.accent, modifier = Modifier.size(42.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = strings.permissionTitle,
                color = palette.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = strings.permissionDescription,
                color = palette.textMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Both permissions live in one card so the screen reads as a single list, not two boxes.
            ThemedCard {
                PermissionSection(
                    icon = Icons.Rounded.PhotoLibrary,
                    iconColor = palette.accent,
                    title = strings.permissionMediaTitle,
                    subtitle = if (mediaGranted) {
                        strings.allFilesGranted
                    } else {
                        strings.permissionMediaNeeded
                    },
                    granted = mediaGranted,
                    action = if (mediaGranted) {
                        null
                    } else {
                        {
                            ThemedButton(
                                text = strings.permissionGrant,
                                onClick = onRequestPermission,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                )

                if (canManageAllFiles) {
                    Box(
                        modifier = Modifier
                            .padding(start = 78.dp, end = 44.dp, top = 16.dp, bottom = 16.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(palette.onBackground.copy(alpha = 0.08f))
                    )
                    PermissionSection(
                        icon = Icons.Rounded.Folder,
                        iconColor = Color(0xFF4CC34F),
                        title = strings.allFilesAccess,
                        subtitle = if (hasAllFilesAccess) {
                            strings.allFilesGranted
                        } else {
                            strings.allFilesDescription
                        },
                        granted = hasAllFilesAccess,
                        action = if (hasAllFilesAccess) {
                            null
                        } else {
                            {
                                ThemedButton(
                                    text = strings.allFilesGrant,
                                    onClick = onRequestAllFilesAccess,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = DialogButtonStyle.Default
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Stays grey and untappable until the media permission makes the gallery usable.
            ThemedButton(
                text = strings.close,
                onClick = onClose,
                enabled = mediaGranted,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Fallback for a permanently denied permission, where the system dialog no longer appears.
            Text(
                text = strings.permissionOpenSettings,
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

/** One row of the unified permission card: coloured icon, title, status and an optional action. */
@Composable
private fun PermissionSection(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    granted: Boolean,
    action: (@Composable () -> Unit)?
) {
    val palette = LocalAppTheme.current.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = palette.onBackground, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                if (granted) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Rounded.CheckCircle, null, tint = palette.accent, modifier = Modifier.size(17.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = if (granted) palette.accent else palette.textMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            if (action != null) {
                Spacer(modifier = Modifier.height(12.dp))
                action()
            }
        }
    }
}
