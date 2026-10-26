package com.example.motionpulse.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.example.motionpulse.R
import com.example.motionpulse.ui.theme.*

@Composable
fun DropletHeader(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    topActions: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    CapFontScale(maxFontScale = 1.3f) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(BackgroundDark)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Box(
                modifier = Modifier
                    .responsiveContentWidth(600.dp)
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 16.dp)
                    .align(Alignment.Center)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Optional Top Row
                    if (navigationIcon != null || topActions != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                navigationIcon?.invoke()
                            }
                            Box(
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                topActions?.invoke()
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Main Row: Droplet on left, Text on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Droplet Image on Left (~42% of screen width, clamp 110dp to 200dp)
                        BoxWithConstraints(
                            modifier = Modifier
                                .width(min(200.dp, max(110.dp, maxWidth * 0.42f)))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.droplet),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Text Column on Right, right-aligned
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            if (!caption.isNullOrBlank()) {
                                Text(
                                    text = caption.uppercase(),
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp,
                                    textAlign = TextAlign.End
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Text(
                                text = title,
                                color = TextPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 28.sp, // ~1.1 line height for 26sp
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { heading() }
                            )

                            if (trailing != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                trailing()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoodPill(
    moodLevelName: String,
    dotColor: Color
) {
    Surface(
        color = Color(0xFF361B6A),
        shape = RoundedCornerShape(17.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = moodLevelName,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@AdaptivePreviews
@Composable
private fun DropletHeaderPreview() {
    MotionPulseTheme {
        Column(modifier = Modifier.background(BackgroundDark)) {
            DropletHeader(
                title = "Welcome back, User",
                caption = "MOTION.PULSE",
                trailing = {
                    MoodPill(moodLevelName = "Energized", dotColor = Color(0xFFEE44CB))
                }
            )
        }
    }
}
