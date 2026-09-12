package com.example.amoledgallery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amoledgallery.ui.theme.AmoledBlack

/**
 * Modern Samsung OneUI AMOLED date section header:
 * - Subtle matte-white typography (#E4E4E7) comfortable on OLED displays
 * - Rounded divider pill elements (#222224) providing clean, elegant structural hierarchy
 */
@Composable
fun DateHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AmoledBlack)
            .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFFE4E4E7),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.15.sp
        )

        // Rounded OneUI divider element
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.5.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color(0xFF222224))
        )
    }
}
