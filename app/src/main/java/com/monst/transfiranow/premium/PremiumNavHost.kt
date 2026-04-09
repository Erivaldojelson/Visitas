package com.monst.transfiranow.premium

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.monst.transfiranow.ui.VisitasViewModel

private object Routes {
    const val HOME = "home"
    const val DETAIL = "detail/{id}"
    const val EDIT = "edit"
    fun detail(id: String) = "detail/$id"
}

@Composable
fun PremiumNavHost(
    viewModel: VisitasViewModel,
    onPickPhoto: () -> Unit,
    onPickQrCode: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.HOME, modifier = modifier) {
        composable(Routes.HOME) {
            PremiumHomeScreen(
                viewModel = viewModel,
                onCreateNew = {
                    viewModel.clearDraft()
                    nav.navigate(Routes.EDIT)
                },
                onOpen = { card -> nav.navigate(Routes.detail(card.id)) },
                onClose = onClose
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStack ->
            val id = backStack.arguments?.getString("id").orEmpty()
            PremiumDetailScreen(
                viewModel = viewModel,
                cardId = id,
                onBack = { nav.popBackStack() },
                onEdit = { card ->
                    viewModel.editCard(card)
                    nav.navigate(Routes.EDIT)
                }
            )
        }

        composable(Routes.EDIT) {
            PremiumEditScreen(
                viewModel = viewModel,
                onBack = { nav.popBackStack() },
                onPickPhoto = onPickPhoto,
                onPickQrCode = onPickQrCode,
                onDone = { nav.popBackStack(Routes.HOME, inclusive = false) }
            )
        }
    }
}
