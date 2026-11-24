# PLAN DE TRABAJO Y TAREAS - ENTREGABLE FINAL (100%)

**Proyecto:** Zoco - Plataforma Digital Abierta para el Emprendimiento Tradicional y Local  
**Objetivo:** Completar el 100% de los criterios funcionales, técnicos y de arquitectura exigidos para el proyecto final.  
**Estado:** Planificado - En espera de confirmación ("continua") para ejecutar con el equipo de subagentes.

---

## Equipo de Subagentes Asignado (Modelo: Gemini 3.8 Flash High)

* **`coordinator`**: Orquestador principal de la integración y revisión final.
* **`monitor`**: Monitor de bloqueos y tiempos de ejecución del enjambre.
* **`ui-adaptive`**: Especialista en Jetpack Compose y adaptabilidad en pantallas pequeñas y grandes ($\ge 600\text{dp}$).
* **`i18n-engine`**: Especialista en internacionalización, cambio de idioma dinámico (ES/EN) y extracción de strings XML.
* **`media-notif`**: Especialista en persistencia multimedia con Room (`MediaDao`) y notificaciones locales.
* **`verifier`**: Verificador de compilación, retrocompatibilidad con la línea base y pruebas de compilación.

---

## Tareas Específicas por Desarrollar

### 📋 TASK-FINAL-01: Creación de `ProfileViewModel` y desacoplamiento de UI [COMPLETADA]
* **Responsable:** `ui-adaptive`
* **Estado:** ✅ Completada y verificada
* **Objetivo:** Cumplir con el 100% de UIs gobernadas por ViewModel (`Compose + ViewModel`).
* **Acciones:**
  1. Crear `ProfileViewModel.kt` en `ui/profile` para gestionar:
     * Estado de conexión a la API Django (`apiUrl`, `isOnline`, `isCheckingConnection`, `connectionStatus`).
     * Idioma seleccionado (`selectedLanguage`).
     * Métodos para probar conectividad (`testConnection()`) y cambiar de URL base (`setApiUrl()`).
  2. Integrar `ProfileViewModel` en `ViewModelFactory.kt` y `ZocoNavHost.kt`.
  3. Refactorizar `ProfileScreen.kt` para consumir `ProfileUiState` mediante `collectAsStateWithLifecycle()`.

---

### 🌐 TASK-FINAL-02: Extracción y cobertura del 100% de cadenas para i18n [COMPLETADA]
* **Responsable:** `i18n-engine`
* **Estado:** ✅ Completada y verificada
* **Objetivo:** Eliminar cadenas de texto hardcodeadas en las pantallas para que la internacionalización sea total.
* **Acciones:**
  1. Identificar textos quemados en español en `ProfileScreen.kt` (títulos de tarjetas, botones de prueba de conexión, labels de IP, nombres de integrantes).
  2. Identificar textos en `CartScreen.kt` ("Tus pedidos recientes", labels de checkout) y `CatalogScreen.kt` (diálogos, badges de procedencia y confirmaciones).
  3. Declarar todas las cadenas faltantes en `res/values/strings.xml` (Español).
  4. Declarar las traducciones exactas en `res/values-en/strings.xml` (Inglés).
  5. Refactorizar `ProfileScreen.kt`, `CartScreen.kt`, `CatalogScreen.kt` y `SellerScreen.kt` para usar `stringResource(...)`.

---

### 🔄 TASK-FINAL-03: Implementación del selector dinámico de idioma en tiempo real [COMPLETADA]
* **Responsable:** `i18n-engine`
* **Estado:** ✅ Completada y verificada
* **Objetivo:** Permitir que el usuario cambie el idioma de la app entre Español e Inglés desde el Perfil de inmediato.
* **Acciones:**
  1. Implementar la actualización del `Locale` de la aplicación mediante `CompositionLocalProvider(LocalConfiguration, LocalContext)` y `LocaleManager` (API 33+).
  2. Persistir la preferencia de idioma en `SharedPreferences` ("zoco_prefs") a través de `ProfileViewModel` y `ViewModelFactory`.
  3. Vincular los `RadioButton` de `ProfileScreen` para ejecutar el cambio inmediato en toda la interfaz sin recargar la aplicación.
  4. Agregar pruebas unitarias en `ProfileViewModelTest.kt`.

---

### 📱 TASK-FINAL-04: Adaptabilidad UX en pantallas grandes ($\ge 600\text{dp}$) para Carrito y Vendedor [COMPLETADA]
* **Responsable:** `ui-adaptive`
* **Estado:** ✅ Completada y verificada
* **Objetivo:** Brindar una experiencia de usuario (UX) rica y diferenciada en tablets, pantallas plegables o emuladores anchos.
* **Acciones:**
  1. **En `CartScreen`:**
     * Móvil ($< 600\text{dp}$): vista vertical en 1 columna optimizada.
     * Pantalla grande ($\ge 600\text{dp}$): vista dividida a 2 columnas usando `Row` + `BoxWithConstraints` (Columna izquierda: lista de productos del carrito y pedidos recientes; Columna derecha: formulario de entrega `DeliveryInfoCard` y totalizador `OrderTotalCard`).
  2. **En `SellerScreen`:**
     * Móvil ($< 600\text{dp}$): formulario vertical estándar.
     * Pantalla grande ($\ge 600\text{dp}$): diseño en dos paneles (Panel izquierdo: formulario de captura de producto; Panel derecho: vista previa interactiva en tiempo real con `ProductItemCard` reutilizando el componente visual del catálogo).

---

### 📷 TASK-FINAL-05: Integración de foto/multimedia en alta de productos
* **Responsable:** `media-notif`
* **Objetivo:** Cerrar el ciclo funcional del módulo Multimedia (`MediaEntity` / `MediaDao`).
* **Acciones:**
  1. Añadir botón/selector de imagen en `SellerScreen` (usando `rememberLauncherForActivityResult` con `ActivityResultContracts.GetContent` o selector de catálogo de fotos/íconos).
  2. Guardar la URI de la imagen seleccionada en la tabla `media` a través de `MediaDao`.
  3. Renderizar la imagen con Coil (`AsyncImage`) o thumbnail en `ProductItemCard` si existe un registro multimedia vinculado al producto.

---

### 🧪 TASK-FINAL-06: Verificación de compilación y pruebas de regresión
* **Responsable:** `verifier`
* **Objetivo:** Comprobar que todas las funcionalidades anteriores sigan operativas al 100%.
* **Acciones:**
  1. Validar que no existan errores de compilación ni advertencias críticas de Kotlin.
  2. Verificar que la base de datos Room v4 mantenga la integridad referencial.
  3. Comprobar que la navegación `NavHost` mantenga el backstack y estados intactos.
  4. Generar el reporte de validación final para entrega.
