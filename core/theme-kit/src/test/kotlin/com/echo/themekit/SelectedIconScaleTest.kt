package com.echo.themekit

import kotlin.test.Test
import kotlin.test.assertEquals

// a theme's categoryIconSelectedDp was read and never drawn. It now sizes the selected category icon against
// ECHO's own ratio, so the default layout draws exactly as it did
class SelectedIconScaleTest {
    @Test
    fun `ECHO's own layout draws the selected icon as before`() {
        assertEquals(1f, CrossbarLayoutSpec.DEFAULT.selectedIconScale(), 0.0001f)
    }

    @Test
    fun `a theme's larger selected icon is drawn larger, and a smaller one smaller`() {
        assertEquals(1.25f, CrossbarLayoutSpec(categoryIconSelectedDp = 90f, categoryIconDp = 56f).selectedIconScale(), 0.0001f)
        assertEquals(56f / 72f, CrossbarLayoutSpec(categoryIconSelectedDp = 56f, categoryIconDp = 56f).selectedIconScale(), 0.0001f)
    }

    @Test
    fun `a theme's text size is read against ECHO's, which is 1`() {
        assertEquals(1f, CrossbarLayoutSpec.DEFAULT.textScale(), 0.0001f)
        assertEquals(1.25f, CrossbarLayoutSpec(itemTextSp = 22.5f).textScale(), 0.0001f)
    }
}
