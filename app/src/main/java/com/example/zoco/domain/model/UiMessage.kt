package com.example.zoco.domain.model

import androidx.annotation.StringRes

/**
 * Un mensaje para mostrar en pantalla SIN escribir el texto en el código.
 * Guarda el número del texto (`R.string.algo`) y sus valores, y la pantalla lo traduce al idioma
 * elegido. Así los mensajes cambian con el selector de idioma y todos los textos viven en strings.xml.
 *
 * Uso: `UiMessage.of(R.string.msg_product_deleted, product.name)`.
 * Si el texto ya viene escrito (por ejemplo, un error que explica el servidor) se usa [UiMessage.raw].
 */
class UiMessage private constructor(
    @StringRes val resId: Int?,
    val args: List<Any>,
    val rawText: String?
) {
    companion object {
        /** Mensaje tomado de strings.xml. Los valores pueden ser textos, números u otro [UiMessage]. */
        fun of(@StringRes resId: Int, vararg args: Any) = UiMessage(resId, args.toList(), null)

        /** Texto ya escrito, que no se traduce. */
        fun raw(text: String) = UiMessage(null, emptyList(), text)
    }
}

/**
 * Error con un mensaje listo para mostrar a la persona. Los repositorios lo usan en `Result.failure`
 * para que los ViewModels puedan reenviar el mensaje sin escribir texto.
 */
class UserFacingException(val uiMessage: UiMessage) : Exception(uiMessage.rawText ?: "")

/** El mensaje del error si trae uno para la persona; si no, [fallback]. */
fun Throwable?.toUiMessage(fallback: UiMessage): UiMessage =
    (this as? UserFacingException)?.uiMessage ?: fallback
