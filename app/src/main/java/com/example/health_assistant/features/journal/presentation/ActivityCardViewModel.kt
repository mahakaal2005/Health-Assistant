package com.example.health_assistant.features.journal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.health_assistant.features.journal.domain.ActivityCard
import com.example.health_assistant.features.journal.domain.usecase.GenerateActivityCardUseCase
import com.example.health_assistant.features.journal.domain.usecase.GetActivityCardsUseCase
import com.example.health_assistant.features.journal.workers.ActivityCardScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for Activity Card functionality
 * Manages UI state and coordinates with use cases
 */
@HiltViewModel
class ActivityCardViewModel @Inject constructor(
    private val getActivityCardsUseCase: GetActivityCardsUseCase,
    private val generateActivityCardUseCase: GenerateActivityCardUseCase,
    private val activityCardScheduler: ActivityCardScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityCardUiState())
    val uiState: StateFlow<ActivityCardUiState> = _uiState.asStateFlow()

    private val _selectedCard = MutableStateFlow<ActivityCard?>(null)
    val selectedCard: StateFlow<ActivityCard?> = _selectedCard.asStateFlow()

    init {
        loadRecentActivityCards()
    }

    /**
     * Load recent activity cards
     */
    fun loadRecentActivityCards(limit: Int = 30) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                getActivityCardsUseCase.getRecentActivityCards(limit)
                    .catch { error ->
                        _uiState.update {
                            it.copy(isLoading = false, error = error.message ?: "Unknown error occurred")
                        }
                    }
                    .collect { cards ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                activityCards = cards,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load activity cards")
                }
            }
        }
    }

    /**
     * Load activity cards for a specific time period
     */
    fun loadActivityCardsForPeriod(period: TimePeriod) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, selectedPeriod = period) }

            try {
                val flow = when (period) {
                    TimePeriod.WEEK -> getActivityCardsUseCase.getThisWeekActivityCards()
                    TimePeriod.MONTH -> getActivityCardsUseCase.getThisMonthActivityCards()
                    TimePeriod.ALL -> getActivityCardsUseCase.getAllActivityCards()
                    TimePeriod.LAST_7_DAYS -> getActivityCardsUseCase.getActivityCardsLastNDays(7)
                    TimePeriod.LAST_30_DAYS -> getActivityCardsUseCase.getActivityCardsLastNDays(30)
                }

                flow.catch { error ->
                        _uiState.update {
                            it.copy(isLoading = false, error = error.message ?: "Unknown error occurred")
                        }
                    }
                    .collect { cards ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                activityCards = cards,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load activity cards")
                }
            }
        }
    }

    /**
     * Select a specific activity card for detailed view
     */
    fun selectActivityCard(card: ActivityCard) {
        _selectedCard.value = card
    }

    /**
     * Get activity card for a specific date
     */
    fun getActivityCardByDate(date: LocalDate) {
        viewModelScope.launch {
            try {
                val card = getActivityCardsUseCase.getActivityCardByDate(date)
                _selectedCard.value = card
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to load activity card for $date")
                }
            }
        }
    }

    /**
     * Get today's activity card (view-only)
     */
    suspend fun getTodaysActivityCard(): ActivityCard? {
        return try {
            getActivityCardsUseCase.getTodayActivityCard()
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message ?: "Failed to load today's activity card") }
            null
        }
    }

    /**
     * Generate today's activity card (for testing date changes)
     */
    suspend fun generateTodaysActivityCard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Force generate card for today using the scheduler
                activityCardScheduler.forceGenerateCardForToday()

                // Wait a moment for generation to complete
                kotlinx.coroutines.delay(2000)

                // Refresh to show the new card
                loadRecentActivityCards()

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to generate activity card")
                }
            }
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Refresh activity cards (view-only)
     */
    fun refresh() {
        when (_uiState.value.selectedPeriod) {
            null -> loadRecentActivityCards()
            else -> loadActivityCardsForPeriod(_uiState.value.selectedPeriod!!)
        }
    }
}

/**
 * UI State for Activity Cards
 */
data class ActivityCardUiState(
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val activityCards: List<ActivityCard> = emptyList(),
    val selectedPeriod: TimePeriod? = null,
    val error: String? = null
)

/**
 * Time period options for filtering activity cards
 */
enum class TimePeriod {
    WEEK,
    MONTH,
    ALL,
    LAST_7_DAYS,
    LAST_30_DAYS
}