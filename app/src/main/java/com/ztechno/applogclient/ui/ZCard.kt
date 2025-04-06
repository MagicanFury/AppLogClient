package com.ztechno.applogclient.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ztechno.applogclient.http.ZPacket
import com.ztechno.applogclient.utils.ZPacketWrapper

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ZCard(p: ZPacketWrapper) {
  Card(
//    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant,),
    modifier = Modifier.padding(8.dp).size(width = 380.dp, height = 100.dp),
    backgroundColor = if (p.isSent()) MaterialTheme.colors.surface else  MaterialTheme.colors.error
  ) {
    Text(text = p.packet.key, modifier = Modifier.padding(8.dp, 16.dp), textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(16.dp))
    Text(text = p.packet.data.toString(), modifier = Modifier.padding(8.dp, 32.dp, 8.dp, 0.dp), textAlign = TextAlign.Center)
  }
}