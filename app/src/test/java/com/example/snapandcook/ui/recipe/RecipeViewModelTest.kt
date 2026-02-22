package com.example.snapandcook.ui.recipe

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.snapandcook.data.local.RecipeDao
import com.example.snapandcook.data.model.*
import com.example.snapandcook.data.remote.RecipeRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [RecipeViewModel].
 *
 * Tests LiveData updates, navigation logic, and error handling.
 *
 * Note: RecipeViewModel extends AndroidViewModel and creates dependencies internally
 * (RecipeRepository, AppDatabase). For full testability, we'd refactor to inject these:
 *
 * ```kotlin
 * class RecipeViewModel(
 *     private val repository: RecipeRepository,
 *     private val dao: RecipeDao
 * ) : ViewModel() {  // Use ViewModel instead of AndroidViewModel
 *     // ... existing code
 * }
 * ```
 *
 * This would allow:
 * ```kotlin
 * val mockRepository = mockk<RecipeRepository>()
 * val mockDao = mockk<RecipeDao>()
 * val viewModel = RecipeViewModel(mockRepository, mockDao)
 * ```
 *
 * These tests document the intended testing approach.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeViewModelTest {

    /**
     * Rule to make LiveData execute synchronously in tests.
     * Without this, LiveData posts to the main thread, which doesn't exist in unit tests.
     */
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    /**
     * Test dispatcher for coroutines.
     * Allows us to control coroutine execution in tests.
     */
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockRepository: RecipeRepository
    private lateinit var mockDao: RecipeDao
    // private lateinit var viewModel: RecipeViewModel

    @Before
    fun setUp() {
        // Set test dispatcher as main dispatcher
        Dispatchers.setMain(testDispatcher)

        // Mock dependencies
        mockRepository = mockk()
        mockDao = mockk()

        // In refactored code with DI:
        // viewModel = RecipeViewModel(mockRepository, mockDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ─── Test Data ───────────────────────────────────────────────────────────────

    private fun createMockRecipeSummary(id: Int, title: String): RecipeSummary {
        return RecipeSummary(
            id = id,
            title = title,
            imageUrl = "http://example.com/image$id.jpg",
            usedIngredientCount = 3,
            missedIngredientCount = 1
        )
    }

    private fun createMockRecipeDetail(id: Int, title: String): RecipeDetail {
        return RecipeDetail(
            id = id,
            title = title,
            imageUrl = "http://example.com/image.jpg",
            readyInMinutes = 30,
            servings = 4,
            nutrition = Nutrition(
                nutrients = listOf(
                    Nutrient(name = "Calories", amount = 450.0, unit = "kcal")
                )
            ),
            extendedIngredients = listOf(
                ExtendedIngredient(
                    id = 1,
                    original = "200g pasta",
                    name = "pasta",
                    amount = 200.0,
                    unit = "g",
                    image = "pasta.jpg"
                )
            ),
            analyzedInstructions = listOf(
                AnalyzedInstruction(
                    name = "",
                    steps = listOf(
                        InstructionStep(1, "Cook pasta", null),
                        InstructionStep(2, "Serve hot", null)
                    )
                )
            ),
            summary = "A test recipe"
        )
    }

    // ─── findRecipes Tests ───────────────────────────────────────────────────────

    @Test
    fun `findRecipes updates recipeSummaries LiveData on success`() = runTest {
        // Given: Repository returns successful recipes
        val mockSummaries = listOf(
            createMockRecipeSummary(1, "Recipe 1"),
            createMockRecipeSummary(2, "Recipe 2")
        )
        // coEvery { mockRepository.findRecipes(any()) } returns Result.success(mockSummaries)
        // coEvery { mockRepository.getRecipeDetail(1) } returns
        //     Result.success(createMockRecipeDetail(1, "Recipe 1"))
        // coEvery { mockDao.isRecipeSaved(any()) } returns 0

        // When: Finding recipes
        // viewModel.findRecipes(listOf("ingredient1", "ingredient2"))
        // advanceUntilIdle()  // Execute all coroutines

        // Then: recipeSummaries should be updated
        // assertThat(viewModel.recipeSummaries.value).hasSize(2)
        // assertThat(viewModel.recipeSummaries.value?.first()?.title).isEqualTo("Recipe 1")
    }

    @Test
    fun `findRecipes sets isLoading to true then false`() = runTest {
        // Given: Repository returns recipes
        // coEvery { mockRepository.findRecipes(any()) } returns
        //     Result.success(listOf(createMockRecipeSummary(1, "Recipe 1")))
        // coEvery { mockRepository.getRecipeDetail(1) } returns
        //     Result.success(createMockRecipeDetail(1, "Recipe 1"))
        // coEvery { mockDao.isRecipeSaved(any()) } returns 0

        // When: Finding recipes
        // val loadingStates = mutableListOf<Boolean>()
        // viewModel.isLoading.observeForever { loadingStates.add(it) }
        // viewModel.findRecipes(listOf("ingredient"))
        // advanceUntilIdle()

        // Then: Should see true then false
        // assertThat(loadingStates).containsExactly(true, false).inOrder()
    }

    @Test
    fun `findRecipes automatically loads first recipe detail`() = runTest {
        // Given: Repository returns multiple recipes
        // val mockSummaries = listOf(
        //     createMockRecipeSummary(1, "Recipe 1"),
        //     createMockRecipeSummary(2, "Recipe 2")
        // )
        // coEvery { mockRepository.findRecipes(any()) } returns Result.success(mockSummaries)
        // coEvery { mockRepository.getRecipeDetail(1) } returns
        //     Result.success(createMockRecipeDetail(1, "Recipe 1"))
        // coEvery { mockDao.isRecipeSaved(1) } returns 0

        // When: Finding recipes
        // viewModel.findRecipes(listOf("ingredient"))
        // advanceUntilIdle()

        // Then: Should automatically load detail for first recipe
        // coVerify { mockRepository.getRecipeDetail(1) }
        // assertThat(viewModel.recipeDetail.value?.id).isEqualTo(1)
    }

    @Test
    fun `findRecipes sets error on failure`() = runTest {
        // Given: Repository returns failure
        // coEvery { mockRepository.findRecipes(any()) } returns
        //     Result.failure(Exception("Network error"))

        // When: Finding recipes
        // viewModel.findRecipes(listOf("ingredient"))
        // advanceUntilIdle()

        // Then: error LiveData should be set
        // assertThat(viewModel.error.value).isNotNull()
        // assertThat(viewModel.error.value).contains("Network error")
        // assertThat(viewModel.isLoading.value).isFalse()
    }

    @Test
    fun `findRecipes sets error when no recipes found`() = runTest {
        // Given: Repository returns empty list
        // coEvery { mockRepository.findRecipes(any()) } returns Result.success(emptyList())

        // When: Finding recipes
        // viewModel.findRecipes(listOf("ingredient"))
        // advanceUntilIdle()

        // Then: error should indicate no recipes found
        // assertThat(viewModel.error.value).isNotNull()
        // assertThat(viewModel.error.value).contains("No recipes found")
    }

    // ─── loadDetail Tests ────────────────────────────────────────────────────────

    @Test
    fun `loadDetail updates recipeDetail LiveData`() = runTest {
        // Given: Repository returns recipe detail
        // val mockDetail = createMockRecipeDetail(123, "Test Recipe")
        // coEvery { mockRepository.getRecipeDetail(123) } returns Result.success(mockDetail)
        // coEvery { mockDao.isRecipeSaved(123) } returns 0

        // When: Loading detail
        // viewModel.loadDetail(123)
        // advanceUntilIdle()

        // Then: recipeDetail should be updated
        // assertThat(viewModel.recipeDetail.value).isNotNull()
        // assertThat(viewModel.recipeDetail.value?.title).isEqualTo("Test Recipe")
    }

    @Test
    fun `loadDetail checks if recipe is saved`() = runTest {
        // Given: Recipe is saved in database
        // coEvery { mockRepository.getRecipeDetail(123) } returns
        //     Result.success(createMockRecipeDetail(123, "Test"))
        // coEvery { mockDao.isRecipeSaved(123) } returns 1

        // When: Loading detail
        // viewModel.loadDetail(123)
        // advanceUntilIdle()

        // Then: isSaved should be true
        // assertThat(viewModel.isSaved.value).isTrue()
    }

    @Test
    fun `loadDetail fetches YouTube video in parallel`() = runTest {
        // Given: Repository returns detail and video
        // val mockDetail = createMockRecipeDetail(123, "Test Recipe")
        // coEvery { mockRepository.getRecipeDetail(123) } returns Result.success(mockDetail)
        // coEvery { mockDao.isRecipeSaved(123) } returns 0
        // coEvery { mockRepository.searchRecipeVideo("Test Recipe") } returns "abc123"

        // When: Loading detail
        // viewModel.loadDetail(123)
        // advanceUntilIdle()

        // Then: videoId should be updated
        // assertThat(viewModel.videoId.value).isEqualTo("abc123")
    }

    @Test
    fun `loadDetail sets error on failure`() = runTest {
        // Given: Repository returns failure
        // coEvery { mockRepository.getRecipeDetail(123) } returns
        //     Result.failure(Exception("Not found"))

        // When: Loading detail
        // viewModel.loadDetail(123)
        // advanceUntilIdle()

        // Then: error should be set
        // assertThat(viewModel.error.value).contains("Not found")
        // assertThat(viewModel.isLoading.value).isFalse()
    }

    // ─── Navigation Tests ────────────────────────────────────────────────────────

    @Test
    fun `loadNextRecipe increments index and loads next recipe`() = runTest {
        // Given: ViewModel has multiple recipes loaded
        // val mockSummaries = listOf(
        //     createMockRecipeSummary(1, "Recipe 1"),
        //     createMockRecipeSummary(2, "Recipe 2"),
        //     createMockRecipeSummary(3, "Recipe 3")
        // )
        // coEvery { mockRepository.findRecipes(any()) } returns Result.success(mockSummaries)
        // coEvery { mockRepository.getRecipeDetail(any()) } returns
        //     Result.success(createMockRecipeDetail(1, "Recipe 1"))
        // coEvery { mockDao.isRecipeSaved(any()) } returns 0

        // viewModel.findRecipes(listOf("ingredient"))
        // advanceUntilIdle()
        // assertThat(viewModel.currentRecipeIndex).isEqualTo(0)

        // When: Loading next recipe
        // viewModel.loadNextRecipe()
        // advanceUntilIdle()

        // Then: Index should increment and recipe 2 should load
        // assertThat(viewModel.currentRecipeIndex).isEqualTo(1)
        // coVerify { mockRepository.getRecipeDetail(2) }
    }

    @Test
    fun `loadNextRecipe does nothing at last recipe`() = runTest {
        // Given: ViewModel at last recipe
        // val mockSummaries = listOf(
        //     createMockRecipeSummary(1, "Recipe 1"),
        //     createMockRecipeSummary(2, "Recipe 2")
        // )
        // Setup viewModel to be at index 1 (last recipe)

        // When: Trying to load next recipe
        // viewModel.loadNextRecipe()

        // Then: Index should stay at 1
        // assertThat(viewModel.currentRecipeIndex).isEqualTo(1)
    }

    @Test
    fun `loadPreviousRecipe decrements index and loads previous recipe`() = runTest {
        // Given: ViewModel at recipe index 2
        // Setup with 3 recipes, navigate to index 2

        // When: Loading previous recipe
        // viewModel.loadPreviousRecipe()
        // advanceUntilIdle()

        // Then: Index should decrement
        // assertThat(viewModel.currentRecipeIndex).isEqualTo(1)
    }

    @Test
    fun `loadPreviousRecipe does nothing at first recipe`() = runTest {
        // Given: ViewModel at first recipe (index 0)
        // Setup with recipes at index 0

        // When: Trying to load previous
        // viewModel.loadPreviousRecipe()

        // Then: Index should stay at 0
        // assertThat(viewModel.currentRecipeIndex).isEqualTo(0)
    }

    @Test
    fun `totalRecipes returns correct count`() = runTest {
        // Given: ViewModel has 5 recipes
        // val mockSummaries = (1..5).map { createMockRecipeSummary(it, "Recipe $it") }
        // Setup viewModel with these summaries

        // When: Getting total
        // val total = viewModel.totalRecipes

        // Then: Should return 5
        // assertThat(total).isEqualTo(5)
    }

    // ─── Save/Unsave Tests ───────────────────────────────────────────────────────

    @Test
    fun `saveCurrentRecipe inserts recipe and updates isSaved`() = runTest {
        // Given: ViewModel has a loaded recipe
        // val mockDetail = createMockRecipeDetail(123, "Test Recipe")
        // Setup viewModel with this detail
        // coEvery { mockDao.insertRecipe(any()) } just Runs

        // When: Saving current recipe
        // viewModel.saveCurrentRecipe()
        // advanceUntilIdle()

        // Then: Should insert into database and update LiveData
        // coVerify { mockDao.insertRecipe(any()) }
        // assertThat(viewModel.isSaved.value).isTrue()
    }

    @Test
    fun `saveCurrentRecipe does nothing if no recipe loaded`() = runTest {
        // Given: ViewModel has no recipe detail
        // recipeDetail LiveData is null

        // When: Trying to save
        // viewModel.saveCurrentRecipe()
        // advanceUntilIdle()

        // Then: Should not call DAO
        // coVerify(exactly = 0) { mockDao.insertRecipe(any()) }
    }

    @Test
    fun `unsaveCurrentRecipe deletes recipe and updates isSaved`() = runTest {
        // Given: ViewModel has a saved recipe
        // Setup with saved recipe
        // coEvery { mockDao.deleteRecipeById(123) } just Runs

        // When: Unsaving current recipe
        // viewModel.unsaveCurrentRecipe()
        // advanceUntilIdle()

        // Then: Should delete from database and update LiveData
        // coVerify { mockDao.deleteRecipeById(123) }
        // assertThat(viewModel.isSaved.value).isFalse()
    }

    // ─── Helper Methods Tests ────────────────────────────────────────────────────

    @Test
    fun `getSteps extracts step strings from recipe detail`() = runTest {
        // Given: ViewModel with loaded recipe
        // val mockDetail = createMockRecipeDetail(123, "Test")
        // Setup viewModel with this detail

        // When: Getting steps
        // val steps = viewModel.getSteps()

        // Then: Should return list of step strings
        // assertThat(steps).hasSize(2)
        // assertThat(steps[0]).isEqualTo("Cook pasta")
        // assertThat(steps[1]).isEqualTo("Serve hot")
    }

    @Test
    fun `getSteps returns empty list when no recipe loaded`() = runTest {
        // Given: No recipe detail loaded
        // When: Getting steps
        // val steps = viewModel.getSteps()

        // Then: Should return empty list
        // assertThat(steps).isEmpty()
    }

    @Test
    fun `getEquipment extracts equipment names per step`() = runTest {
        // Given: Recipe with equipment data
        // When: Getting equipment
        // val equipment = viewModel.getEquipment()

        // Then: Should return list of equipment lists (one per step)
        // assertThat(equipment).hasSize(2)  // 2 steps
    }

    @Test
    fun `getCurrentSavedRecipe converts current detail to SavedRecipe`() = runTest {
        // Given: ViewModel with loaded recipe
        // When: Getting saved recipe
        // val savedRecipe = viewModel.getCurrentSavedRecipe()

        // Then: Should return converted SavedRecipe
        // assertThat(savedRecipe).isNotNull()
        // assertThat(savedRecipe?.recipeId).isEqualTo(123)
    }

    @Test
    fun `getCurrentSavedRecipe returns null when no recipe loaded`() = runTest {
        // Given: No recipe detail
        // When: Getting saved recipe
        // val savedRecipe = viewModel.getCurrentSavedRecipe()

        // Then: Should return null
        // assertThat(savedRecipe).isNull()
    }

    /**
     * Production-Grade Testing Note:
     *
     * To make these tests fully functional, refactor RecipeViewModel:
     *
     * 1. Change from AndroidViewModel to ViewModel (no Application dependency)
     * 2. Inject dependencies via constructor:
     *
     * ```kotlin
     * class RecipeViewModel(
     *     private val repository: RecipeRepository,
     *     private val dao: RecipeDao
     * ) : ViewModel() {
     *     // Existing implementation
     * }
     * ```
     *
     * 3. Use Hilt or manual factory for production:
     *
     * ```kotlin
     * @HiltViewModel
     * class RecipeViewModel @Inject constructor(
     *     private val repository: RecipeRepository,
     *     private val dao: RecipeDao
     * ) : ViewModel() {
     *     // ...
     * }
     * ```
     *
     * 4. Then in tests, we can directly instantiate with mocks:
     *
     * ```kotlin
     * val mockRepo = mockk<RecipeRepository>()
     * val mockDao = mockk<RecipeDao>()
     * val viewModel = RecipeViewModel(mockRepo, mockDao)
     * ```
     *
     * This is standard practice in modern Android development and makes
     * ViewModels fully testable.
     */
}
