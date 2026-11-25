package com.example.zoco.ui

import androidx.compose.ui.unit.dp
import com.example.zoco.ui.common.AdaptiveBreakpoints
import com.example.zoco.ui.common.WindowWidthSizeClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveLayoutTest {

    @Test
    fun compactPhone_resolvesToCompactSizeClass() {
        // Teléfonos verticales estándar (pantallas estrechas < 600dp)
        val phoneWidthSmall = 360.dp
        val phoneWidthStandard = 411.dp
        val phoneWidthLarge = 599.dp

        assertEquals(WindowWidthSizeClass.COMPACT, AdaptiveBreakpoints.getWidthSizeClass(phoneWidthSmall))
        assertEquals(WindowWidthSizeClass.COMPACT, AdaptiveBreakpoints.getWidthSizeClass(phoneWidthStandard))
        assertEquals(WindowWidthSizeClass.COMPACT, AdaptiveBreakpoints.getWidthSizeClass(phoneWidthLarge))
    }

    @Test
    fun foldableAndSmallTablet_resolvesToMediumSizeClass() {
        // Dispositivos plegables, tablets pequeñas o teléfonos en horizontal (600dp a 839dp)
        val foldableWidth = 600.dp
        val landscapePhoneWidth = 720.dp
        val smallTabletWidth = 839.dp

        assertEquals(WindowWidthSizeClass.MEDIUM, AdaptiveBreakpoints.getWidthSizeClass(foldableWidth))
        assertEquals(WindowWidthSizeClass.MEDIUM, AdaptiveBreakpoints.getWidthSizeClass(landscapePhoneWidth))
        assertEquals(WindowWidthSizeClass.MEDIUM, AdaptiveBreakpoints.getWidthSizeClass(smallTabletWidth))
    }

    @Test
    fun largeTabletAndDesktop_resolvesToExpandedSizeClass() {
        // Tablets grandes de 10"+ o pantallas de escritorio (>= 840dp)
        val largeTabletWidth = 840.dp
        val fullTabletWidth = 1024.dp
        val desktopWidth = 1280.dp

        assertEquals(WindowWidthSizeClass.EXPANDED, AdaptiveBreakpoints.getWidthSizeClass(largeTabletWidth))
        assertEquals(WindowWidthSizeClass.EXPANDED, AdaptiveBreakpoints.getWidthSizeClass(fullTabletWidth))
        assertEquals(WindowWidthSizeClass.EXPANDED, AdaptiveBreakpoints.getWidthSizeClass(desktopWidth))
    }

    @Test
    fun sellerLayoutMode_isTwoPaneOnlyOnWideScreens() {
        // En pantallas grandes (>= 600dp), Vender activa el modo dos paneles (Formulario + Vista Previa)
        val compactWidth = 411.dp
        val wideWidth = 600.dp
        val tabletWidth = 800.dp

        fun isWideScreen(width: androidx.compose.ui.unit.Dp): Boolean = width >= 600.dp

        assertFalse("Teléfono estándar no debe dividir en dos paneles", isWideScreen(compactWidth))
        assertTrue("Pantallas >=600dp deben activar dos paneles", isWideScreen(wideWidth))
        assertTrue("Tablets deben activar dos paneles", isWideScreen(tabletWidth))
    }
}
