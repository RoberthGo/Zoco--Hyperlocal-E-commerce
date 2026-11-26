package com.example.zoco.ui.common

/** Número con dos decimales, sin símbolo. Ejemplo: 45.5 -> "45.50". */
fun formatAmount(amount: Double): String = "%.2f".format(amount)

/** Precio con símbolo de peso. Ejemplo: 45.5 -> "$45.50". */
fun formatPrice(amount: Double): String = "$" + formatAmount(amount)
