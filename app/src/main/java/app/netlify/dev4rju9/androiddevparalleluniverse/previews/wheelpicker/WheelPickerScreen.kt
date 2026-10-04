package app.netlify.dev4rju9.androiddevparalleluniverse.previews.wheelpicker

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.netlify.dev4rju9.wheelpicker.WheelPicker

@Composable
fun WheelPickerScreen(
    modifier: Modifier = Modifier
) {
    var selectedAge by remember { mutableStateOf("18") }
    var selectedMonth by remember { mutableStateOf("January") }
    var selectedWeight by remember { mutableStateOf("40") }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 1. Basic Wheel Picker
        Text(
            text = "Basic Picker",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        WheelPicker(
            items = (18..60).map { it.toString() },
            itemContent = { item, isSelected ->
                Text(
                    text = item,
                    fontSize = if (isSelected) 22.sp else 18.sp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            },
            onItemSelected = { _, item ->
                selectedAge = item
            }
        )

        Text(
            text = "Selected age: $selectedAge",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 2. Picker with dividers
        Text(
            text = "Month Picker",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        WheelPicker(
            items = listOf(
                "January",
                "February",
                "March",
                "April",
                "May",
                "June",
                "July",
                "August",
                "September",
                "October",
                "November",
                "December"
            ),
            itemContent = { item, isSelected ->
                Text(
                    text = item,
                    fontSize = if (isSelected) 22.sp else 18.sp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            },
            enableDivider = true,
            dividerWidth = 120.dp,
            divider = {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(120.dp)
                        .height(40.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )
                }
            },
            onItemSelected = { _, item ->
                selectedMonth = item
            }
        )

        Text(
            text = "Selected month: $selectedMonth",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 3. Picker with label
        Text(
            text = "Weight Picker",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        WheelPicker(
            items = (40..120).map { it.toString() },
            itemContent = { item, isSelected ->
                Text(
                    text = item,
                    fontSize = if (isSelected) 22.sp else 18.sp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            },
            enableDivider = true,
            dividerWidth = 80.dp,
            divider = {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(80.dp)
                        .height(40.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            },
            labelContent = {
                Text(
                    text = "kg",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            },
            onItemSelected = { _, item ->
                selectedWeight = item
            }
        )

        Text(
            text = "Selected weight: $selectedWeight kg",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Composable
fun WheelPickerPreview() {
    MaterialTheme {
        Surface {
            Box(
                modifier = Modifier.padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                val sampleItems = (1..10).map { "%02d".format(it) }

                WheelPicker(
                    items = sampleItems,
                    initialIndex = 4,
                    itemContent = { item, isSelected ->
                        Text(
                            text = item,
                            fontSize = if (isSelected) 22.sp else 18.sp,
                            color = if (isSelected) {
                                Color.Black
                            } else {
                                Color.LightGray
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
                                .height(40.dp)
                                .border(
                                    width = 1.dp,
                                    color = Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                    },
                    labelContent = {
                        Text(
                            text = "Hrs",
                            color = Color.Gray,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    },
                    onItemSelected = { _, _ -> }
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF222222
)
@Composable
fun WheelPickerDarkPreview() {
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
                    initialIndex = 0,
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
                    enableDivider = false,
                    onItemSelected = { _, _ -> }
                )
            }
        }
    }
}