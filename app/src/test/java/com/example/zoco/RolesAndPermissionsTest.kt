package com.example.zoco

import com.example.zoco.data.local.SessionManager
import com.example.zoco.data.remote.api.AuthApiService
import com.example.zoco.data.remote.dto.AuthResponseDto
import com.example.zoco.data.remote.dto.BecomeSellerRequestDto
import com.example.zoco.data.remote.dto.BecomeSellerResponseDto
import com.example.zoco.data.remote.dto.LoginRequestDto
import com.example.zoco.data.remote.dto.RegisterRequestDto
import com.example.zoco.data.remote.dto.RoleUpdateRequestDto
import com.example.zoco.data.remote.dto.UserDto
import com.example.zoco.data.repository.AuthRepositoryImpl
import com.example.zoco.domain.model.PredefinedUsers
import com.example.zoco.domain.model.Product
import com.example.zoco.domain.model.UserRole
import com.example.zoco.ui.navigation.Screen
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class RolesAndPermissionsTest {

    private class FakeAuthApiService(var offline: Boolean = false) : AuthApiService {
        override suspend fun login(request: LoginRequestDto): AuthResponseDto {
            if (offline) throw IOException("offline")
            val preset = listOf(
                PredefinedUsers.BUYER,
                PredefinedUsers.SELLER,
                PredefinedUsers.ADMIN
            ).firstOrNull { it.session.username == request.username && it.password == request.password }
                ?: throw HttpException(
                    Response.error<Any>(
                        400,
                        "{\"detail\":[\"Credenciales inválidas o usuario inactivo.\"]}"
                            .toResponseBody("application/json".toMediaType())
                    )
                )
            return AuthResponseDto(
                token = "token_${preset.session.username}",
                user = UserDto(
                    id = preset.session.userId,
                    username = preset.session.username,
                    email = preset.session.email,
                    firstName = preset.session.fullName,
                    role = preset.session.role.name,
                    phone = preset.session.phone,
                    address = preset.session.address
                ),
                message = "ok"
            )
        }

        var lastRegisterRole: String? = null

        override suspend fun register(request: RegisterRequestDto): AuthResponseDto {
            if (offline) throw IOException("offline")
            lastRegisterRole = request.role
            return AuthResponseDto(
                token = "token_reg",
                user = UserDto(
                    id = 99L,
                    username = request.username,
                    email = request.email,
                    firstName = request.firstName,
                    role = "BUYER"
                )
            )
        }

        override suspend fun logout(): Response<Unit> = Response.success(Unit)
        override suspend fun uploadAvatar(image: okhttp3.MultipartBody.Part): com.example.zoco.data.remote.dto.AvatarResponseDto = error("no usado")

        override suspend fun becomeSeller(request: BecomeSellerRequestDto): BecomeSellerResponseDto {
            if (offline) throw IOException("offline")
            return BecomeSellerResponseDto(
                user = UserDto(
                    id = PredefinedUsers.BUYER.session.userId,
                    username = PredefinedUsers.BUYER.session.username,
                    email = PredefinedUsers.BUYER.session.email,
                    firstName = PredefinedUsers.BUYER.session.fullName,
                    role = "SELLER"
                ),
                message = "Ahora eres vendedor"
            )
        }

        override suspend fun getUsers(): List<UserDto> {
            if (offline) throw IOException("offline")
            return PredefinedUsers.ALL_MANAGED_USERS.map {
                UserDto(
                    id = it.id,
                    username = it.username,
                    email = it.email,
                    firstName = it.fullName,
                    role = it.role.name
                )
            }
        }

        override suspend fun updateUserRole(userId: Long, request: RoleUpdateRequestDto): UserDto {
            if (offline) throw IOException("offline")
            return UserDto(id = userId, username = "user_$userId", role = request.role)
        }
    }

    @Test
    fun visibleScreens_matchEachRoleExactly() {
        assertEquals(
            listOf(Screen.CATALOG, Screen.CART, Screen.PROFILE),
            Screen.visibleFor(UserRole.BUYER)
        )
        assertEquals(
            listOf(Screen.CATALOG, Screen.CART, Screen.SELLER, Screen.PROFILE),
            Screen.visibleFor(UserRole.SELLER)
        )
        assertEquals(
            listOf(Screen.CATALOG, Screen.CART, Screen.SELLER, Screen.ADMIN, Screen.PROFILE),
            Screen.visibleFor(UserRole.ADMIN)
        )
    }

    @Test
    fun sellerCanDeleteOnlyOwnProducts_adminCanDeleteAny_buyerCannotDelete() {
        val juanProduct = Product(
            id = 1L,
            name = "Pan de Juan",
            price = 40.0,
            producerName = "Panadería Don Juan",
            sellerId = 2L,
            sellerUsername = "vendedor_juan"
        )
        val mariaProduct = Product(
            id = 2L,
            name = "Canasta de María",
            price = 120.0,
            producerName = "Artesanías La Esperanza",
            sellerId = 4L,
            sellerUsername = "artesana_maria"
        )

        val buyer = PredefinedUsers.BUYER.session
        val juan = PredefinedUsers.SELLER.session
        val admin = PredefinedUsers.ADMIN.session

        // BUYER no puede borrar ninguno
        assertFalse(juanProduct.canBeDeletedBy(buyer))
        assertFalse(mariaProduct.canBeDeletedBy(buyer))

        // SELLER solo puede borrar sus propios productos
        assertTrue(juanProduct.canBeDeletedBy(juan))
        assertFalse(mariaProduct.canBeDeletedBy(juan))

        // ADMIN puede borrar cualquier producto por moderación
        assertTrue(juanProduct.canBeDeletedBy(admin))
        assertTrue(mariaProduct.canBeDeletedBy(admin))
    }

    @Test
    fun startsLoggedOut_untilLoginSucceeds() = runTest {
        val repo = AuthRepositoryImpl(SessionManager(prefs = null), FakeAuthApiService())

        assertFalse(repo.sessionFlow.value.isLoggedIn)

        val bad = repo.login("laura_compradora", "mala")
        assertTrue(bad.isFailure)
        assertEquals("Credenciales inválidas o usuario inactivo.", bad.exceptionOrNull()?.message)
        assertFalse(repo.sessionFlow.value.isLoggedIn)

        assertTrue(repo.login("laura_compradora", "zoco123").isSuccess)
        assertTrue(repo.sessionFlow.value.isLoggedIn)
        assertEquals(UserRole.BUYER, repo.sessionFlow.value.role)
    }

    @Test
    fun register_alwaysCreatesBuyer_andNeverSendsAdminRole() = runTest {
        val api = FakeAuthApiService()
        val repo = AuthRepositoryImpl(SessionManager(prefs = null), api)

        val result = repo.register("nuevo", "nuevo@mail.com", "secreto1", "Nuevo Usuario")

        assertTrue(result.isSuccess)
        assertEquals("BUYER", api.lastRegisterRole)
        assertEquals(UserRole.BUYER, repo.sessionFlow.value.role)
    }

    @Test
    fun register_offline_failsWithoutCreatingLocalAccount() = runTest {
        val repo = AuthRepositoryImpl(SessionManager(prefs = null), FakeAuthApiService(offline = true))

        val result = repo.register("nuevo", "nuevo@mail.com", "secreto1", "Nuevo Usuario")

        assertTrue(result.isFailure)
        assertFalse(repo.sessionFlow.value.isLoggedIn)
    }

    @Test
    fun buyerBecomesSeller_onlyThroughApi() = runTest {
        val api = FakeAuthApiService()
        val repo = AuthRepositoryImpl(SessionManager(prefs = null), api)

        // Sin sesión no se puede
        assertTrue(repo.becomeSeller("Dulcería Laura").isFailure)

        repo.login("laura_compradora", "zoco123")
        assertTrue(repo.becomeSeller("Dulcería Laura").isSuccess)
        assertEquals(UserRole.SELLER, repo.sessionFlow.value.role)
        assertEquals("Dulcería Laura", repo.sessionFlow.value.producerName)

        // Un vendedor ya no puede volver a "convertirse"
        assertTrue(repo.becomeSeller("Otro").isFailure)
    }

    @Test
    fun becomeSeller_offline_doesNotChangeRoleLocally() = runTest {
        val api = FakeAuthApiService()
        val repo = AuthRepositoryImpl(SessionManager(prefs = null), api)
        repo.login("laura_compradora", "zoco123")

        api.offline = true
        val result = repo.becomeSeller("Dulcería Laura")

        assertTrue(result.isFailure)
        assertEquals(UserRole.BUYER, repo.sessionFlow.value.role)
    }

    @Test
    fun adminOnlyByLogin_andRoleChangeNeedsApi() = runTest {
        val api = FakeAuthApiService()
        val repo = AuthRepositoryImpl(SessionManager(prefs = null), api)

        assertTrue(repo.login("admin", "admin123").isSuccess)
        assertEquals(UserRole.ADMIN, repo.sessionFlow.value.role)
        assertTrue(repo.usersFlow.value.isNotEmpty())

        api.offline = true
        assertFalse(repo.updateUserRole(PredefinedUsers.BUYER.session.userId, UserRole.SELLER))
        val buyer = repo.usersFlow.value.first { it.username == "laura_compradora" }
        assertEquals(UserRole.BUYER, buyer.role)
    }

    @Test
    fun logout_clearsSession() = runTest {
        val sessionManager = SessionManager(prefs = null)
        val repo = AuthRepositoryImpl(sessionManager, FakeAuthApiService())
        repo.login("admin", "admin123")

        repo.logout()

        assertFalse(repo.sessionFlow.value.isLoggedIn)
        assertEquals(null, SessionManager.activeToken)
        assertTrue(repo.usersFlow.value.isEmpty())
    }
}
