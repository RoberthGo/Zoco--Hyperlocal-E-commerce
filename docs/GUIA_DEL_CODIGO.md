# Guía del código de la app Zoco

Esta guía es para entender la app sin ser experto en Android. Léela de arriba abajo una vez y luego úsala como mapa.

## 1. Qué hace la app
Zoco es un mercado local. Hay tres tipos de cuenta:

| Rol | Qué puede hacer | Pestañas que ve |
|---|---|---|
| Comprador | Ver productos, comprar, ver sus pedidos | Catálogo, Carrito, Perfil |
| Vendedor | Lo anterior + publicar productos y gestionar los pedidos que recibe | + Vender |
| Administrador | Ver todo y moderar (roles, pedidos, productos) | + Admin |

- Al abrir la app hay que **iniciar sesión o registrarse**. El registro solo crea compradores.
- Un comprador puede convertirse en vendedor desde **Perfil → Convertirme en vendedor**.
- Los administradores no se registran: los crea el dueño del servidor.

## 2. Mapa de carpetas
Todo el código está en `app/src/main/java/com/example/zoco/`:

```
ZocoApplication.kt   Se crea al abrir la app; guarda las piezas compartidas
MainActivity.kt      Pantalla raíz: decide login / barra lateral / barra inferior

ui/                  LO QUE SE VE (pantallas). Una carpeta por pantalla:
  auth/              Login y registro
  catalog/           Catálogo de productos
  cart/              Carrito y pedidos
  seller/            Publicar productos (vendedores)
  admin/             Panel del administrador
  profile/           Perfil, foto, idioma, cerrar sesión
  navigation/        Pestañas y rutas (Screen.kt, ZocoNavHost.kt)
  common/            Piezas compartidas (formato de precios, fábrica de ViewModels...)
  theme/             Colores y tipografía

domain/              LAS "COSAS" DE LA APP, sin detalles técnicos
  model/             Product, Order, UserSession, UserRole...
  repository/        Interfaces: qué se puede pedir (ProductRepository, AuthRepository...)

data/                DE DÓNDE SALEN LOS DATOS
  local/             Base de datos del teléfono (Room), sesión guardada, copias de fotos
  remote/            Comunicación con el servidor (Retrofit), búsqueda automática del servidor
  repository/        Implementaciones de las interfaces de domain/
  sync/              Subir en segundo plano lo que quedó pendiente sin internet
```

## 3. Cómo viaja la información (el patrón de TODAS las pantallas)

```
Pantalla (XxxScreen.kt)  ->  ViewModel (XxxViewModel.kt)  ->  Repositorio  ->  Room (teléfono) / Servidor
   solo dibuja                 guarda el "estado"              decide de dónde
   y avisa clics               y reacciona a clics             salen los datos
```

1. La **pantalla** no tiene lógica. Recibe un `uiState` (todo lo que debe mostrar) y funciones `onAlgo` (lo que pasa cuando el usuario toca algo).
2. El **ViewModel** guarda el `uiState`. Cuando cambia, la pantalla se redibuja sola.
3. El **repositorio** decide si el dato viene de la base local o del servidor.

Ejemplo: tocar "Cerrar sesión" en `ProfileScreen` llama a `onLogout` → `ProfileViewModel.logout()` → `AuthRepositoryImpl.logout()` → avisa al servidor y `SessionManager.clearSession()` borra la sesión → `MainActivity` ve que ya no hay sesión y muestra el login.

## 4. Las ideas que conviene entender

### Sesión y roles
- `SessionManager` guarda la sesión (usuario, rol, token) en el teléfono. Sin token = sin sesión = pantalla de login.
- El **token** se agrega solo a cada petición al servidor (`RetrofitClient.authInterceptor`). Si el servidor responde 401 (sesión vencida), la app vuelve al login.
- `Screen.visibleFor(rol)` decide qué pestañas se ven. **Ocultar una pestaña no es seguridad**: el servidor es quien realmente impide lo que cada rol no puede hacer.

### Funciona sin internet ("primero local")
- Lo que ves siempre sale de la base local (Room), por eso carga al instante.
- Si creas o editas algo sin internet, queda marcado como **pendiente** y `SyncPendingWorker` lo sube cuando vuelve la conexión.
- Eliminar sí exige conexión (para no dejar el servidor y el teléfono contradiciéndose).

### Encontrar el servidor solo
- `ServerDiscovery` prueba varias direcciones a la vez (emulador `10.0.2.2`, USB `127.0.0.1`, la puerta de enlace del Wi-Fi, la última que funcionó y, como último recurso, toda la red local) y se queda con la que responda.
- Nadie tiene que escribir una IP. Se ejecuta al abrir la app y antes de cada login.

### Fotos
- Al elegir una foto, `ImageStore` guarda una copia reducida dentro de la app.
- Los productos las suben con `ProductRepositoryImpl.uploadPendingImage` y el perfil con `AuthRepositoryImpl.updateAvatar`.
- Después, la ruta del teléfono se cambia por la URL del servidor para que otros usuarios la vean.

### Idioma
- Se elige en Perfil. `LanguageProvider` (en `MainActivity.kt`) hace que toda la app cambie al instante.
- Los textos están en `app/src/main/res/values/strings.xml` (español) y `values-en/strings.xml` (inglés).
  Nota: algunos mensajes de los ViewModels aún están escritos directamente en español.

## 5. "Quiero cambiar X, ¿dónde lo toco?"

| Quiero... | Archivo |
|---|---|
| Cambiar un texto de la app | `res/values/strings.xml` (y `values-en/`) |
| Cambiar qué pestañas ve cada rol | `ui/navigation/Screen.kt` → `visibleFor` |
| Cambiar la pantalla de login/registro | `ui/auth/AuthScreen.kt` |
| Cambiar las reglas del registro (longitud de contraseña, etc.) | `ui/auth/AuthScreen.kt` (validación) y en el servidor `accounts/serializers.py` |
| Cambiar el diseño del catálogo | `ui/catalog/CatalogScreen.kt` |
| Cambiar el perfil (foto, idioma...) | `ui/profile/ProfileScreen.kt` (cada tarjeta es una función) |
| Cambiar cómo se muestran los precios | `ui/common/Formatting.kt` |
| Cambiar los productos de ejemplo | `data/local/SampleData.kt` |
| Cambiar qué se guarda en la base del teléfono | `data/local/*Entity.kt` y `ZocoDatabase.kt` (hay que subir la versión) |
| Agregar una llamada nueva al servidor | `data/remote/api/*ApiService.kt` + su DTO en `data/remote/dto/` |
| Cambiar colores | `ui/theme/Color.kt` |

## 6. Cómo ejecutar el proyecto
1. Backend: `cd Zoco-API` → `.\.venv\Scripts\python.exe manage.py runserver 0.0.0.0:8000`.
2. Abrir esta carpeta en Android Studio y ejecutar en un emulador o teléfono.
3. Abrir el puerto 8000 en el firewall de Windows (una sola vez) si se usa un teléfono real por Wi-Fi.
4. Pruebas: `./gradlew testDebugUnitTest`.

## 7. Vocabulario rápido
- **Composable**: función que dibuja un trozo de pantalla (`@Composable fun ...`).
- **ViewModel**: guarda el estado de una pantalla y sobrevive a rotaciones.
- **StateFlow**: valor que avisa a quien lo observa cuando cambia.
- **Room**: base de datos SQLite dentro del teléfono.
- **Retrofit**: librería para llamar al servidor.
- **DTO**: forma en que viajan los datos entre la app y el servidor (`data/remote/dto/`).
- **Entity**: forma en que se guardan los datos en Room (`data/local/*Entity.kt`).
