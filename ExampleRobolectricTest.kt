package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.TuneShiftDatabase
import com.example.data.TuneShiftRepository
import com.example.domain.model.MatchStatus
import com.example.domain.model.MusicPlatform
import com.example.ui.TuneShiftViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app name matches TuneShift`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("TuneShift", context.getString(R.string.app_name))
    }

    @Test
    fun `D E F G H I K full review workflow confirm reject replace unmatched retry skip and summary totals`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Your music. Anywhere.").assertIsDisplayed()

        // 1. Navigate to Transfer Workspace -> Import
        composeRule.onNodeWithTag("start_transfer_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("workspace_list")
            .performScrollToNode(hasTestTag("continue_to_import_button"))
        composeRule.onNodeWithTag("continue_to_import_button").performClick()
        composeRule.waitForIdle()

        // 2. Insert example tracklist and parse
        composeRule.onNodeWithTag("import_method_structured_text").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("import_list")
            .performScrollToNode(hasTestTag("insert_example_tracks_button"))
        composeRule.onNodeWithTag("insert_example_tracks_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("import_list")
            .performScrollToNode(hasTestTag("parse_structured_text_button"))
        composeRule.onNodeWithTag("parse_structured_text_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("preview_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()

        // 3. Deselect duplicate and run matching
        composeRule.onNodeWithTag("preview_list")
            .performScrollToNode(hasTestTag("deselect_duplicates_button"))
        composeRule.onNodeWithTag("deselect_duplicates_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("preview_list")
            .performScrollToNode(hasTestTag("run_matching_button"))
        composeRule.onNodeWithTag("run_matching_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("matching_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()

        // 4. (G) Reject an auto-accepted High confidence match (#1 Midnight City) -> moves to NO_MATCH
        composeRule.onNodeWithTag("matching_list")
            .performScrollToNode(hasTestTag("reject_match_button_1"))
        composeRule.onNodeWithTag("reject_match_button_1").performClick()
        composeRule.waitForIdle()

        // 5. Confirm a Needs review match (#6 Blinding Lights (Live))
        composeRule.onNodeWithTag("matching_list")
            .performScrollToNode(hasTestTag("confirm_match_button_6"))
        composeRule.onNodeWithTag("confirm_match_button_6").performClick()
        composeRule.waitForIdle()

        // 6. Open dedicated Unmatched Tracks screen
        composeRule.onNodeWithTag("matching_list")
            .performScrollToNode(hasTestTag("open_unmatched_screen_button"))
        composeRule.onNodeWithTag("open_unmatched_screen_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("unmatched_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()

        // 7. (F) Manually replace rejected track #1 via Search/Replace dialog
        composeRule.onNodeWithTag("unmatched_list")
            .performScrollToNode(hasTestTag("unmatched_search_replace_1"))
        composeRule.onNodeWithTag("unmatched_search_replace_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("correction_title_input").performTextReplacement("Midnight City (Radio Edit)")
        composeRule.onNodeWithTag("correction_artist_input").performTextReplacement("M83")
        composeRule.onNodeWithTag("save_correction_button").performClick()
        composeRule.waitForIdle()

        // 8. Edit & Retry on unmatched track #7 ("Track 07 Without Artist")
        composeRule.onNodeWithTag("unmatched_list")
            .performScrollToNode(hasTestTag("unmatched_edit_retry_7"))
        composeRule.onNodeWithTag("unmatched_edit_retry_7").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("retry_title_input").performTextReplacement("Track 07")
        composeRule.onNodeWithTag("retry_artist_input").performTextReplacement("Studio Artist")
        composeRule.onNodeWithTag("retry_single_match_button").performClick()
        composeRule.waitForIdle()

        // 9. Navigate back to Match Review -> Summary & verify summary actions
        composeRule.onNodeWithTag("nav_back_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("matching_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("matching_list")
            .performScrollToNode(hasTestTag("continue_to_summary_button"))
        composeRule.onNodeWithTag("continue_to_summary_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodesWithTag("summary_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("summary_list")
            .performScrollToNode(hasTestTag("export_csv_button"))
        composeRule.onNodeWithTag("export_csv_button").assertIsDisplayed()
        composeRule.onNodeWithTag("summary_list")
            .performScrollToNode(hasTestTag("copy_tracklist_button"))
        composeRule.onNodeWithTag("copy_tracklist_button").performClick()
        composeRule.waitForIdle()
    }

    private fun waitUntilLooperCondition(timeoutMs: Long = 5000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < deadline) {
            ShadowLooper.shadowMainLooper().idle()
            Thread.sleep(15)
        }
        ShadowLooper.shadowMainLooper().idle()
    }

    @Test
    fun `J persistence and refresh of migration state across repository and viewmodel reload`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(app, TuneShiftDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = TuneShiftRepository(db.tuneShiftDao())

        val vm1 = TuneShiftViewModel(app, repo)
        val collectJob1 = launch(kotlinx.coroutines.Dispatchers.Unconfined) { vm1.uiState.collect {} }
        vm1.selectSourcePlatform(MusicPlatform.SPOTIFY)
        vm1.selectDestinationPlatform(MusicPlatform.APPLE_MUSIC)

        val catalogCsv = "Title,Artist,Album,Duration\nRedbone,Childish Gambino,Awaken My Love,5:26\nMidnight City,M83,Hurry Up We're Dreaming,4:03"
        val sourceCsv = "Title,Artist,Album,Duration\nRedbone,Childish Gambino,Awaken My Love,5:26\nBlinding Lights (Live),The Weeknd,,3:20\nObscure Song,Nobody,,"

        vm1.importDestinationCatalogCsv(catalogCsv, "apple_catalog.csv")
        waitUntilLooperCondition { vm1.uiState.value.destinationCatalog.isNotEmpty() }

        var importedDone = false
        vm1.importFromCsvContent(sourceCsv, "my_playlist.csv") { importedDone = true }
        waitUntilLooperCondition { importedDone }
        assertTrue(importedDone)

        var matchedDone = false
        vm1.runMatchingEngine { matchedDone = true }
        waitUntilLooperCondition { matchedDone }
        assertTrue(matchedDone)

        val stateBeforeDecisions = vm1.uiState.value
        val savedId = stateBeforeDecisions.activePlaylistId
        val track1 = stateBeforeDecisions.matchResults[0]
        val track2 = stateBeforeDecisions.matchResults[1]
        val track3 = stateBeforeDecisions.matchResults[2]
        assertEquals(MatchStatus.HIGH_CONFIDENCE, track1.status)
        assertEquals(MatchStatus.NO_MATCH, track2.status)
        assertEquals(MatchStatus.NO_MATCH, track3.status)

        vm1.rejectMatch(track1.trackId)
        val searchHits = vm1.searchReplacementCandidates("Midnight City")
        assertTrue(searchHits.isNotEmpty())
        vm1.selectReplacementCandidate(track2.trackId, searchHits.first())
        vm1.skipTrack(track3.trackId)
        waitUntilLooperCondition {
            val res = vm1.uiState.value.matchResults
            res.size == 3 &&
                res[0].status == MatchStatus.NO_MATCH &&
                res[1].status == MatchStatus.HIGH_CONFIDENCE &&
                res[2].status == MatchStatus.SKIPPED
        }
        // Allow async Room savePlaylistSession coroutine to commit
        Thread.sleep(150)
        ShadowLooper.shadowMainLooper().idle()

        val expectedSummary = vm1.uiState.value.summary
        assertEquals(3, expectedSummary.totalOriginalTracks)
        assertEquals(1, expectedSummary.confirmedMatches)
        assertEquals(1, expectedSummary.unmatchedTracks)
        assertEquals(1, expectedSummary.skippedTracks)
        assertEquals(0, expectedSummary.needsReviewMatches)
        collectJob1.cancel()

        val vm2 = TuneShiftViewModel(app, repo)
        val collectJob2 = launch(kotlinx.coroutines.Dispatchers.Unconfined) { vm2.uiState.collect {} }
        var loaded = false
        var loadedHasMatches = false
        vm2.loadSavedPlaylist(savedId) { hasMatches ->
            loadedHasMatches = hasMatches
            loaded = true
        }
        waitUntilLooperCondition { loaded }
        assertTrue(loaded)
        assertTrue(loadedHasMatches)

        val restored = vm2.uiState.value
        assertEquals(3, restored.matchResults.size)
        assertEquals(MatchStatus.NO_MATCH, restored.matchResults[0].status)
        assertEquals(MatchStatus.HIGH_CONFIDENCE, restored.matchResults[1].status)
        assertEquals("Midnight City", restored.matchResults[1].matchedTitle)
        assertEquals(MatchStatus.SKIPPED, restored.matchResults[2].status)
        assertFalse(restored.matchResults[1].autoAccepted)
        assertNotNull(restored.summary)
        assertEquals(expectedSummary, restored.summary)

        collectJob2.cancel()
        db.close()
    }

    @Test
    fun `faq and privacy navigation and clear local data dialog work`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_faq").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("FAQ & Integration Roadmap").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_privacy").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("privacy_list")
            .performScrollToNode(hasTestTag("clear_local_data_button"))
        composeRule.onNodeWithTag("clear_local_data_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_clear_data_button").performClick()
        composeRule.waitForIdle()
    }
}
