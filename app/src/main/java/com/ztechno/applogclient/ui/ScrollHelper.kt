package com.ztechno.applogclient.ui

import android.graphics.Paint.Align
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.ztechno.applogclient.ui.render.ZCardItemInterface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun ScrollableWithBottomButton(
  scrollContent: (@Composable () -> Unit)?,
  bottomContent: (@Composable () -> Unit)? = null,
  btnText: String,
  btnClick: () -> Unit
  ) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    if (scrollContent != null) {
      scrollContent()
    } else {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = 60.dp, start = 16.dp, end = 16.dp) // give space for the button
          .verticalScroll(rememberScrollState()),
      ) {
        repeat(50) {
          Text("Item #$it", modifier = Modifier.padding(16.dp))
        }
      }
    }
    
    if (bottomContent != null) {
      bottomContent()
    } else {
      Button(
        onClick = btnClick,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Text(btnText)
      }
    }
    
  }
}