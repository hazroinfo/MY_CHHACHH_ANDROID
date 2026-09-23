package com.mychhachh.app.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mychhachh.app.data.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PeopleScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun peopleFollowAndLoadMoreCallbacksWork() {
        var followedId = 0L
        var loadMoreCalled = false

        composeRule.setContent {
            PeopleScreen(
                currentUserId = 1L,
                users = listOf(
                    User(id = 1L, name = "Current User", username = "current"),
                    User(
                        id = 2L,
                        name = "Test Person",
                        username = "testperson",
                        city = "Hazro",
                        village = "Test Village"
                    )
                ),
                loading = false,
                error = null,
                query = "",
                onQuery = {},
                onSearch = {},
                onOpen = {},
                onFollow = { followedId = it },
                hasMore = true,
                onLoadMore = { loadMoreCalled = true }
            )
        }

        composeRule.onNodeWithText("People You May Know").assertIsDisplayed()
        composeRule.onNodeWithText("Follow").performClick()
        composeRule.runOnIdle { assertEquals(2L, followedId) }

        composeRule.onNodeWithText("Load more people").performScrollTo().performClick()
        composeRule.runOnIdle { assertTrue(loadMoreCalled) }
    }
}
