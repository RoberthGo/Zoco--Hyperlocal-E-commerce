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
            ).first { it.session.username == request.username }
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

        override suspend fun register(request: RegisterRequestDto): AuthResponseDto {
            if (offline) throw IOException("offline")
            return AuthResponseDto(
                token = "token_reg",
                user = UserDto(
                    id = 99L,
                    username = request.username,
                    email = request.email,
                    firstName = request.firstName,
                    role = request.role
                )
            )
        }

        override suspend fun logout(): Response<Unit> = Response.success(Unit)

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
    fun quickSwitch_and_becomeSeller_workOnlineAndOffline() = runTest {
        val sessionManager = SessionManager(prefs = null)
        val fakeApi = FakeAuthApiService(offline = false)
        val repo = AuthRepositoryImpl(sessionManager, fakeApi)

        // Inicia como BUYER
        assertEquals(UserRole.BUYER, repo.sessionFlow.value.role)
        assertEquals("laura_compradora", repo.sessionFlow.value.username)

        // Convertirse en Vendedor con nombre de negocio
        val upgradeResult = repo.becomeSeller("Dulcería Laura")
        assertTrue(upgradeResult.isSuccess)
        assertEquals(UserRole.SELLER, repo.sessionFlow.value.role)
        assertEquals("Dulcería Laura", repo.sessionFlow.value.producerName)

        // Cambio rápido a Admin (offline fallback también funciona)
        fakeApi.offline = true
        val adminSwitch = repo.quickSwitch(UserRole.ADMIN)
        assertTrue(adminSwitch.isSuccess)
        assertEquals(UserRole.ADMIN, repo.sessionFlow.value.role)
        assertEquals("admin", repo.sessionFlow.value.username)

        // Admin cambia rol de usuario localmente aunque esté offline
        assertTrue(repo.updateUserRole(PredefinedUsers.BUYER.session.userId, UserRole.SELLER))
        val updatedBuyer = repo.usersFlow.value.first { it.username == "laura_compradora" }
        assertEquals(UserRole.SELLER, updatedBuyer.role)
    }
}
