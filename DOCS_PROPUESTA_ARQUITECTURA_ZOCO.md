# ANTEPROYECTO Y ARQUITECTURA TÉCNICA DE SOFTWARE
## Plataforma Zoco (Mercado Barrio) - Comercio Local Hiperlocal

---

### 1. Identificación y Justificación de la Problemática

#### 1.1 Contexto y Problemática
En las comunidades locales, los pequeños productores, artesanos y negocios de barrio enfrentan serias barreras para competir con grandes cadenas y plataformas masivas de comercio electrónico. Las plataformas existentes imponen altas comisiones por intermediación, exigen logística estandarizada compleja y no contemplan las particularidades del comercio comunitario (entregas locales inmediatas, horarios flexibles acordados y cercanía vecinal).

Por otro lado, los consumidores locales carecen de un canal centralizado y accesible donde consultar la oferta disponible en su entorno inmediato, conocer a los productores de su comunidad y coordinar entregas directas.

#### 1.2 Propuesta de Solución
**Zoco** es una aplicación móvil nativa desarrollada con el SDK oficial de Android y la suite moderna de Jetpack. Permite:
- Conectar a productores y artesanos comunitarios directamente con consumidores locales sin intermediarios abusivos.
- Fomentar el comercio justo y la economía circular de proximidad.
- Proporcionar una experiencia fluida con soporte para operación sin conexión (offline-first) y sincronización con servicios web.

---

### 2. Requerimientos del Sistema

#### 2.1 Requerimientos Funcionales (RF)
- **RF-01: Exploración y Catálogo de Productos Locales:** El usuario debe poder visualizar el catálogo de productos disponibles en el barrio, ordenados de forma clara e interactiva.
- **RF-02: Búsqueda y Filtrado Multicriterio:** El usuario debe poder buscar productos por texto (`TextField`), filtrar por categorías mediante selección exclusiva (`RadioButton`) y filtrar por entrega rápida disponible (`Checkbox`).
- **RF-03: Gestión de Pedidos y Carrito de Compras:** El usuario debe poder agregar y remover productos del carrito de compras, visualizando el cálculo dinámico del costo total a pagar.
- **RF-04: Programación de Horario de Entrega / Recogida:** El usuario debe poder programar el horario específico en el que desea recibir o retirar su pedido utilizando un selector de tiempo nativo (`TimePicker`).
- **RF-05: Registro de Nuevos Productos Locales:** Los vendedores locales deben disponer de un formulario interactivo con validación de campos obligatorios para registrar nuevos artículos en el catálogo local.
- **RF-06: Navegación Global y Menú Principal:** La aplicación debe proporcionar acceso unificado a las pantallas de Catálogo, Carrito, Vendedor y Perfil mediante una barra de navegación inferior en teléfonos y riel lateral en pantallas amplias.
- **RF-07: Internacionalización y Localización:** La aplicación debe soportar múltiples idiomas (Español e Inglés) a nivel de recursos de la plataforma (`res/values` y `res/values-en`).

#### 2.2 Requerimientos No Funcionales (RNF)
- **RNF-01: Arquitectura y Mantenibilidad:** La aplicación debe seguir los principios de Clean Architecture y Unidirectional Data Flow (UDF), separando las capas de Presentación (`ui`), Dominio (`domain`) y Datos (`data`).
- **RNF-02: Persistencia Offline-First:** Los datos deben persistir de manera local mediante SQLite con Room, permitiendo la consulta y operatividad sin conectividad activa a internet.
- **RNF-03: Diseño Adaptativo y Responsivo:** La interfaz de usuario debe adaptarse al factor de forma del dispositivo (teléfonos inteligentes compactos y tabletas/pantallas grandes con más de 600dp de ancho).
- **RNF-04: Rendimiento y Eficiencia de Renderizado:** La UI debe construirse íntegramente con Jetpack Compose y componentes Material 3, garantizando 60 fps y renderizado reactivo con `StateFlow`.
- **RNF-05: Integración con Servicios Web:** La arquitectura debe estar preparada para comunicar con APIs RESTful mediante Retrofit, OkHttp y serialización Moshi.

---

### 3. Propuesta Técnica y Arquitectura de la Plataforma

#### 3.1 Diagrama de Capas de la Solución (Clean Architecture)

```
+-------------------------------------------------------------+
|                      CAPA DE PRESENTACIÓN                   |
|  [Compose UI Screens] <---> [ViewModels (StateFlow / UDF)]  |
|  (Catalog, Cart, Seller, Profile, NavigationRail/Bar)       |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                       CAPA DE DOMINIO                       |
|   - Modelos Puros de Negocio: Product, CartItem, Category   |
|   - Contratos / Interfaces: ProductRepository               |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                        CAPA DE DATOS                        |
|   - Implementación: ProductRepositoryImpl                   |
|   - Local (Offline): Room Database (ProductDao, Entities)   |
|   - Remoto (Nube): Retrofit REST API / Web Services         |
+-------------------------------------------------------------+
```

#### 3.2 Batería Tecnológica de Jetpack y Librerías Utilizadas
1. **Jetpack Compose (Material 3):** Framework declarativo para construcción de UI reactiva con componentes oficiales:
   - `TextField`: Entradas de texto y búsqueda.
   - `RadioButton`: Selección única de categoría e idioma.
   - `Checkbox`: Filtros booleanos de despacho.
   - `TimePicker` & `TimePickerDialog`: Selección de hora de entrega.
   - `Button` & `IconButton`: Acciones directas y despacho de eventos.
   - `LazyColumn` & `LazyVerticalGrid`: Listas y cuadrículas adaptativas de alto rendimiento.
2. **Lifecycle & ViewModel Compose:** Retención de estado ante cambios de configuración (`uiState` inmutable expuesto vía `StateFlow`).
3. **Room Database (Jetpack Room):** Persistencia relacional local para garantizar capacidades offline-first.
4. **Navigation Adaptativo:** `NavigationBar` para dispositivos móviles verticales y `NavigationRail` con `BoxWithConstraints` para dispositivos de pantalla expandida (tablets o plegables).
5. **Retrofit 2 + OkHttp + Moshi:** Conectividad con servicios web y APIs REST externas.
6. **Android Resource Bundles:** Internacionalización nativa mediante archivos `strings.xml` por calificador de idioma.

---

### 4. Criterios de Aceptación Técnicos Verificados (Fase 1)

| Criterio Técnico | Estado | Implementación en Código |
| :--- | :---: | :--- |
| **Elementos interactivos funcionales** | Cumplido | Listas (`LazyColumn`/Grid), Botones (`Button`), Cajas de texto (`OutlinedTextField`), Radios (`RadioButton`), Checks (`Checkbox`), Timepicker (`TimePicker` nativo M3 con diálogo). |
| **Arquitectura con ViewModel** | Cumplido | `CatalogViewModel`, `CartViewModel` y `SellerViewModel` gestionando estados con `StateFlow` y eventos reactivos. |
| **Navegación completa** | Cumplido | Menú de navegación global con `Screen` enum que enlaza las cuatro pantallas del sistema. |
| **Soporte para diferentes pantallas** | Cumplido | Layout adaptativo que alterna entre `LazyColumn` + `NavigationBar` (móvil) y `LazyVerticalGrid` + `NavigationRail` (pantallas de ancho $\ge$ 600dp). |
| **Soporte para múltiples idiomas** | Cumplido | Archivos de recursos localizados: `res/values/strings.xml` (Español) y `res/values-en/strings.xml` (Inglés). |
| **Arquitectura de Repository** | Cumplido | Interfaz `ProductRepository` en dominio e implementación `ProductRepositoryImpl` en datos. |
| **Persistencia offline y servicios** | Cumplido | Base de datos `ZocoDatabase` (Room) con precarga inicial de productos locales y configuración para conexión REST con Retrofit. |
