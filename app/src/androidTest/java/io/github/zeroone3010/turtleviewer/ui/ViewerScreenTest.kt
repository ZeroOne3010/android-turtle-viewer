package io.github.zeroone3010.turtleviewer.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.zeroone3010.turtleviewer.model.OpenedFile
import io.github.zeroone3010.turtleviewer.model.ViewerContent
import io.github.zeroone3010.turtleviewer.rdf.ReadableRdfState
import io.github.zeroone3010.turtleviewer.gpx.GpxDisplayItem
import io.github.zeroone3010.turtleviewer.gpx.GpxPoint
import org.junit.Rule
import org.junit.Test

class ViewerScreenTest {
    @get:Rule val composeRule = createComposeRule()
    @Test fun emptyStateShowsOpenFile() { composeRule.setContent { ViewerScreen(ViewerUiState(), {}) }; composeRule.onNodeWithText("Open file").assertIsDisplayed() }
    @Test fun loadedTextIsDisplayed() { composeRule.setContent { ViewerScreen(ViewerUiState(content = ViewerContent.Text("@prefix ex: <https://example.com/>.")), {}) }; composeRule.onNodeWithText("@prefix ex: <https://example.com/>.").assertIsDisplayed() }
    @Test fun darkModeToggleCanBeEnabled() {
        composeRule.setContent { ViewerScreen(ViewerUiState(content = ViewerContent.Text("text")), {}) }
        composeRule.onNodeWithText("Dark mode").performClick().assertIsSelected()
    }
    @Test fun fontSizeControlsRespectCaps() {
        composeRule.setContent { ViewerScreen(ViewerUiState(content = ViewerContent.Text("text")), {}) }
        repeat(10) { composeRule.onNodeWithContentDescription("Decrease font size").performClick() }
        composeRule.onNodeWithContentDescription("Decrease font size").assertIsNotEnabled()

        repeat(26) { composeRule.onNodeWithContentDescription("Increase font size").performClick() }
        composeRule.onNodeWithContentDescription("Increase font size").assertIsNotEnabled()
    }
    @Test fun gpxReadableTabRemainsAvailableWhileSourceHighlightingContinues() {
        composeRule.setContent { ViewerScreen(ViewerUiState(content = ViewerContent.Text("<gpx/>"), sourceLoading = true, readableGpx = ReadableGpxState.Ready(listOf(GpxDisplayItem.Point(GpxPoint(60.0, 25.0, null, null), "—", "60°0.000′ N, 25°0.000′ E", null, null, true)))), {}) }
        composeRule.onNodeWithText("Readable").assertIsDisplayed()
        composeRule.onNodeWithText("60°0.000′ N, 25°0.000′ E").assertIsDisplayed()
        composeRule.onNodeWithText("Source").performClick()
        composeRule.onNodeWithText("<gpx/>").assertIsDisplayed()
    }

    @Test fun gpxReadableTabIsSelectedWhileTheTrackIsLoading() {
        composeRule.setContent {
            ViewerScreen(
                ViewerUiState(
                    content = ViewerContent.Text("<gpx/>"),
                    readableGpx = ReadableGpxState.Loading
                ),
                {}
            )
        }

        composeRule.onNodeWithText("Readable").assertIsSelected()
        composeRule.onNodeWithText("Loading", substring = true).assertIsDisplayed()
    }

    @Test fun gpxSourceTabCanBeSelectedWhileTheTrackIsLoading() {
        composeRule.setContent {
            ViewerScreen(
                ViewerUiState(
                    content = ViewerContent.Text("<gpx/>"),
                    readableGpx = ReadableGpxState.Loading
                ),
                {}
            )
        }

        composeRule.onNodeWithText("Source").performClick().assertIsSelected()
        composeRule.onNodeWithText("<gpx/>").assertIsDisplayed()
    }

    @Test fun sourcePreparationRendersRawTextWhileHighlightingRuns() {
        composeRule.setContent {
            ViewerScreen(
                ViewerUiState(
                    content = ViewerContent.Text("<gpx/>"),
                    sourceLoading = true,
                    readableGpx = ReadableGpxState.Loading
                ),
                {}
            )
        }

        composeRule.onNodeWithText("Source").performClick()
        composeRule.onNodeWithText("<gpx/>").assertIsDisplayed()
    }
    @Test fun readableErrorCanShowTechnicalDetails() {
        composeRule.setContent {
            ViewerScreen(
                ViewerUiState(
                    content = ViewerContent.Text("@prefix ex: <https://example.test/>."),
                    readableRdf = ReadableRdfState.Error(
                        "Unable to build readable outline.",
                        "java.lang.IllegalStateException: broken outline"
                    )
                ),
                {}
            )
        }
        composeRule.onNodeWithText("Readable").performClick()
        composeRule.onNodeWithText("Show diagnostic details").performClick()
        composeRule.onNodeWithText("java.lang.IllegalStateException: broken outline").assertIsDisplayed()
    }
}
