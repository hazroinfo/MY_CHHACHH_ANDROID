package com.mychhachh.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mychhachh.app.data.User
import com.mychhachh.app.ui.components.*
import com.mychhachh.app.ui.theme.JellyInk
import com.mychhachh.app.ui.theme.JellyMuted

@Composable
fun PeopleScreen(
    currentUserId: Long,
    users: List<User>, loading: Boolean, error: String?, query: String,
    onQuery: (String) -> Unit, onSearch: () -> Unit, onOpen: (Long) -> Unit, onFollow: (Long) -> Unit,
    hasMore: Boolean = false, onLoadMore: () -> Unit = {}
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp, 8.dp, 10.dp, 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item { PageTitle("People", "Find and connect with people across Chhachh", JellyIcons.People) }
        item {
            JellyGlass(Modifier.fillMaxWidth(), padding = 12.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (query.isBlank()) "People You May Know" else "People Results",
                                color = JellyInk,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Text(
                                if (query.isBlank()) "Suggested community connections" else "Matching people",
                                color = JellyMuted,
                                fontSize = 9.5f.sp
                            )
                        }
                        Text(users.size.toString(), color = JellyMuted, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }

                    if (loading && users.isEmpty()) {
                        LoadingBlock()
                    } else if (users.isEmpty() && error == null) {
                        EmptyCard("No people found.", JellyIcons.People)
                    }
                }
            }
        }
        error?.let { item { ErrorCard(it, onSearch) } }

        items(users, key = { "person-${it.id}" }) { u ->
            JellyGlass(Modifier.fillMaxWidth(), padding = 10.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(u, 52.dp, Modifier.clickable { onOpen(u.id) })
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f).clickable { onOpen(u.id) }) {
                        UserName(u, 14)
                        if (u.username.isNotBlank()) Text("@${u.username}", color = JellyMuted, fontSize = 10.sp)
                        val place = listOf(u.city, u.village).filter { it.isNotBlank() }.joinToString(" · ")
                        if (place.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                JellyIcon(JellyIcons.Pin, size = 16.dp)
                                Spacer(Modifier.width(3.dp))
                                Text(place, color = JellyMuted, fontSize = 9.sp)
                            }
                        }
                        val detail = u.work.ifBlank { u.school }
                        if (detail.isNotBlank()) Text(detail.take(42), color = JellyMuted, fontSize = 8.7f.sp, maxLines = 1)
                        if (u.relationshipStatus.isNotBlank()) {
                            Text(
                                u.relationshipStatus.replace('_', ' '),
                                color = JellyMuted,
                                fontSize = 8.4f.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (u.id != currentUserId) {
                        JellyButton(
                            if (u.followed) "Following" else "Follow",
                            primary = !u.followed
                        ) { onFollow(u.id) }
                    }
                }
            }
        }

        if (hasMore) {
            item {
                JellyButton(
                    text = if (loading) "Loading…" else "Load more people",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading,
                    onClick = onLoadMore
                )
            }
        }
    }
}
