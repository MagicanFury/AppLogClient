package com.ztechno.applogclient.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

//import com.spr.jetpack_loading.components.indicators.LineSpinFadeLoaderIndicator

abstract class ZComponentActivity : ComponentActivity() {

  var loadingViewModel = LoadingViewModel()
  
  @Composable
  fun loadable(viewModel: LoadingViewModel = loadingViewModel, content: @Composable () -> Unit) {
    val state by remember { viewModel.isLoading }
    
    Column(
      modifier = Modifier.fillMaxHeight(1f)
        .padding(bottom = 60.dp,start = 8.dp, end = 8.dp), // give space for the button,
      verticalArrangement = Arrangement.Center,
//      horizontalAlignment = Alignment.CenterHorizontally,
    
    ) {
      if (state) {
//        LineSpinFadeLoaderIndicator(color = MaterialTheme.colors.primary)
        Text("Loading", color = MaterialTheme.colors.primary)
      } else {
        content()
      }
      
    }
  }
}