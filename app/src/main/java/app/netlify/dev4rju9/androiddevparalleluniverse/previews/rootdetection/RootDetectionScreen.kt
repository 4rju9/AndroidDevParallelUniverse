package app.netlify.dev4rju9.androiddevparalleluniverse.previews.rootdetection

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.netlify.dev4rju9.androiddevparalleluniverse.ui.theme.AndroidDevParallelUniverseTheme
import app.netlify.dev4rju9.rootdetection.DeviceStateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootDetectionScreen(
    viewModel: RootDetectionViewModel
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text(
                        text = "Root Detection",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(
                16.dp,
                alignment = Alignment.Top
            ),
            horizontalAlignment = Alignment.Start
        ) {

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        alignment = Alignment.Start
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status :- ",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = uiState.status,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        alignment = Alignment.Start
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Button(
                        enabled = !uiState.loading,
                        onClick = {
                            scope.launch(Dispatchers.IO) {

                                viewModel.setLoading()

                                val result = DeviceStateUtils.evaluateStartupStateWithContent(
                                    context = applicationContext,
                                    shouldEnableTimingCheck = true
                                ) {
                                    scope.launch(Dispatchers.Main) { Toast.makeText(context, "Device is safe !", Toast.LENGTH_SHORT).show() }
                                }

                                viewModel.updateResult(result)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = if (uiState.loading) {
                                "Checking..."
                            } else {
                                "Start"
                            },
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = viewModel::clear,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Clear",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (uiState.elapsedMs > 0) {
                item {
                    Text(
                        text = "Elapsed Time :- ${uiState.elapsedMs} ms",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            itemsIndexed(uiState.flags) { index, flag ->

                if (index == 0) {
                    Text(
                        text = "Signals",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = flag,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontStyle = FontStyle.Italic
                    )
                } else {
                    Text(
                        text = flag,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun RootDetectionPreview() {
    AndroidDevParallelUniverseTheme {
        val viewModel = RootDetectionViewModel()
        RootDetectionScreen(viewModel)
    }
}