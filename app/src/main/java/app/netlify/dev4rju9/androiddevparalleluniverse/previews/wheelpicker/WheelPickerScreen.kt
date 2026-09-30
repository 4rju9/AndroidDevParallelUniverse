package app.netlify.dev4rju9.androiddevparalleluniverse.previews.wheelpicker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    var selectedAge by remember { mutableStateOf("25") }
    var selectedMonth by remember { mutableStateOf("January") }
    var selectedWeight by remember { mutableStateOf("60") }

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
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedTextSize = 22.sp,
            unselectedTextSize = 18.sp,
            enableDivider = true,
            dividerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            dividerWidth = 120.dp,
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
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            enableDivider = true,
            dividerColor = MaterialTheme.colorScheme.primary,
            dividerWidth = 80.dp,
            label = "kg",
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            labelSize = 18.sp,
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

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun WheelPickerPreview() {
    MaterialTheme {
        Surface {
            Box(
                modifier = Modifier.padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                val sampleItems = (1..10).map { String.format("%02d", it) }

                WheelPicker(
                    items = sampleItems,
                    initialIndex = 4,
                    selectedTextColor = Color.Black,
                    unselectedTextColor = Color.LightGray,
                    enableDivider = true,
                    dividerColor = Color.LightGray,
                    label = "Hrs",
                    labelColor = Color.Gray,
                    onItemSelected = { index, item ->
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222222)
@Composable
fun WheelPickerDarkPreview() {
    MaterialTheme {
        Surface(color = Color(0xFF222222)) {
            Box(
                modifier = Modifier.padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

                WheelPicker(
                    items = months,
                    initialIndex = 0,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.DarkGray,
                    enableDivider = false,
                    onItemSelected = { _, _ -> }
                )
            }
        }
    }
}