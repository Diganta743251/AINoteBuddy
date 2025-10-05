package com.ainotebuddy.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class AinoteBuddyIssueRegistry : IssueRegistry() {
    override val issues: List<Issue> = listOf(
        SplitWhitespaceDetector.ISSUE,
        SplitWhitespaceDetector.ISSUE_SEARCH_PACKAGE_CONSISTENCY,
        SplitWhitespaceDetector.ISSUE_AI_PACKAGE_CONSISTENCY,
    )
    override val api = CURRENT_API
}