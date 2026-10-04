package app.netlify.dev4rju9.wheelpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun <T> WheelPicker(
    modifier: Modifier = Modifier,
    items: List<T>,
    initialIndex: Int = 0,

    // Item
    itemHeight: Dp = 40.dp,
    itemContent: @Composable BoxScope.(item: T, isSelected: Boolean) -> Unit,

    // Divider
    enableDivider: Boolean = true,
    dividerWidth: Dp = 60.dp,
    dividerSpacingMultiplier: Float = 1f,
    divider: @Composable BoxScope.() -> Unit = {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .height(itemHeight * dividerSpacingMultiplier),
            verticalArrangement = Arrangement.Center
        ) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Cyan,
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Cyan,
                thickness = 1.dp
            )
        }
    },

    // Label
    labelContent: @Composable (() -> Unit)? = null,

    // Behavior
    visibleItemsCount: Int = 3,
    enabled: Boolean = true,
    onItemSelected: (index: Int, item: T) -> Unit
) {
    // Empty values are used as top/bottom spacers.
    val paddedItems = listOf(null) + items.map { it as T? } + listOf(null)

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex
    )

    val flingBehavior = rememberSnapFlingBehavior(listState)

    val selectedIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo

            if (layoutInfo.visibleItemsInfo.isEmpty()) {
                return@derivedStateOf initialIndex + 1
            }

            val center = layoutInfo.viewportEndOffset / 2

            layoutInfo.visibleItemsInfo.find {
                val itemCenter = it.offset + it.size / 2

                itemCenter in
                        (center - it.size / 2)..(center + it.size / 2)
            }?.index ?: (initialIndex + 1)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { selectedIndex }
            .distinctUntilChanged()
            .collect { paddedIndex ->
                val realIndex = (paddedIndex - 1)
                    .coerceIn(items.indices)

                items.getOrNull(realIndex)?.let { item ->
                    onItemSelected(realIndex, item)
                }
            }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(dividerWidth),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                userScrollEnabled = enabled,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight * visibleItemsCount)
            ) {
                itemsIndexed(paddedItems) { index, item ->
                    val isSelected = index == selectedIndex

                    Box(
                        modifier = Modifier.height(itemHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        item?.let {
                            itemContent(it, isSelected)
                        }
                    }
                }
            }

            if (enableDivider) {
                divider()
            }
        }

        labelContent?.let {
            it()
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF222222
)
@Composable
private fun WheelPickerDarkPreview() {
    MaterialTheme {
        Surface(color = Color(0xFF222222)) {
            Box(
                modifier = Modifier.padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                val months = listOf(
                    "Jan", "Feb", "Mar", "Apr",
                    "May", "Jun", "Jul", "Aug",
                    "Sep", "Oct", "Nov", "Dec"
                )

                WheelPicker(
                    items = months,
                    itemContent = { item, isSelected ->
                        Text(
                            text = item,
                            fontSize = if (isSelected) 22.sp else 18.sp,
                            color = if (isSelected) {
                                Color.White
                            } else {
                                Color.DarkGray
                            }
                        )
                    },
                    enableDivider = true,
                    dividerWidth = 70.dp,
                    divider = {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(Color.Cyan.copy(alpha = 0.2f))
                                .border(
                                    1.dp,
                                    Color.Cyan,
                                    CircleShape
                                )
                        )
                    },
                    labelContent = {
                        Text(
                            text = "m",
                            color = Color.Cyan.copy(alpha = 0.4f),
                            modifier = Modifier.offset(x = 6.dp)
                        )
                    },
                    onItemSelected = { _, _ -> }
                )
            }
        }
    }
}