package com.ainotebuddy.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.gson.Gson
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    private val gson = Gson()
    var notes by mutableStateOf(listOf<String>())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Wearable.getDataClient(this).addListener(this)
        setContent {
            MaterialTheme {
                WearNoteScreen(notes)
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        Wearable.getDataClient(this).removeListener(this)
    }
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/notes") {
                val dataMap = com.google.android.gms.wearable.DataMapItem.fromDataItem(event.dataItem).dataMap
                val notesJson = dataMap.getString("notes_json")
                val noteList = gson.fromJson(notesJson, Array<String>::class.java).toList()
                runOnUiThread { notes = noteList }
            }
        }
    }
}

@Composable
fun WearNoteScreen(notes: List<String>) {
    var noteText by remember { mutableStateOf("") }
    val context = LocalContext.current
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { 
            // Use a simple position indicator without parameters
            PositionIndicator(
                scalingLazyListState = rememberScalingLazyListState()
            )
        }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text("AINoteBuddy", style = MaterialTheme.typography.title1)
            }
            item {
                Text("Quick Note Input:")
            }
            item {
                Text(noteText, modifier = Modifier.fillMaxWidth())
            }
            item {
                val scope = rememberCoroutineScope()
                Button(onClick = {
                    if (noteText.isNotBlank()) {
                        scope.launch {
                            val dataMap = PutDataMapRequest.create("/new_note").apply {
                                dataMap.putString("note_text", noteText)
                            }.asPutDataRequest()
                            Wearable.getDataClient(context as ComponentActivity).putDataItem(dataMap)
                            noteText = ""
                        }
                    }
                }, enabled = noteText.isNotBlank()) {
                    Text("Save")
                }
            }
            items(notes) { note ->
                Text(note, style = MaterialTheme.typography.body1)
            }
        }
    }
} 