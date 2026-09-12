package com.example.amoledgallery.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoledgallery.R
import com.example.amoledgallery.ui.theme.AmoledBlack
import com.example.amoledgallery.ui.theme.TextMuted

/**
 * Samsung OneUI style large header with generous top space (Large Title),
 * prominent title and media count.
 */
@Composable
fun OneUiHeader(
    mediaCount: Int,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.header_media)
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AmoledBlack)
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
                color = TextMuted,
                fontSize = 14.sp
            )
        }
    }
}
