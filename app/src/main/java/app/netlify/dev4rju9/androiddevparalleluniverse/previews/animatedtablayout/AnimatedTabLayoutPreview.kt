package app.netlify.dev4rju9.androiddevparalleluniverse.previews.animatedtablayout

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.netlify.dev4rju9.animatedtablayout.AnimatedTabLayout

@Preview
@Composable
fun AnimatedTabLayoutPreview() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    AnimatedTabLayout(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF1E1E1E)),
        indicatorModifier = Modifier
            .padding(4.dp)
            .background(Color(0xFF00BFA5), RoundedCornerShape(50)),
        selectedIndex = selectedTabIndex,
        onTabSelected = { selectedTabIndex = it },
        animationSpec = tween(400),
        tabs = listOf(
            { isSelected ->
                TabItem(text = "One", isSelected = isSelected)
            },
            { isSelected ->
                TabItem(text = "Two", isSelected = isSelected)
            },
            { isSelected ->
                TabItem(text = "Three", isSelected = isSelected)
            }
        )
    )
}

@Composable
private fun TabItem(text: String, isSelected: Boolean) {
    Text(
        text = text,
        color = if (isSelected) Color.White else Color.Gray,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
    )
}