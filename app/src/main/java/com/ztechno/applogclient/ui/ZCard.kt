package com.ztechno.applogclient.ui

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ztechno.applogclient.ui.render.ZCardItemInterface

@Composable
fun ZCard(p: ZCardItemInterface, textAlign: TextAlign = TextAlign.Center) {
  Card(
//    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant,),
    modifier = Modifier.padding(8.dp, 8.dp, 8.dp, 0.dp).defaultMinSize(minWidth = 380.dp, minHeight = 10.dp),
    backgroundColor = p.bgColor()
  ) {
    Text(text = p.getKey(), modifier = Modifier.padding(8.dp, 8.dp), textAlign = TextAlign.Left, fontSize = 12.sp)
//    Spacer(modifier = Modifier.height(8.dp))
    Text(text = p.getText(), modifier = Modifier.padding(69.dp, 8.dp, 8.dp, 8.dp), textAlign = textAlign, fontSize = 14.sp)
  }
}