package com.petdiary.parrot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.petdiary.parrot.core.designsystem.ParrotDiaryTheme
import com.petdiary.parrot.data.SettingsRepository
import com.petdiary.parrot.feature.care.ExpensesScreen
import com.petdiary.parrot.feature.care.RemindersScreen
import com.petdiary.parrot.feature.me.MeScreen
import com.petdiary.parrot.feature.food.FoodLibScreen
import com.petdiary.parrot.feature.memories.MemoriesScreen
import com.petdiary.parrot.feature.memories.PlaceDetailScreen
import com.petdiary.parrot.feature.memories.PlacesScreen
import com.petdiary.parrot.feature.pets.AddEditBirdScreen
import com.petdiary.parrot.feature.pets.BirdDetailScreen
import com.petdiary.parrot.feature.pets.BirdsScreen
import com.petdiary.parrot.feature.today.TodayScreen
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearance by vm.appearance.collectAsState()
            ParrotDiaryTheme(appearance = appearance) {
                AppNavHost(appearance = appearance, onCycleAppearance = vm::cycleAppearance)
            }
        }
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(private val settingsRepository: SettingsRepository) : ViewModel() {
    val appearance: StateFlow<String> = settingsRepository.appearance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "SYSTEM")

    private val order = listOf("SYSTEM", "LIGHT", "DARK")

    fun cycleAppearance() {
        viewModelScope.launch {
            settingsRepository.setAppearance(order[(order.indexOf(appearance.value) + 1) % order.size])
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun AppNavHost(appearance: String, onCycleAppearance: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabs = listOf(
        Tab("today", "今天", Icons.Outlined.Home),
        Tab("memories", "回忆", Icons.Outlined.PhotoLibrary),
        Tab("birds", "鸟", Icons.Outlined.Pets),
        Tab("me", "我的", Icons.Outlined.Settings),
    )
    val isTopLevel = route in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "today",
            modifier = Modifier.padding(padding),
        ) {
            composable("today") {
                TodayScreen(
                    onBirdClick = { petId -> nav.navigate("bird/$petId") },
                    onAppearanceToggle = onCycleAppearance,
                    appearanceDark = appearance == "DARK",
                )
            }
            composable("memories") {
                MemoriesScreen(
                    onOpenPlace = { name -> nav.navigate("place/${android.net.Uri.encode(name)}") },
                    onOpenPlaces = { nav.navigate("places") },
                )
            }
            composable("birds") {
                BirdsScreen(
                    onBirdClick = { petId -> nav.navigate("bird/$petId") },
                    onAddClick = { nav.navigate("bird/edit") },
                )
            }
            composable("me") {
                MeScreen(
                    appearance = appearance,
                    onCycleAppearance = onCycleAppearance,
                    onOpenReminders = { nav.navigate("reminders") },
                    onOpenExpenses = { nav.navigate("expenses") },
                )
            }
            composable("reminders") { RemindersScreen(onBack = { nav.popBackStack() }) }
            composable(
                route = "place/{name}",
                arguments = listOf(navArgument("name") { type = NavType.StringType }),
            ) {
                PlaceDetailScreen(onBack = { nav.popBackStack() })
            }
            composable("places") {
                PlacesScreen(
                    onBack = { nav.popBackStack() },
                    onOpenPlace = { name -> nav.navigate("place/" + android.net.Uri.encode(name)) },
                )
            }
            composable("expenses") { ExpensesScreen(onBack = { nav.popBackStack() }) }
            composable(
                route = "bird/edit?petId={petId}",
                arguments = listOf(navArgument("petId") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                AddEditBirdScreen(onDone = { nav.popBackStack() })
            }
            composable("bird/{petId}") { entry ->
                val petId = entry.arguments?.getString("petId").orEmpty()
                BirdDetailScreen(
                    onEdit = { nav.navigate("bird/edit?petId=$petId") },
                    onBack = { nav.popBackStack() },
                    onOpenFoodLib = { nav.navigate("food/$petId") },
                )
            }
            composable("food/{petId}") { entry ->
                val petId = entry.arguments?.getString("petId").orEmpty()
                FoodLibScreen(petId = petId, onBack = { nav.popBackStack() })
            }
        }
    }
}
