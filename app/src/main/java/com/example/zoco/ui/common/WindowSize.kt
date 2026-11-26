package com.example.zoco.ui.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cómo de grande es el espacio disponible para la app. Cada pantalla decide su diseño con esto:
 *  - [COMPACT]  menos de 600dp de ancho: celular en vertical.
 *  - [MEDIUM]   de 600dp a 839dp: plegable, tablet pequeña.
 *  - [EXPANDED] 840dp o más: tablet grande, celular en horizontal, ventana de escritorio.
 */
enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

/** Límites (en dp) entre un tamaño y otro. Es el único lugar donde viven estos números. */
object Breakpoints {
    val MEDIUM_MIN: Dp = 600.dp
    val EXPANDED_MIN: Dp = 840.dp

    /** Si el alto es menor a esto (celular en horizontal) se ahorra espacio vertical. */
    val SHORT_HEIGHT: Dp = 480.dp

    fun widthClassFor(width: Dp): WidthClass = when {
        width < MEDIUM_MIN -> WidthClass.COMPACT
        width < EXPANDED_MIN -> WidthClass.MEDIUM
        else -> WidthClass.EXPANDED
    }
}

/** Tamaño actual de la ventana. ZocoApp lo calcula una vez y cualquier pantalla puede leerlo con `.current`. */
data class WindowSize(val widthClass: WidthClass, val isShort: Boolean) {
    val isCompact: Boolean get() = widthClass == WidthClass.COMPACT
    val isExpanded: Boolean get() = widthClass == WidthClass.EXPANDED

    /** true si hay espacio para mostrar dos columnas lado a lado. */
    val hasRoomForTwoColumns: Boolean get() = widthClass != WidthClass.COMPACT
}

val LocalWindowSize = compositionLocalOf { WindowSize(WidthClass.COMPACT, isShort = false) }

/** Calcula el [WindowSize] del espacio disponible y lo pone a disposición de [content]. */
@Composable
fun ProvideWindowSize(content: @Composable () -> Unit) {
    BoxWithConstraints {
        val size = WindowSize(Breakpoints.widthClassFor(maxWidth), isShort = maxHeight < Breakpoints.SHORT_HEIGHT)
        CompositionLocalProvider(LocalWindowSize provides size, content = content)
    }
}
