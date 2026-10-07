package com.echo.feature.settings.ui

import org.junit.Assert.assertEquals
import org.junit.Test

// a theme's README is markdown; its store page shows it as plain text, so the marks must not show
class ReadmeTextTest {
    @Test
    fun `heading and bold marks go, list dashes become bullets`() {
        assertEquals(
            "Intro\n\n• Icons: every icon\n• Sounds\nMore",
            readmeText("# Aurora\nIntro\n\n- **Icons**: every icon\n- Sounds\n## More"),
        )
    }
}
