package com.example.zoco.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileViewModelTest {

    @Test
    fun defaultLanguage_isCorrect() {
        val viewModel = ProfileViewModel()
        val language = viewModel.uiState.value.selectedLanguage
        assert(language == "es" || language == "en")
    }

    @Test
    fun setLanguage_updatesStateToEnglish() {
        val viewModel = ProfileViewModel()
        viewModel.setLanguage("en")
        assertEquals("en", viewModel.uiState.value.selectedLanguage)
    }

    @Test
    fun setLanguage_updatesStateToSpanish() {
        val viewModel = ProfileViewModel()
        viewModel.setLanguage("en")
        assertEquals("en", viewModel.uiState.value.selectedLanguage)
        viewModel.setLanguage("es")
        assertEquals("es", viewModel.uiState.value.selectedLanguage)
    }

    @Test
    fun updateApiUrl_updatesState() {
        val viewModel = ProfileViewModel()
        val testUrl = "http://192.168.1.100:8000/api/"
        viewModel.updateApiUrl(testUrl)
        assertEquals(testUrl, viewModel.uiState.value.apiUrl)
    }
}
