package app.netlify.dev4rju9.wheelpickerdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.netlify.dev4rju9.wheelpickerdemo.components.WheelPicker
import app.netlify.dev4rju9.wheelpickerdemo.ui.theme.WheelPickerDemoTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WheelPickerDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var contentSize by remember { mutableFloatStateOf(25f) }
                    var contentHeight by remember { mutableFloatStateOf(40f) }
                    var dividerHeightFactor by remember { mutableFloatStateOf(1f) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text("Content Size", color = Color.Cyan)

                            Slider(
                                value = contentSize,
                                onValueChange = { contentSize = it },
                                valueRange = 25f..50f,
                                steps = 10,
                                colors = SliderDefaults.colors().copy(
                                    thumbColor = Color.Cyan,
                                    activeTrackColor = Color.Cyan.copy(alpha = 0.4f),
                                    inactiveTrackColor = Color.Cyan.copy(alpha = 0.4f)
                                )
                            )

                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text("Content Height", color = Color.Cyan)

                            Slider(
                                value = contentHeight,
                                onValueChange = { contentHeight = it },
                                valueRange = 40f..100f,
                                steps = 10,
                                colors = SliderDefaults.colors().copy(
                                    thumbColor = Color.Cyan,
                                    activeTrackColor = Color.Cyan.copy(alpha = 0.4f),
                                    inactiveTrackColor = Color.Cyan.copy(alpha = 0.4f)
                                )
                            )

                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text("Divider Width", color = Color.Cyan)

                            Slider(
                                value = dividerHeightFactor,
                                onValueChange = { dividerHeightFactor = it },
                                valueRange = 1f..3f,
                                steps = 10,
                                colors = SliderDefaults.colors().copy(
                                    thumbColor = Color.Cyan,
                                    activeTrackColor = Color.Cyan.copy(alpha = 0.4f),
                                    inactiveTrackColor = Color.Cyan.copy(alpha = 0.4f)
                                )
                            )

                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            WheelPicker(
                                modifier = Modifier.fillMaxWidth().height((contentHeight.roundToInt() * 3).dp),
                                items = (10..20).map { "$it.0" },
                                initialSelected = 0,
                                dividerWidth = 120.dp,
                                contentHeight = (contentHeight.roundToInt()).dp,
                                contentSize = Pair((contentSize.roundToInt()).sp, (contentSize.roundToInt() - 15).sp),
                                contentColor = Pair(Color.Cyan, Color.Cyan.copy(alpha = 0.4f)),
                                thickness = 2.0,
                                dividerHeightFactor = dividerHeightFactor,
                                label = "made with ❤️",
                                labelSize = 16.sp,
                                showLabel = true,
                                visibleItems = 3,
                                enabled = true
                            ) { index, current ->
                                println("Temperature: $current at index - $index")
                            }
                        }
                    }
                }
            }
        }
    }
}