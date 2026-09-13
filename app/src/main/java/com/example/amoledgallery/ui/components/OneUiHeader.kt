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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.amoledgallery.R
import com.example.amoledgallery.ui.theme.AmoledBlack
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.TextMuted
import com.example.amoledgallery.ui.themedBackgroundBlur

/**
 * Samsung OneUI style large header with generous top space (Large Title),
 * prominent title and media count. When [heroThumbnail] is supplied (One UI 8.5's
 * "latest photo/video" album header preview), it's shown blurred and darkened
 * behind the title instead of a flat background - only where the active design
 * language wants blur and the device is on HIGH/ULTRA graphics quality.
 */
@Composable
fun OneUiHeader(
    mediaCount: Int,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.header_media),
    heroThumbnail: Any? = null
) {
    val useHero = heroThumbnail != null && LocalAppTheme.current.shapes.useBlur

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
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (useHero) Modifier else Modifier.background(AmoledBlack))
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
                Text(
                    text = if (mediaCount == 1) "1 Element" else "$mediaCount Elemente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (useHero) Color.White.copy(alpha = 0.85f) else TextMuted,
                    fontSize = 14.sp
                )
            }
        }
    }
}
