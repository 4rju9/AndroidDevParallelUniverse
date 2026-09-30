package app.netlify.dev4rju9.androiddevparalleluniverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.netlify.dev4rju9.androiddevparalleluniverse.previews.animatedtablayout.AnimatedTabLayoutScreen
import app.netlify.dev4rju9.androiddevparalleluniverse.previews.rootdetection.RootDetectionScreen
import app.netlify.dev4rju9.androiddevparalleluniverse.previews.rootdetection.RootDetectionViewModel
import app.netlify.dev4rju9.androiddevparalleluniverse.previews.wheelpicker.WheelPickerScreen
import app.netlify.dev4rju9.androiddevparalleluniverse.ui.theme.AndroidDevParallelUniverseTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            AndroidDevParallelUniverseTheme {
                val viewModel = RootDetectionViewModel()

                var selectedModule by remember { mutableStateOf(Modules.INITIAL) }
                var selectedTabIndex by remember { mutableIntStateOf(0) }
                val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val isInitial = selectedModule == Modules.INITIAL

                Scaffold(
                    topBar = {
                        TopAppBar(
                            modifier = Modifier.fillMaxWidth(),
                            title = {
                                Text(
                                    text = selectedModule.title,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                if (!isInitial) {
                                    IconButton(
                                        onClick = {
                                            selectedModule = Modules.INITIAL
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                }
                            }
                        )
                    }
                ) { innerPadding ->

                    val modifier = Modifier.fillMaxSize().padding(innerPadding)

                    when (selectedModule) {
                        Modules.INITIAL -> {
                            LazyColumn(
                                modifier = modifier
                                    .padding(32.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.Top),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                items(Modules.entries) { module ->
                                    if (module != Modules.INITIAL) {
                                        Button(
                                            onClick = {
                                                selectedModule = module
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            Text(
                                                text = module.title,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Modules.ROOT_DETECTOR -> RootDetectionScreen(modifier, viewModel)
                        Modules.WHEEL_PICKER -> WheelPickerScreen(modifier.padding(32.dp))
                        Modules.ANIMATED_TAB_LAYOUT -> AnimatedTabLayoutScreen(modifier.padding(vertical = 32.dp, horizontal = 8.dp))
                    }
                }

            }
        }
    }

}

enum class Modules(val title: String) {
    INITIAL("Android Dev Parallel Universe"),
    ROOT_DETECTOR("Root Detector"),
    WHEEL_PICKER("Wheel Picker"),
    ANIMATED_TAB_LAYOUT("Animated Tab Layout")
}