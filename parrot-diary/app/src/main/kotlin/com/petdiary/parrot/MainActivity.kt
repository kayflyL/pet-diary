package com.petdiary.parrot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.petdiary.parrot.core.designsystem.ParrotDiaryTheme
import com.petdiary.parrot.data.SettingsRepository
import com.petdiary.parrot.feature.pets.AddEditBirdScreen
import com.petdiary.parrot.feature.pets.BirdDetailScreen
import com.petdiary.parrot.feature.pets.BirdsScreen
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
                AppNavHost()
            }
        }
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(settingsRepository: SettingsRepository) : ViewModel() {
    val appearance: StateFlow<String> = settingsRepository.appearance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "SYSTEM")
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "birds") {
        composable("birds") {
            BirdsScreen(
                onBirdClick = { petId -> nav.navigate("bird/$petId") },
                onAddClick = { nav.navigate("bird/edit") },
            )
        }
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
            )
        }
    }
}
