package com.example.zoco.ui.common

import androidx.compose.ui.tooling.preview.Preview

/**
 * Atajo para ver una pantalla en Android Studio en 4 tamaños a la vez (celular, celular horizontal,
 * plegable y tablet). Los tamaños de ventana reales y sus límites están en [WindowSize.kt].
 */
@Preview(
    name = "1. Teléfono Compacto (Vertical)",
    group = "Dispositivos",
    device = "spec:width=411dp,height=891dp,dpi=420",
    showBackground = true
)
@Preview(
    name = "2. Teléfono Horizontal (Landscape)",
    group = "Dispositivos",
    device = "spec:width=891dp,height=411dp,dpi=420",
    showBackground = true
)
@Preview(
    name = "3. Plegable / Tablet Pequeña (Medio)",
    group = "Dispositivos",
    device = "spec:width=673dp,height=841dp,dpi=420",
    showBackground = true
)
@Preview(
    name = "4. Tablet Grande (Expandido)",
    group = "Dispositivos",
    device = "spec:width=1280dp,height=800dp,dpi=240",
    showBackground = true
)
annotation class DeviceScreenSizePreviews
