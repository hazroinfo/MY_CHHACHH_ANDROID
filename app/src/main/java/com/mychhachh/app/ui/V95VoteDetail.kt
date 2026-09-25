package com.mychhachh.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun V95VoteDetail(c: V95Controller) {
    val vote = c.selectedVote
    if (vote == null) {
        V95Empty(c.t("Voting unavailable", "ووٹنگ دستیاب نہیں"))
        return
    }

    var comment by remember(vote.id) { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 7.dp),
        contentPadding = PaddingValues(top = 6.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                V95Button(c.t("Back", "واپس")) { c.route = V95Route.VOTES }
                Spacer(Modifier.width(8.dp))
                V95PageHeading(c.t("Voting", "ووٹنگ"), "")
            }
        }

        item { V95VoteCard(c, vote, detail = true) }

        item {
            V95GlassCard(radius = 22.dp, padding = 12.dp) {
                Text(c.t("Comments", "کمنٹس"), color = V95Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                if (c.user == null) {
                    V95Button(c.t("Login to comment", "کمنٹ کے لیے لاگ اِن کریں"), primary = true) { c.route = V95Route.AUTH }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        V95InputShell(Modifier.weight(1f)) {
                            BasicTextField(
                                comment,
                                { comment = it },
                                Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(color = V95Ink, fontSize = 13.sp),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (comment.isBlank()) Text(c.t("Write a comment…", "کمنٹ لکھیں…"), color = V95Muted, fontSize = 12.sp)
                                        inner()
                                    }
                                }
                            )
                        }
                        V95Button(c.t("Send", "بھیجیں"), primary = true, enabled = comment.isNotBlank()) {
                            c.addVoteComment(comment) { comment = "" }
                        }
                    }
                }
            }
        }

        if (c.voteCommentsList.isEmpty()) {
            item { V95Empty(c.t("No comments yet", "ابھی کوئی کمنٹ نہیں")) }
        } else {
            items(c.voteCommentsList, key = { it.id }) { item ->
                V95GlassCard(radius = 19.dp, padding = 10.dp) {
                    Row(verticalAlignment = Alignment.Top) {
                        V95Avatar(item.user, 36.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.user.name, color = V95Ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Text(item.text, color = V95Ink, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}
