package com.ztechno.applogclient.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ztechno.applogclient.http.ZPacket

@Composable
fun ZCard(p: ZPacket) {
  Card(
//    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant,),
    modifier = Modifier.padding(8.dp).size(width = 380.dp, height = 100.dp),
  ) {
    Text(text = p.key, modifier = Modifier.padding(8.dp, 16.dp), textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(16.dp))
    Text(text = p.data.toString(), modifier = Modifier.padding(8.dp, 32.dp, 8.dp, 0.dp), textAlign = TextAlign.Center)
  }
}