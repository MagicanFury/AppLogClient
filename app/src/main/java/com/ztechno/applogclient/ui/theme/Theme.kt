package com.ztechno.applogclient.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


private val LightColorPalette = lightColors(
    primary = PrimaryColor,
    primaryVariant = PrimaryColorDark,
    secondary = PurpleColor,
    
    background = Color.White,
    onBackground = Color.Black,
    
    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

private val DarkColorPalette = darkColors(
    primary = PrimaryColor,
    primaryVariant = PrimaryColorDark,
    secondary = PurpleColor,
    
    error = ErrColor,
    onError = Color.White,
    
    background = backgroundDark,
    surface = cardBackground,
    onSurface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
//    secondaryVariant = cardBackground,
//    onSecondary = Color.White,
    
)

@Composable
fun AppLogClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = {
            Surface(
                modifier = Modifier.fillMaxSize().padding(0.dp),
                color = MaterialTheme.colors.background, // Material2
                content = content,
            )
        }
    )
    
}