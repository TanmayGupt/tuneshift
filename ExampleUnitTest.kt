package com.example

import com.example.domain.adapter.CsvCodec
import com.example.domain.adapter.PlatformAdapterRegistry
import com.example.domain.matching.TextNormalizer
import com.example.domain.matching.TrackMatchingEngine
import com.example.domain.model.MatchStatus
import com.example.domain.model.MigrationSummarySnapshot
import com.example.domain.model.MusicPlatform
import com.example.domain.model.SourceTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `A exact title and artist match achieves high confidence and auto-accepts`() {
        val source = listOf(
            SourceTrack(
                id = "s1",
                position = 1,
                rawTitle = "Midnight City",
                rawArtist = "M83",
                album = "Hurry Up, We're Dreaming",
                durationMs = 243_000L
            )
        )
        val catalog = listOf(
            SourceTrack(
                id = "c1",
                position = 1,
                rawTitle = "Midnight City",
                rawArtist = "M83",
                album = "Hurry Up, We're Dreaming",
                durationMs = 243_500L
            )
        )

        val results = TrackMatchingEngine.matchTracks(source, MusicPlatform.YOUTUBE_MUSIC, catalog)
        assertEquals(1, results.size)
        assertEquals(MatchStatus.HIGH_CONFIDENCE, results[0].status)
        assertTrue(results[0].autoAccepted)
        assertTrue(results[0].confidence >= 95)
    }

    @Test
    fun `B punctuation and capitalization differences normalize and match reliably`() {
        val source = listOf(
            SourceTrack(id = "s1", position = 1, rawTitle = "DON'T STOP BELIEVIN'", rawArtist = "Journey"),
            SourceTrack(id = "s2", position = 2, rawTitle = "Losing My Religion", rawArtist = "R.E.M.")
        )
        val catalog = listOf(
            SourceTrack(id = "c1", position = 1, rawTitle = "Dont Stop Believin", rawArtist = "JOURNEY"),
            SourceTrack(id = "c2", position = 2, rawTitle = "Losing My Religion", rawArtist = "REM")
        )

        val results = TrackMatchingEngine.matchTracks(source, MusicPlatform.SPOTIFY, catalog)
        assertEquals(MatchStatus.HIGH_CONFIDENCE, results[0].status)
        assertEquals(MatchStatus.HIGH_CONFIDENCE, results[1].status)
        assertTrue(results[0].confidence >= 88)
        assertTrue(results[1].confidence >= 88)
    }

    @Test
    fun `C featuring artists and title variations match or flag edition differences`() {
        val source = listOf(
            SourceTrack(
                id = "s1",
                position = 1,
                rawTitle = "Instant Crush (feat. Julian Casablancas)",
                rawArtist = "Daft Punk"
            ),
            SourceTrack(
                id = "s2",
                position = 2,
                rawTitle = "Dreams (2004 Remaster)",
                rawArtist = "Fleetwood Mac"
            ),
            SourceTrack(
                id = "s3",
                position = 3,
                rawTitle = "Blinding Lights (Live)",
                rawArtist = "The Weeknd"
            )
        )
        val catalog = listOf(
            SourceTrack(
                id = "c1",
                position = 1,
                rawTitle = "Instant Crush",
                rawArtist = "Daft Punk & Julian Casablancas"
            ),
            SourceTrack(
                id = "c2",
                position = 2,
                rawTitle = "Dreams",
                rawArtist = "Fleetwood Mac"
            ),
            SourceTrack(
                id = "c3",
                position = 3,
                rawTitle = "Blinding Lights",
                rawArtist = "The Weeknd"
            )
        )

        val results = TrackMatchingEngine.matchTracks(source, MusicPlatform.APPLE_MUSIC, catalog)
        assertEquals(MatchStatus.HIGH_CONFIDENCE, results[0].status)
        assertEquals(MatchStatus.HIGH_CONFIDENCE, results[1].status)
        // Live vs studio mismatch must NOT be silently auto-accepted
        assertEquals(MatchStatus.NEEDS_REVIEW, results[2].status)
        assertFalse(results[2].autoAccepted)
    }

    @Test
    fun `D duplicate songs are detected across remaster and featuring variations`() {
        val tracks = listOf(
            SourceTrack(id = "1", position = 1, rawTitle = "Midnight City", rawArtist = "M83"),
            SourceTrack(id = "2", position = 2, rawTitle = "Midnight City (Remastered 2020)", rawArtist = "M83"),
            SourceTrack(id = "3", position = 3, rawTitle = "Redbone", rawArtist = "Childish Gambino")
        )

        val results = TrackMatchingEngine.matchTracks(tracks, MusicPlatform.DEEZER)
        assertFalse(results[0].isDuplicate)
        assertTrue(results[1].isDuplicate)
        assertEquals(1, results[1].sourceTrack.duplicateOfPosition)
        assertFalse(results[2].isDuplicate)
    }

    @Test
    fun `E songs with no available match are classified NO_MATCH and never dropped`() {
        val source = listOf(
            SourceTrack(id = "s1", position = 1, rawTitle = "Completely Obscure Demo", rawArtist = "Local Garage Band"),
            SourceTrack(id = "s2", position = 2, rawTitle = "Track 09", rawArtist = "")
        )
        val catalog = listOf(
            SourceTrack(id = "c1", position = 1, rawTitle = "Midnight City", rawArtist = "M83")
        )

        val results = TrackMatchingEngine.matchTracks(source, MusicPlatform.YOUTUBE_MUSIC, catalog)
        assertEquals(2, results.size)
        assertEquals(MatchStatus.NO_MATCH, results[0].status)
        assertEquals(MatchStatus.NO_MATCH, results[1].status)
        assertFalse(results[0].autoAccepted)
        assertFalse(results[1].autoAccepted)
    }

    @Test
    fun `H and I export confirmed matches and unmatched songs separately into RFC-4180 CSV`() {
        val source = listOf(
            SourceTrack(id = "s1", position = 1, rawTitle = "Redbone", rawArtist = "Childish Gambino", isrc = "USYAH1600100"),
            SourceTrack(id = "s2", position = 2, rawTitle = "Unfound Song", rawArtist = "Mystery Band")
        )
        val catalog = listOf(
            SourceTrack(id = "c1", position = 1, rawTitle = "Redbone", rawArtist = "Childish Gambino", isrc = "USYAH1600100")
        )

        val results = TrackMatchingEngine.matchTracks(source, MusicPlatform.SPOTIFY, catalog)
        val confirmed = results.filter { it.status == MatchStatus.HIGH_CONFIDENCE }
        val unmatched = results.filter { it.status == MatchStatus.NO_MATCH }

        val adapter = PlatformAdapterRegistry.get(MusicPlatform.SPOTIFY)
        val confirmedCsv = adapter.exportCsv("My Playlist", confirmed)
        val unmatchedCsv = adapter.exportUnmatchedCsv("My Playlist", unmatched)

        assertEquals(1, confirmedCsv.trackCount)
        assertTrue(confirmedCsv.content.contains("Redbone"))
        assertFalse(confirmedCsv.content.contains("Unfound Song"))

        assertEquals(1, unmatchedCsv.trackCount)
        assertTrue(unmatchedCsv.content.contains("Unfound Song"))
        assertFalse(unmatchedCsv.content.contains("Redbone"))
    }

    @Test
    fun `K summary snapshot totals remain consistent`() {
        val snapshot = MigrationSummarySnapshot(
            totalOriginalTracks = 10,
            selectedTracksCount = 8,
            confirmedMatches = 4,
            needsReviewMatches = 2,
            unmatchedTracks = 1,
            skippedTracks = 1,
            duplicateTracks = 2
        )
        assertEquals(8, snapshot.totalEvaluated)
        assertEquals(50, snapshot.exportReadinessPercent)
        assertFalse(snapshot.isReadyForCleanExport)
        assertTrue(snapshot.readinessLabel.contains("Review required"))
    }

    @Test
    fun `csv codec handles quoted commas and binary safety`() {
        val raw = "\"Track Name\",\"Artist Name(s)\"\n\"Hello, World\",\"AC/DC & \"\"Friends\"\"\""
        val rows = CsvCodec.parseRows(raw)
        assertEquals(2, rows.size)
        assertEquals("Hello, World", rows[1][0])
        assertEquals("AC/DC & \"Friends\"", rows[1][1])

        val adapter = PlatformAdapterRegistry.get(MusicPlatform.UNIVERSAL_CSV)
        assertNotNull(adapter.parseCsv("\u0000\u0001\u0002\u0003\u0004\u0005", "Bad").errorMessage)
        assertNull(TextNormalizer.analyzeTitle("Café del Mar").let { null })
    }
}
