package app.netlify.dev4rju9.androiddevparalleluniverse.previews.animatedtablayout

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.netlify.dev4rju9.animatedtablayout.AnimatedTabLayout

@Composable
fun AnimatedTabLayoutScreen(
    modifier: Modifier = Modifier
) {
    var selectedBasicTab by remember { mutableIntStateOf(0) }
    var selectedIconTab by remember { mutableIntStateOf(0) }
    var selectedCustomTab by remember { mutableIntStateOf(0) }

    val basicTabs = listOf("Home", "Profile", "Settings")

    Column(
        modifier = modifier,
    ) {

        // 1. Basic text tabs
        Text(
            text = "Basic Tabs",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedTabLayout(
            tabs = basicTabs.map { title ->
                {
                        isSelected ->
                    Text(
                        text = title,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (isSelected) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                }
            },
            selectedIndex = selectedBasicTab,
            onTabSelected = { selectedBasicTab = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            indicatorModifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 2. Icon + text tabs
        Text(
            text = "Icon + Text Tabs",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        val iconTabs = listOf(
            "Home" to Icons.Default.Home,
            "Search" to Icons.Default.Search,
            "Favorite" to Icons.Default.Favorite
        )

        AnimatedTabLayout(
            tabs = iconTabs.map { (title, icon) ->
                {
                        isSelected ->
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = title,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            },
            selectedIndex = selectedIconTab,
            onTabSelected = { selectedIconTab = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            indicatorModifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 3. Custom indicator
        Text(
            text = "Custom Indicator",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        val customTabs = listOf("Recent", "Popular", "Trending")

        AnimatedTabLayout(
            tabs = customTabs.map { title ->
                {
                        isSelected ->
                    Text(
                        text = title,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (isSelected) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        }
                    )
                }
            },
            selectedIndex = selectedCustomTab,
            onTabSelected = { selectedCustomTab = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            indicatorModifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

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
fun TabItem(text: String, isSelected: Boolean) {
    Text(
        text = text,
        color = if (isSelected) Color.White else Color.Gray,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
    )
}