package app.netlify.dev4rju9.wheelpickerdemo.components

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
    initialSelected: Int = 0,
    contentSize: Pair<TextUnit, TextUnit> = Pair(25.sp, 20.sp),
    contentHeight: Dp = 40.dp,
    contentColor: Pair<Color, Color>,
    thickness: Double = 1.2,
    dividerWidth: Dp = 60.dp,
    dividerHeightFactor: Float = 1f,
    label: String = "",
    labelSize: TextUnit = 20.sp,
    showLabel: Boolean = false,
    visibleItems: Int = 3,
    enabled: Boolean = true,
    onSelected: (Int, String) -> Unit
) {

    val paddedItems = listOf("") + items + listOf("")
    val listState = rememberLazyListState(initialSelected)

    val flingBehavior = rememberSnapFlingBehavior(listState)
    val selectedIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf initialSelected
            val center = layoutInfo.viewportEndOffset / 2
            layoutInfo.visibleItemsInfo.find {
                val itemCenter = it.offset + it.size / 2
                itemCenter in (center - it.size / 2)..(center + it.size / 2)
            }?.index ?: initialSelected
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { selectedIndex }
            .distinctUntilChanged()
            .collect { index ->
                val realIndex = (index - 1).coerceIn(items.indices)
                items.getOrNull(realIndex)?.let {
                    onSelected(realIndex, it)
                }
            }
    }

    Row (
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .width(dividerWidth),
            contentAlignment = Alignment.Center
        ) {

            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                userScrollEnabled = enabled,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight * visibleItems)
            ) {

                itemsIndexed(paddedItems) { index, item ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .height(contentHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item,
                            fontSize = if (isSelected) contentSize.first else contentSize.second,
                            color = if (isSelected) contentColor.first else contentColor.second
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(contentHeight*dividerHeightFactor),
                verticalArrangement = Arrangement.Center
            ) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Cyan,
                    thickness = thickness.dp
                )
                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Cyan,
                    thickness = thickness.dp
                )
            }
        }

        if (showLabel) {
            Text(
                text = label,
                fontSize = labelSize,
                color = Color.Cyan.copy(alpha = 0.4f)
            )
        }

    }
}
