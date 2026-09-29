package app.netlify.dev4rju9.androiddevparalleluniverse.previews.wheelpicker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.netlify.dev4rju9.wheelpicker.WheelPicker

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