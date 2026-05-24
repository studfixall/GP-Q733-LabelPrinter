package com.gp.q733.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gp.q733.domain.model.Label
import com.gp.q733.presentation.ui.screens.ScanProductScreen
import com.gp.q733.presentation.ui.screens.device.DeviceScreen
import com.gp.q733.presentation.ui.screens.editor.EditorScreen
import com.gp.q733.presentation.ui.screens.home.HomeScreen
import com.gp.q733.presentation.ui.screens.settings.SettingsScreen
import com.gp.q733.presentation.viewmodel.DeviceViewModel
import com.gp.q733.presentation.viewmodel.EditorViewModel
import com.gp.q733.presentation.viewmodel.HomeViewModel
import com.gp.q733.presentation.viewmodel.ScanProductViewModel
import com.gp.q733.presentation.viewmodel.SettingsViewModel
import com.gp.q733.ui.product.ProductManagementScreen
import com.gp.q733.ui.product.ProductViewModel
import com.gp.q733.ui.template.TemplateBrowserScreen
import com.gp.q733.ui.template.TemplateBrowserViewModel
import com.gp.q733.ui.template.TemplatePrintScreen
import com.gp.q733.ui.template.TemplatePrintViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Device : Screen("device")
    object Editor : Screen("editor/{templateId}?width={width}&height={height}") {
        fun createRoute(templateId: String = "new", width: Float? = null, height: Float? = null): String {
            return if (width != null && height != null) {
                "editor/$templateId?width=$width&height=$height"
            } else {
                "editor/$templateId"
            }
        }
    }
    object Settings : Screen("settings")
    object ScanProduct : Screen("scan_product")
    object ProductManagement : Screen("product_management")
    object TemplateBrowser : Screen("template_browser")
    object TemplatePrint : Screen("template_print")
}

/**
 * Temporary shared holder to pass selected template Label between screens
 */
object SharedTemplateHolder {
    var label: Label? = null
}

@Composable
fun Q733NavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()

            HomeScreen(
                viewModel = viewModel,
                onNavigateToDevice = { navController.navigate(Screen.Device.route) },
                onNavigateToEditor = { templateId, width, height ->
                    navController.navigate(Screen.Editor.createRoute(templateId, width, height))
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToScanProduct = { navController.navigate(Screen.ScanProduct.route) },
                onNavigateToProductManagement = { navController.navigate(Screen.ProductManagement.route) },
                onNavigateToTemplateBrowser = { navController.navigate(Screen.TemplateBrowser.route) },
                onEditLabel = { label ->
                    navController.navigate(Screen.Editor.createRoute("saved_${label.id}"))
                }
            )
        }

        composable(Screen.Device.route) {
            val viewModel: DeviceViewModel = hiltViewModel()
            DeviceScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("templateId") { type = NavType.StringType },
                navArgument("width") {
                    type = NavType.FloatType
                    defaultValue = 50f
                },
                navArgument("height") {
                    type = NavType.FloatType
                    defaultValue = 30f
                }
            )
        ) { backStackEntry ->
            val viewModel: EditorViewModel = hiltViewModel()
            val templateId = backStackEntry.arguments?.getString("templateId") ?: "new"
            val width = backStackEntry.arguments?.getFloat("width") ?: 50f
            val height = backStackEntry.arguments?.getFloat("height") ?: 30f

            var lastLoadedTemplate by rememberSaveable { mutableStateOf("") }
            var lastLabelSize by rememberSaveable { mutableStateOf(Pair(0f, 0f)) }

            LaunchedEffect(templateId, width, height) {
                val currentSize = Pair(width, height)

                when {
                    templateId == "new" && (templateId != lastLoadedTemplate || currentSize != lastLabelSize) -> {
                        viewModel.resetLabel(width, height)
                        lastLabelSize = currentSize
                    }
                    templateId.startsWith("saved_") && templateId != lastLoadedTemplate -> {
                        val labelId = templateId.removePrefix("saved_")
                        viewModel.loadLabelFromStore(labelId)
                    }
                    templateId != "new" && !templateId.startsWith("saved_") && templateId != lastLoadedTemplate -> {
                        viewModel.loadTemplateSync(templateId)
                    }
                }
                lastLoadedTemplate = templateId
            }

            EditorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onPrint = { /* Print is handled inside ViewModel */ }
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ScanProduct.route) {
            val viewModel: ScanProductViewModel = hiltViewModel()
            ScanProductScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { labelId, width, height ->
                    navController.navigate(Screen.Editor.createRoute(labelId, width, height))
                }
            )
        }

        composable(Screen.ProductManagement.route) {
            val viewModel: ProductViewModel = hiltViewModel()
            ProductManagementScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TemplateBrowser.route) {
            val viewModel: TemplateBrowserViewModel = hiltViewModel()
            TemplateBrowserScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onTemplateSelected = { label ->
                    SharedTemplateHolder.label = label
                    navController.navigate(Screen.TemplatePrint.route)
                }
            )
        }

        composable(Screen.TemplatePrint.route) {
            val viewModel: TemplatePrintViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                SharedTemplateHolder.label?.let {
                    viewModel.setTemplate(it)
                    SharedTemplateHolder.label = null
                }
            }
            TemplatePrintScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
