package app.netlify.dev4rju9.animatedtablayout

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedTabLayout(
    tabs: List<@Composable (isSelected: Boolean) -> Unit>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    indicatorModifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Dp> = tween(durationMillis = 300, easing = FastOutSlowInEasing),
) {
    BoxWithConstraints(
        modifier = modifier
    ) {
        val tabWidth = if (tabs.isNotEmpty()) this.maxWidth / tabs.size else 0.dp

        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex,
            animationSpec = animationSpec,
            label = "indicator_offset"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .then(indicatorModifier)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tabContent ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(index) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    tabContent(index == selectedIndex)
                }
            }
        }
    }
}