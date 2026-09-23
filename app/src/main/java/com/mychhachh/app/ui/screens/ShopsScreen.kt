package com.mychhachh.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mychhachh.app.data.Shop
import com.mychhachh.app.ui.components.*
import com.mychhachh.app.ui.theme.JellyInk
import com.mychhachh.app.ui.theme.JellyMuted

@Composable
fun ShopsScreen(
    shops: List<Shop>, loading: Boolean, error: String?, query: String,
    onQuery: (String) -> Unit, onSearch: () -> Unit, onOpen: (Long) -> Unit, onFollow: (Long) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { PageTitle("Shops", "Local shops and services", JellyIcons.Shop) }
        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 9.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        query,
                        onQuery,
                        Modifier.weight(1f),
                        placeholder = { Text("Search shops") },
                        singleLine = true,
                        shape = RoundedCornerShape(17.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    JellyButton("Search", icon = JellyIcons.Search, onClick = onSearch)
                }
            }
        }
        if (loading && shops.isEmpty()) item { LoadingBlock() }
        error?.let { item { ErrorCard(it, onSearch) } }
        if (!loading && shops.isEmpty() && error == null) item { EmptyCard("No shops found.", JellyIcons.Shop) }
        items(shops, key = { "shop-${it.id}" }) { s ->
            JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(17.dp)).clickable { onOpen(s.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!s.photo.isNullOrBlank()) {
                            AsyncImage(s.photo, s.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else JellyIcon(JellyIcons.Shop, size = 42.dp)
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f).clickable { onOpen(s.id) }) {
                        Text(s.name, color = JellyInk, fontWeight = FontWeight.Black, fontSize = 14.5f.sp)
                        if (s.username.isNotBlank()) Text("@${s.username}", color = JellyMuted, fontSize = 10.sp)
                        if (s.category.isNotBlank()) Text(s.category, color = JellyMuted, fontSize = 9.5f.sp)
                        val loc = listOf(s.area, s.village, s.city).filter { it.isNotBlank() }.joinToString(" • ")
                        if (loc.isNotBlank()) Text(loc, color = JellyMuted, fontSize = 9.5f.sp)
                        if (s.followers > 0) Text("${s.followers} followers", color = JellyMuted, fontSize = 9.sp)
                    }
                    JellyButton(
                        if (s.followed) "Following" else "Follow",
                        primary = !s.followed,
                        icon = JellyIcons.Follow
                    ) { onFollow(s.id) }
                }
            }
        }
    }
}
