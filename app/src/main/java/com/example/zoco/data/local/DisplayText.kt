package com.example.zoco.data.local

private val TECHNICAL_TAGS = Regex("""\s*(\[Local Offline]|\[Local]|\(Room SQLite\))\s*""")

/** Quita etiquetas técnicas de los datos de ejemplo guardados en versiones anteriores de la app. */
internal fun String.cleanForDisplay(): String =
    replace(TECHNICAL_TAGS, " ").trim()
