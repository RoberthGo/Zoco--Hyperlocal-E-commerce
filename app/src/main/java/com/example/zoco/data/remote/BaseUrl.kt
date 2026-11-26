package com.example.zoco.data.remote

/** Asegura que la URL base termine en "/" (Retrofit lo exige). Ejemplo: "http://x:8000/api/v1" -> "http://x:8000/api/v1/". */
fun String.asBaseUrl(): String = if (endsWith("/")) this else "$this/"
