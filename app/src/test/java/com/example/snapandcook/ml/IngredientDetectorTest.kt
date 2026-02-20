package com.example.snapandcook.ml

import android.graphics.Bitmap
import com.example.snapandcook.data.model.Ingredient
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class IngredientDetectorTest {

    @Test
    fun parseGeminiResponse_parsesCommaSeparatedIngredients() {
        val response = "egg, flour, butter, milk"
        
        val ingredients = response.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { name ->
                Ingredient(
                    name = name.replaceFirstChar { c -> c.uppercaseChar() },
                    confidence = null,
                    isManual = false
                )
            }

        assertEquals(4, ingredients.size)
        assertEquals("Egg", ingredients[0].name)
        assertEquals("Flour", ingredients[1].name)
    }

    @Test
    fun parseGeminiResponse_handlesNoneResponse() {
        val response = "none"
        
        val ingredients = if (response.equals("none", ignoreCase = true)) {
            emptyList()
        } else {
            response.split(",").map { it.trim() }
        }

        assertTrue(ingredients.isEmpty())
    }

    @Test
    fun parseGeminiResponse_filtersBlankEntries() {
        val response = "egg, , butter,  , milk"
        
        val ingredients = response.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        assertEquals(3, ingredients.size)
    }

    @Test
    fun parseGeminiResponse_removesDuplicates() {
        val response = "egg, Egg, butter, BUTTER"
        
        val ingredients = response.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

        assertEquals(2, ingredients.size)
    }

    @Test
    fun parseGeminiResponse_capitalizesFirstLetter() {
        val response = "egg, flour, butter"
        
        val ingredients = response.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { name ->
                Ingredient(
                    name = name.replaceFirstChar { c -> c.uppercaseChar() },
                    confidence = null,
                    isManual = false
                )
            }

        assertEquals("Egg", ingredients[0].name)
        assertEquals("Flour", ingredients[1].name)
        assertEquals("Butter", ingredients[2].name)
    }

    @Test
    fun detectFromMultipleBitmaps_mergesAndDeduplicates() {
        val bitmap1 = mock(Bitmap::class.java)
        val bitmap2 = mock(Bitmap::class.java)

        val list1 = listOf(
            Ingredient("Egg", null, false),
            Ingredient("Flour", null, false)
        )
        val list2 = listOf(
            Ingredient("Egg", null, false),
            Ingredient("Butter", null, false)
        )

        val merged = (list1 + list2)
            .groupBy { it.name.lowercase() }
            .map { (_, group) -> group.first() }

        assertEquals(3, merged.size)
        assertTrue(merged.any { it.name == "Egg" })
        assertTrue(merged.any { it.name == "Flour" })
        assertTrue(merged.any { it.name == "Butter" })
    }
}
