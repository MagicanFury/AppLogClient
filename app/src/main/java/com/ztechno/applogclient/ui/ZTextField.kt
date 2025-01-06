package com.ztechno.applogclient.ui

import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ztechno.applogclient.SetupViewModel

@Composable
fun ZTextField(viewModel: SetupViewModel, initialValue: String? = "", label: String = "Label", modifier: Modifier = Modifier) {
//  var text by remember { mutableStateOf(initialValue ?: "Text") }
  TextField(
    value = viewModel.deviceId,
    onValueChange = { viewModel.updateDeviceId(it) },
    label = { Text(label) },
    modifier = modifier
  )
//  var text by remember { mutableStateOf(initialValue ?: "Text") }
//  TextField(
//    value = text,
//    onValueChange = { text = it },
//    label = { Text("Label") },
//    modifier = modifier
//  )
}