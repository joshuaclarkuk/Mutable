package com.joshuaclark.mutable.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.joshuaclark.mutable.model.SongViewModel
import com.joshuaclark.mutable.ui.screens.MainMenuScreen
import com.joshuaclark.mutable.ui.screens.SongScreen

object Routes {
    const val MAIN_MENU = "main_menu"
    const val SONG = "song"
}

@Composable
fun AppNavigation(navController: NavHostController, songViewModel: SongViewModel, onImportClicked: () -> Unit) {
    NavHost(navController, startDestination = Routes.MAIN_MENU) {
        composable(Routes.MAIN_MENU) {
            MainMenuScreen(onNavigateToSong = {
                navController.navigate(
                    Routes.SONG
                )
            }, onImportSong = onImportClicked, songViewModel = songViewModel)
        }
        composable(Routes.SONG) {
            SongScreen(onNavigateToMainMenu = {
                navController.navigate(
                    Routes.MAIN_MENU
                )
            }, songViewModel = songViewModel)
        }
    }
}