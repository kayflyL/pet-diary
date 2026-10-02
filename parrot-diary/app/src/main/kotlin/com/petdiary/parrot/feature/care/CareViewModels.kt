package com.petdiary.parrot.feature.care

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petdiary.parrot.data.ExpenseRepository
import com.petdiary.parrot.data.ReminderRepository
import com.petdiary.parrot.data.local.ReminderEntity
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

data class DueReminderUi(val id: String, val title: String, val daysLeft: Int, val overdue: Boolean)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
) : ViewModel() {

    val reminders: StateFlow<List<ReminderEntity>> = reminderRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val presets get() = reminderRepository.presets

    fun addPreset(kind: String, title: String, intervalDays: Int) {
        viewModelScope.launch {
            reminderRepository.addFromPreset(ReminderRepository.Preset(kind, title, intervalDays), System.currentTimeMillis())
        }
    }

    fun completeOnce(id: String) {
        viewModelScope.launch { reminderRepository.completeOnce(id) }
    }

    fun delete(id: String) {
        viewModelScope.launch { reminderRepository.delete(id) }
    }
}

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
