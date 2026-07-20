package io.github.zeroone3010.turtleviewer.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.zeroone3010.turtleviewer.model.OpenedFile
import io.github.zeroone3010.turtleviewer.model.ViewerContent
import org.junit.Rule
import org.junit.Test

class ViewerScreenTest {
    @get:Rule val composeRule = createComposeRule()
    @Test fun emptyStateShowsOpenFile() { composeRule.setContent { ViewerScreen(ViewerUiState(), {}) }; composeRule.onNodeWithText("Open file").assertIsDisplayed() }
    @Test fun loadedTextIsDisplayed() { composeRule.setContent { ViewerScreen(ViewerUiState(content = ViewerContent.Text("@prefix ex: <https://example.com/>.")), {}) }; composeRule.onNodeWithText("@prefix ex: <https://example.com/>.").assertIsDisplayed() }
}
