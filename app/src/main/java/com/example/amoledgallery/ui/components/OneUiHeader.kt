package com.example.amoledgallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.amoledgallery.R
import com.example.amoledgallery.ui.theme.AmoledBlack
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.TextMuted
import com.example.amoledgallery.ui.themedBackgroundBlur

/**
 * Large header (Large Title) whose backdrop changes per [DesignLanguage] so the app's most
 * persistently visible screen element also carries the "different OS" feeling:
 * - SAMSUNG_ONEUI: One UI 8.5's own "latest photo/video" hero preview, blurred+darkened behind
 *   the title (only where blur is enabled, i.e. HIGH/ULTRA).
 * - GOOGLE_EXPRESSIVE: a bold diagonal accent-color gradient wash instead of a photo.
 * - IOS_LIQUID_GLASS / XIAOMI_HYPEROS: flat backdrop, no hero - Liquid Glass gets its
 *   distinctiveness from the floating glass nav bar, HyperOS tints the item count in its accent.
 */
@Composable
fun OneUiHeader(
    mediaCount: Int,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.header_media),
    heroThumbnail: Any? = null
) {
    val theme = LocalAppTheme.current
    val useHero = heroThumbnail != null && theme.language == DesignLanguage.SAMSUNG_ONEUI
    val useGradientWash = theme.language == DesignLanguage.GOOGLE_EXPRESSIVE

    Box(modifier = modifier.fillMaxWidth()) {
        if (useHero) {
            AsyncImage(
                model = heroThumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .themedBackgroundBlur(radiusDp = 40f)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        } else if (useGradientWash) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(theme.palette.accent.copy(alpha = 0.32f), Color.Transparent)
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (useHero || useGradientWash) Modifier else Modifier.background(AmoledBlack))
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                letterSpacing = (-0.5).sp
            )
            if (mediaCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                val countColor = when {
                    useHero -> Color.White.copy(alpha = 0.85f)
                    theme.language == DesignLanguage.XIAOMI_HYPEROS -> theme.palette.accent
                    else -> TextMuted
                }
                Text(
                    text = if (mediaCount == 1) "1 Element" else "$mediaCount Elemente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = countColor,
                    fontSize = 14.sp
                )
            }
        }
    }
}
