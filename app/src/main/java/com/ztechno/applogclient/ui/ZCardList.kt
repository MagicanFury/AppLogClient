package com.ztechno.applogclient.ui

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.ztechno.applogclient.ui.render.ZCardItemInterface

@Composable
fun ZCardList(list: SnapshotStateList<ZCardItemInterface>?, textAlign: TextAlign = TextAlign.Left, modifier: Modifier = Modifier) {
  val mList = list ?: remember { mutableStateListOf() }
  LazyColumn(
    modifier = modifier
      .wrapContentHeight(Alignment.CenterVertically)
//      .verticalScroll(rememberScrollState()),
  ) {
    items(items = mList) {
      ZCard(it, textAlign)
    }
  }
  
//  Text("...", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(1.0f))
}