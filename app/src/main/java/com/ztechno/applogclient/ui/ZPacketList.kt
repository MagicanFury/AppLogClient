package com.ztechno.applogclient.ui

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import com.ztechno.applogclient.http.ZPacket

@Composable
fun ZPacketList(list: SnapshotStateList<ZPacket>?) {
  val mList = list ?: remember { mutableStateListOf() }
  LazyColumn(
    modifier = Modifier
//      .verticalScroll(rememberScrollState())
      .fillMaxHeight(0.9f)
  ) {
    items(items = mList) {
      ZCard(it)
    }
  }
  Text("...")
}