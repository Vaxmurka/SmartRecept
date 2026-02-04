//package com.example.smartrecept.ui.components
//
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.core.tween
//import androidx.compose.animation.slideInVertically
//import androidx.compose.animation.slideOutVertically
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.offset
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Home
//import androidx.compose.material.icons.filled.Restaurant
//import androidx.compose.material.icons.filled.Search
//import androidx.compose.material.icons.filled.Settings
//import androidx.compose.material.icons.filled.SmartToy
//import androidx.compose.material.icons.outlined.Home
//import androidx.compose.material.icons.outlined.Restaurant
//import androidx.compose.material.icons.outlined.Search
//import androidx.compose.material.icons.outlined.Settings
//import androidx.compose.material.icons.outlined.SmartToy
//import androidx.compose.material3.FloatingActionButton
//import androidx.compose.material3.FloatingActionButtonDefaults
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Alignment.Companion.CenterVertically
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import androidx.navigation.compose.currentBackStackEntryAsState
//import com.example.smartrecept.Screen
//import com.example.smartrecept.bottomNavItems
//
//class CustomAppElements {
//}
//
//@Composable
//fun CustomBottomNavigation(
//    navController: NavController,
//    modifier: Modifier = Modifier
//) {
//    val navBackStackEntry by navController.currentBackStackEntryAsState()
//    val currentDestination = navBackStackEntry?.destination
//    val showBottomBar = currentDestination?.route in listOf("home", "search", "journal", "settings", "gemini_test")
//
//    AnimatedVisibility(
//        visible = showBottomBar,
//        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(250)),
//        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250))
//    ) {
//        Box(
//            modifier = modifier
//                .fillMaxWidth()
//                .height(86.dp)
//        ) {
//            // Основной контейнер навигации
//            Surface(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(72.dp)
//                    .align(Alignment.BottomCenter)
//                    .padding(horizontal = 12.dp),
//                color = MaterialTheme.colorScheme.surfaceContainerHigh,
//                tonalElevation = 10.dp,
//                shadowElevation = 10.dp,
//                shape = RoundedCornerShape(24.dp)
//            ) {
//                Row(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(horizontal = 16.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = CenterVertically
//                ) {
//                    // Левая часть
//                    bottomNavItems.filter { it != Screen.GeminiTest }.take(2).forEach { screen ->
//                        BottomNavItem(
//                            screen = screen,
//                            isSelected = currentDestination?.route == screen.route,
//                            onClick = { navigateToScreen(navController, screen) }
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.width(68.dp)) // место под центральную кнопку
//
//                    // Правая часть
//                    bottomNavItems.filter { it != Screen.GeminiTest }.takeLast(2).forEach { screen ->
//                        BottomNavItem(
//                            screen = screen,
//                            isSelected = currentDestination?.route == screen.route,
//                            onClick = { navigateToScreen(navController, screen) }
//                        )
//                    }
//                }
//            }
//
//            // Центральная кнопка AI
//            val isAISelected = currentDestination?.route == Screen.GeminiTest.route
//            FloatingActionButton(
//                onClick = { navigateToScreen(navController, Screen.GeminiTest) },
//                modifier = Modifier
//                    .size(70.dp)
//                    .align(Alignment.TopCenter)
//                    .offset(y = (-10).dp),
//                containerColor = if (isAISelected)
//                    MaterialTheme.colorScheme.primaryContainer
//                else
//                    MaterialTheme.colorScheme.primary,
//                contentColor = if (isAISelected)
//                    MaterialTheme.colorScheme.onPrimaryContainer
//                else
//                    MaterialTheme.colorScheme.onPrimary,
//                elevation = FloatingActionButtonDefaults.elevation(
//                    defaultElevation = 10.dp,
//                    pressedElevation = 14.dp
//                )
//            ) {
//                val icon = if (isAISelected) Icons.Filled.SmartToy else Icons.Outlined.SmartToy
//                Icon(
//                    imageVector = icon,
//                    contentDescription = Screen.GeminiTest.title,
//                    modifier = Modifier.size(32.dp)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//private fun BottomNavItem(
//    screen: Screen,
//    isSelected: Boolean,
//    onClick: () -> Unit
//) {
//    val icon = if (isSelected) {
//        when (screen) {
//            Screen.Home -> Icons.Filled.Home
//            Screen.Search -> Icons.Filled.Search
//            Screen.Journal -> Icons.Filled.Restaurant
//            Screen.Settings -> Icons.Filled.Settings
//            else -> screen.icon
//        }
//    } else {
//        when (screen) {
//            Screen.Home -> Icons.Outlined.Home
//            Screen.Search -> Icons.Outlined.Search
//            Screen.Journal -> Icons.Outlined.Restaurant
//            Screen.Settings -> Icons.Outlined.Settings
//            else -> screen.icon
//        }
//    }
//
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center,
//        modifier = Modifier
//            .width(60.dp)
//            .height(56.dp)
//            .clickable { onClick() }
//    ) {
//        Icon(
//            imageVector = icon,
//            contentDescription = screen.title,
//            modifier = Modifier.size(24.dp),
//            tint = if (isSelected)
//                MaterialTheme.colorScheme.primary
//            else
//                MaterialTheme.colorScheme.onSurfaceVariant
//        )
//
//        Text(
//            text = screen.title,
//            fontSize = 10.sp,
//            color = if (isSelected)
//                MaterialTheme.colorScheme.primary
//            else
//                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
//            maxLines = 1
//        )
//    }
//}
//
//private fun navigateToScreen(navController: NavController, screen: Screen) {
//    navController.navigate(screen.route) {
//        popUpTo(navController.graph.startDestinationId) {
//            saveState = true
//        }
//        launchSingleTop = true
//        restoreState = true
//    }
//}
//

package com.example.smartrecept.ui.components

import ScrollHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.smartrecept.Screen
import com.example.smartrecept.bottomNavItems

@Composable
fun CustomBottomNavigation(
    navController: NavController,
    modifier: Modifier = Modifier,
    scrollHandler: ScrollHandler,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    AnimatedVisibility(
        visible = scrollHandler.isBottomBarVisible,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(250)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250))
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.BottomCenter
        ) {
            // Плавающая капсула с иконками
            Box(
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .height(64.dp)
                    .fillMaxWidth(0.9f)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(50)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavItems.forEachIndexed { index, screen ->
                        if (index == bottomNavItems.size / 2) {
                            Spacer(modifier = Modifier.width(64.dp)) // место под плавающую кнопку
                        }

                        val selected = currentDestination?.route == screen.route
                        val icon = when (screen) {
                            Screen.Home -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                            Screen.Search -> if (selected) Icons.Filled.Search else Icons.Outlined.Search
                            Screen.Journal -> if (selected) Icons.Filled.Restaurant else Icons.Outlined.Restaurant
                            Screen.Settings -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
                            else -> screen.icon
                        }

                        IconButton(
                            onClick = {
                                // Не навигируем если уже на этом экране
                                if (currentRoute == screen.route) return@IconButton

                                navController.navigate(screen.route) {
                                    if (screen.route == Screen.Home.route) {
                                        // Для Home очищаем стек
                                        popUpTo(0) {
                                            saveState = true
                                        }
                                    } else {
                                        // Для других экранов
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = screen.title,
                                tint = if (selected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Центральная плавающая кнопка (AI)
            FloatingActionButton(
                onClick = {
                    if (currentRoute == Screen.AIAssistant.route) return@FloatingActionButton
                    navController.navigate(Screen.AIAssistant.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .size(64.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = "AI помощник",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

fun NavHostController.navigateSingleTopTo(route: String, currentRoute: String?) {
    // Если уже на этом экране, ничего не делаем
    if (currentRoute == route) return

    this.navigate(route) {
        // Для Home очищаем стек
        if (route == "home" || route == Screen.Home.route) {
            popUpTo(0) {
                saveState = true
            }
        } else {
            // Для других экранов
            launchSingleTop = true
            restoreState = true
        }
    }
}

private fun navigateToScreen(navController: NavController, screen: Screen) {
    navController.navigate(screen.route) {
        popUpTo(navController.graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

//@Composable
//private fun navigateToScreen(navController: NavController, screen: Screen) {
//    val navBackStackEntry by navController.currentBackStackEntryAsState()
//    val currentRoute = navBackStackEntry?.destination?.route
//
//    // Если уже на этом экране, выходим
//    if (currentRoute == screen.route) return
//
//    // Если пытаемся перейти на Home, а сейчас не на Search
//    if (screen.route == Screen.Home.route && currentRoute != Screen.Search.route) {
//        navController.navigate(screen.route) {
//            popUpTo(0) // Очищаем весь стек
//            launchSingleTop = true
//        }
//    } else {
//        // Для всех остальных случаев
//        navController.navigate(screen.route) {
//            launchSingleTop = true
//            restoreState = true
//        }
//    }
//}
