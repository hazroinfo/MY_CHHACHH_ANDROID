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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mychhachh.app.data.User
import com.mychhachh.app.ui.components.*
import com.mychhachh.app.ui.theme.JellyMuted

@Composable
fun PeopleScreen(
    currentUserId: Long,
    users: List<User>, loading: Boolean, error: String?, query: String,
    onQuery: (String) -> Unit, onSearch: () -> Unit, onOpen: (Long) -> Unit, onFollow: (Long) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { PageTitle("People", "Find people from Chhachh", JellyIcons.People) }
        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 9.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        query,
                        onQuery,
                        Modifier.weight(1f),
                        placeholder = { Text("Search people") },
                        singleLine = true,
                        shape = RoundedCornerShape(17.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    JellyButton("Search", icon = JellyIcons.Search, onClick = onSearch)
                }
            }
        }
        if (loading && users.isEmpty()) item { LoadingBlock() }
        error?.let { item { ErrorCard(it, onSearch) } }
        if (!loading && users.isEmpty() && error == null) item { EmptyCard("No people found.", JellyIcons.People) }
        items(users, key = { "person-${it.id}" }) { u ->
            JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(u, 50.dp, Modifier.clickable { onOpen(u.id) })
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f).clickable { onOpen(u.id) }) {
                        UserName(u, 14)
                        if (u.username.isNotBlank()) Text("@${u.username}", color = JellyMuted, fontSize = 10.5f.sp)
                        val loc = listOf(u.area, u.village, u.city).filter { it.isNotBlank() }.joinToString(" • ")
                        if (loc.isNotBlank()) Text(loc, color = JellyMuted, fontSize = 9.5f.sp)
                    }
                    if (u.id != currentUserId) {
                        JellyButton(
                            if (u.followed) "Following" else "Follow",
                            primary = !u.followed,
                            icon = JellyIcons.Follow
                        ) { onFollow(u.id) }
                    }
                }
            }
        }
    }
}
