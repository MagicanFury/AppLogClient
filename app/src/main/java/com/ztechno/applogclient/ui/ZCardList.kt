package com.ztechno.applogclient.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.ztechno.applogclient.ui.render.ZCardItemInterface

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ZPacketList(list: SnapshotStateList<ZCardItemInterface>?, textAlign: TextAlign = TextAlign.Center) {
  val mList = list ?: remember { mutableStateListOf() }
  LazyColumn(
    modifier = Modifier
//      .verticalScroll(rememberScrollState())
      .fillMaxHeight(0.9f)
  ) {
    items(items = mList) {
      ZCard(it, textAlign)
    }
    
  }
  Text("...")
}