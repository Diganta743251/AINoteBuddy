package com.ainotebuddy.app.ui.screens.template

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.data.model.organization.defaultNoteTemplates
import com.ainotebuddy.app.ui.theme.AINoteBuddyTheme
import java.time.LocalDateTime
import java.util.*

@Preview(showBackground = true)
@Composable
fun TemplateListScreenPreview() {
    // Use predefined defaults to match current NoteTemplate fields
    val sampleTemplates: List<NoteTemplate> = defaultNoteTemplates
    
    AINoteBuddyTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // For preview purposes, just render the list directly using TemplateList()
            // Local wrapper to access private TemplateList inside preview
            @Composable fun TemplateListWrapper() {
                // Use a basic text to avoid preview dependencies
                androidx.compose.material3.Text("Templates preview is available in app runtime")
            }
            TemplateListWrapper()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E1E1E, uiMode = 2)
@Composable
fun TemplateListScreenDarkPreview() {
    TemplateListScreenPreview()
}
