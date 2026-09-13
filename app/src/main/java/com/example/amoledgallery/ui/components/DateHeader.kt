package com.example.amoledgallery.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
 * - HyperOS: small, quiet label.
 */
@Composable
fun DateHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val palette = theme.palette
    when (theme.language) {
        DesignLanguage.SAMSUNG_ONEUI -> Text(
            text = title,
            color = palette.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 12.dp)
        )
        DesignLanguage.GOOGLE_EXPRESSIVE -> Text(
            text = title,
            color = palette.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 22.dp, bottom = 10.dp)
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
        DesignLanguage.XIAOMI_HYPEROS -> Text(
            text = title,
            color = palette.onBackground.copy(alpha = 0.85f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, top = 18.dp, bottom = 8.dp)
        )
    }
}
