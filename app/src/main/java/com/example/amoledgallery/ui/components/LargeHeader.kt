package com.example.amoledgallery.ui.components
import com.example.amoledgallery.ui.i18n.strings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.amoledgallery.ui.design.pressScale
import com.example.amoledgallery.ui.design.geminiChip
import com.example.amoledgallery.ui.theme.DesignLanguage
import com.example.amoledgallery.ui.theme.LocalAppTheme
import com.example.amoledgallery.ui.theme.onAccent

/**
 * The large title at the top of the Bilder / Alben / Menü tabs, in each OS's own layout:
 * - One UI: the expanded app bar - an empty upper third with a big centred title, action icons
 *   right-aligned underneath. The title fades and lags behind while scrolling (see [collapseFraction]).
 * - Material 3 Expressive: actions row on top, big start-aligned headline below.
 * - iOS: actions (glass circles) on top, bold 34pt large title below.
 * - HyperOS: plain action icons on top, lighter-weight start-aligned title below.
 */
@Composable
fun LargeHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    collapseFraction: () -> Float = { 0f },
    /**
     * One UI only: a taller expanded bar so the grid starts lower and the ⋮ button sits within reach of
     * the thumb, like Samsung Gallery's photo and album lists.
     */
    tall: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val theme = LocalAppTheme.current
    val palette = theme.palette

    when (theme.language) {
        DesignLanguage.SAMSUNG_ONEUI -> {
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            val actionRowHeight = 56.dp
            Column(modifier = modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((screenHeight * if (tall) 0.44f else 0.36f).coerceAtLeast(260.dp) - actionRowHeight)
                        .graphicsLayer {
                            val f = collapseFraction()
                            alpha = (1f - f * 1.5f).coerceIn(0f, 1f)
                            translationY = f * 70.dp.toPx()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.statusBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            color = palette.onBackground,
                            fontSize = theme.shapes.headerTitleSize,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subtitle != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = subtitle, color = palette.textMuted, fontSize = 15.sp)
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(actionRowHeight)
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }

        // Google Photos: no big title - a pill chip with the title and count on the left, actions on the right.
        DesignLanguage.GOOGLE_EXPRESSIVE -> Row(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(76.dp)
                .padding(start = 16.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .geminiChip(CircleShape)
                    .padding(horizontal = 20.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = palette.onBackground,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = palette.textMuted,
                        fontSize = 15.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.padding(start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }

        DesignLanguage.IOS_LIQUID_GLASS -> Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
            Text(
                text = title,
                color = palette.onBackground,
                fontSize = theme.shapes.headerTitleSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = palette.textMuted,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // HyperOS Gallery: plain action icons on top, then a big thin title with lots of air and no subtitle.
        DesignLanguage.XIAOMI_HYPEROS -> Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
            Text(
                text = title,
                color = palette.onBackground,
                fontSize = theme.shapes.headerTitleSize,
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 28.dp, end = 28.dp, top = 30.dp)
                    .graphicsLayer { alpha = (1f - collapseFraction() * 1.3f).coerceIn(0f, 1f) }
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

/**
 * Material 3 Expressive hero: the newest photo in a large rounded card that is clearly a card - it
 * never bleeds into the background, so there is no edge to blend - with a scrim-backed caption.
 */
@Composable
fun ExpressiveHeroCard(
    imageModel: Any?,
    title: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppTheme.current.palette
    val shape = RoundedCornerShape(32.dp)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .pressScale(interaction, 0.97f)
            .height(230.dp)
            .clip(shape)
            .background(palette.surfaceVariant)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.10f),
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.72f)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(CircleShape)
                .background(palette.accent)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(strings.newBadge, color = palette.onAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                text = caption,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.3).sp
            )
        }
    }
}
