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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.smartrecept.R
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
                    painter = painterResource(id = R.drawable.neurology_24px),
                    contentDescription = "AI помощник",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

fun NavHostController.navigateSingleTopTo(route: String, currentRoute: String?) {
    if (currentRoute == route) return

    this.navigate(route) {
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

