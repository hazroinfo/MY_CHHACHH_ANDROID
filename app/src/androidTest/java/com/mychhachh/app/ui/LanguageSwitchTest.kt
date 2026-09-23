package com.mychhachh.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.mychhachh.app.ui.components.AppLanguage
import com.mychhachh.app.ui.components.JellyButton
import com.mychhachh.app.ui.theme.MyChhachhTheme
import org.junit.Rule
import org.junit.Test

class LanguageSwitchTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun switchesBetweenUrduAndEnglishWithoutRestart() {
        AppLanguage.set("ur")
        composeRule.setContent {
            MyChhachhTheme {
                JellyButton("Home", onClick = {})
            }
        }
        composeRule.onNodeWithText("ہوم").assertIsDisplayed()

        composeRule.runOnIdle { AppLanguage.set("en") }
        composeRule.onNodeWithText("Home").assertIsDisplayed()

        composeRule.runOnIdle { AppLanguage.set("en") }
    }
}
