package com.ainotebuddy.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ainotebuddy.app.ui.screens.template.TemplateEditorScreen
import com.ainotebuddy.app.ui.screens.template.TemplateListScreen
import com.ainotebuddy.app.ui.viewmodel.template.TemplateEditorViewModel
import com.ainotebuddy.app.ui.viewmodel.template.TemplateListViewModel

/**
 * Sealed class representing the template navigation routes
 */
sealed class TemplateScreens(val route: String) {
    object List : TemplateScreens("template/list")
    object Editor : TemplateScreens("template/editor/{templateId}") {
        fun createRoute(templateId: String? = null) = "template/editor/${templateId ?: "new"}"
    }
}

/**
 * Adds template navigation to the NavGraphBuilder
 */
fun NavGraphBuilder.templateNavigation(
    navController: NavController,
    onBackClick: () -> Unit
) {
    // Template List Screen
    composable(TemplateScreens.List.route) {
        val viewModel: TemplateListViewModel = hiltViewModel()
        
        TemplateListScreen(
            onTemplateClick = { templateId ->
                navController.navigate(TemplateScreens.Editor.createRoute(templateId))
            },
            onCreateNewTemplate = {
                navController.navigate(TemplateScreens.Editor.createRoute())
            },
            viewModel = viewModel
        )
    }
    
    // Template Editor Screen
    composable(
        route = TemplateScreens.Editor.route,
        arguments = listOf(
            navArgument("templateId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val viewModel: TemplateEditorViewModel = hiltViewModel()
        val templateId = backStackEntry.arguments?.getString("templateId")
        
        // Load the template if editing
        LaunchedEffect(templateId) {
            templateId?.let { viewModel.loadTemplate(it) }
        }
        
        TemplateEditorScreen(
            templateId = templateId,
            onBackClick = onBackClick,
            onSaveClick = { _ ->
                // After saving, navigate back to the list
                navController.popBackStack(
                    route = TemplateScreens.List.route,
                    inclusive = false
                )
            },
            viewModel = viewModel
        )
    }
}

/**
 * Composable that provides navigation for template-related screens
 */
@Composable
fun TemplateNavigation(
    navController: NavController,
    onBackClick: () -> Unit
) {
    // Set up navigation controller for template screens
    val templateNavController = rememberNavController()
    
    // Set up navigation graph
    NavHost(
        navController = templateNavController,
        startDestination = TemplateScreens.List.route
    ) {
        templateNavigation(
            navController = templateNavController,
            onBackClick = onBackClick
        )
    }
}
