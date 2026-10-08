package com.echo.feature.crossbar.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.echo.core.domain.achievement.Achievement
import com.echo.core.ui.design.panelDesignUnits
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// On the Konker the wall's detail card showed TIER / PLAYERS / STATUS with no values (Pokémon Unbound): a long
// description filled the card, which clips at its rounded edge, and pushed the values out. The values are
// what the card is for, so they keep their room and the description gives way.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1200dp-h752dp")
class AchievementDetailCardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a long description never pushes the tier, players and status values out of a short card`() {
        val achievement = Achievement(
            id = "1", name = "Champion of the Unbound Region, and Then Some More Words",
            description = "Defeat every gym leader, the elite four and the champion, then do it again on the hardest " +
                "difficulty without using items, without fainting, and without leaving the region at all. ".repeat(3),
            iconUrl = null, isHidden = false, isUnlocked = false, unlockedAt = null, globalPercent = 3.2, points = 25,
        )
        compose.setContent {
            val u = panelDesignUnits(1200f, 752f, LocalDensity.current)
            Box(Modifier.size(460.dp, 170.dp)) { DetailCard(achievement, u) }
        }
        compose.onNodeWithText("Locked").assertIsDisplayed()
        compose.onNodeWithText("3.2%", substring = true).assertIsDisplayed()
    }
}
