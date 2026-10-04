package com.petdiary.parrot.feature.care

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petdiary.parrot.data.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExpenseStatsUi(
    val total: Double,
    val entries: List<com.petdiary.parrot.data.local.ExpenseLogEntity>,
    val byCategory: List<com.petdiary.parrot.data.local.CategoryTotalRow>,
)

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    val period = MutableStateFlow("MONTH")

    val entries: StateFlow<List<com.petdiary.parrot.data.local.ExpenseLogEntity>> = period
        .flatMapLatest { p ->
            val now = System.currentTimeMillis()
            val since = when (p) {
                "WEEK" -> now - 7L * 86_400_000L
                "YEAR" -> now - 365L * 86_400_000L
                else -> now - 30L * 86_400_000L
            }
            expenseRepository.observeSince(since)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addExpense(category: String, amount: Double, note: String?) {
        viewModelScope.launch { expenseRepository.add(null, category, amount, note) }
    }
}
