package app.netlify.dev4rju9.wheelpicker

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun WheelPicker(
    modifier: Modifier = Modifier,
    items: List<String>,
    initialIndex: Int = 0,
    // Item Styling
    itemHeight: Dp = 40.dp,
    selectedTextColor: Color,
    unselectedTextColor: Color,
    selectedTextSize: TextUnit = 25.sp,
    unselectedTextSize: TextUnit = 20.sp,
    // Divider Styling
    enableDivider: Boolean = false,
    dividerColor: Color = Color.Cyan,
    dividerThickness: Dp = 1.dp,
    dividerWidth: Dp = 60.dp,
    dividerSpacingMultiplier: Float = 1f,
    // Label Styling
    label: String? = null,
    labelColor: Color = Color.Cyan.copy(alpha = 0.4f),
    labelSize: TextUnit = 20.sp,
    // Behavior
    visibleItemsCount: Int = 3,
    enabled: Boolean = true,
    onItemSelected: (index: Int, item: String) -> Unit
) {
    val paddedItems = listOf("") + items + listOf("")
    val listState = rememberLazyListState(initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(listState)

    val selectedIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf initialIndex
            val center = layoutInfo.viewportEndOffset / 2
            layoutInfo.visibleItemsInfo.find {
                val itemCenter = it.offset + it.size / 2
                itemCenter in (center - it.size / 2)..(center + it.size / 2)
            }?.index ?: initialIndex
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { selectedIndex }
            .distinctUntilChanged()
            .collect { index ->
                val realIndex = (index - 1).coerceIn(items.indices)
                items.getOrNull(realIndex)?.let {
                    onItemSelected(realIndex, it)
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
                        Text(
                            text = item,
                            fontSize = if (isSelected) selectedTextSize else unselectedTextSize,
                            color = if (isSelected) selectedTextColor else unselectedTextColor
                        )
                    }
                }
            }

            if (enableDivider) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .height(itemHeight * dividerSpacingMultiplier),
                    verticalArrangement = Arrangement.Center
                ) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = dividerColor,
                        thickness = dividerThickness
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = dividerColor,
                        thickness = dividerThickness
                    )
                }
            }
        }

        label?.let {
            Text(
                text = it,
                fontSize = labelSize,
                color = labelColor
            )
        }
    }
}