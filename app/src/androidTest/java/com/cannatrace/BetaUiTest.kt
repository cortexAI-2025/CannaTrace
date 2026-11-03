package com.cannatrace

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class BetaUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun createLotAndReaderCannotWrite() {
        val name = "FICTIF UI ${UUID.randomUUID().toString().take(8)}"
        compose.onNodeWithText("Démonstration operateur").performClick()
        compose.onNodeWithText("Créer un lot fictif").performScrollTo().performClick()
        compose.onNodeWithText("Désignation").performScrollTo().performTextInput(name)
        compose.onNodeWithText("Catégorie").performScrollTo().performTextInput("Fictif")
        compose.onNodeWithText("Quantité initiale").performScrollTo().performTextReplacement("100")
        compose.onNodeWithText("Emplacement").performScrollTo().performTextInput("Zone UI")
        compose.onNodeWithText("Enregistrer le lot").performScrollTo().performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(name).assertIsDisplayed()
        compose.onNodeWithText("Quitter").performClick()
        compose.onNodeWithText("Démonstration lecteur").performClick()
        compose.onNodeWithText("Créer un lot fictif").assertDoesNotExist()
        compose.onNodeWithText("Ouvrir la fiche").performScrollTo().performClick()
        compose.onNodeWithText("Profil lecteur : lecture seule").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Valider ENTREE").assertDoesNotExist()
    }
}
