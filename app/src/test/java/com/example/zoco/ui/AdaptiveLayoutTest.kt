package com.example.zoco.ui

import androidx.compose.ui.unit.dp
import com.example.zoco.ui.common.Breakpoints
import com.example.zoco.ui.common.WidthClass
import com.example.zoco.ui.common.WindowSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {

    @Test
    fun narrowPhone_isCompact() {
        assertEquals(WidthClass.COMPACT, Breakpoints.widthClassFor(360.dp))
        assertEquals(WidthClass.COMPACT, Breakpoints.widthClassFor(411.dp))
        assertEquals(WidthClass.COMPACT, Breakpoints.widthClassFor(599.dp))
    }

    @Test
    fun foldableAndSmallTablet_isMedium() {
        assertEquals(WidthClass.MEDIUM, Breakpoints.widthClassFor(600.dp))
        assertEquals(WidthClass.MEDIUM, Breakpoints.widthClassFor(720.dp))
        assertEquals(WidthClass.MEDIUM, Breakpoints.widthClassFor(839.dp))
    }

    @Test
    fun largeTabletAndDesktop_isExpanded() {
        assertEquals(WidthClass.EXPANDED, Breakpoints.widthClassFor(840.dp))
        assertEquals(WidthClass.EXPANDED, Breakpoints.widthClassFor(1024.dp))
        assertEquals(WidthClass.EXPANDED, Breakpoints.widthClassFor(1280.dp))
    }

    @Test
    fun twoColumns_areOnlyUsedWhenThereIsRoom() {
        assertFalse(WindowSize(WidthClass.COMPACT, isShort = false).hasRoomForTwoColumns)
        assertTrue(WindowSize(WidthClass.MEDIUM, isShort = false).hasRoomForTwoColumns)
        assertTrue(WindowSize(WidthClass.EXPANDED, isShort = false).hasRoomForTwoColumns)
    }

    @Test
    fun phoneInLandscape_isShortButExpanded() {
        val width = 891.dp
        val height = 411.dp
        val size = WindowSize(Breakpoints.widthClassFor(width), isShort = height < Breakpoints.SHORT_HEIGHT)
        assertTrue(size.isExpanded)
        assertTrue(size.isShort)
    }
}
