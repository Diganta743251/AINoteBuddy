package com.ainotebuddy.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import kotlin.test.Test

class SplitWhitespaceChainedQuickFixTest {

    private val SEARCH_STUB = kotlin(
        """
        package com.ainotebuddy.app.search
        
        val WS_REGEX: Regex = Regex("\\s+")
        """
    ).indented().to("src/test/java/com/ainotebuddy/app/search/SearchStub.kt")

    @Test
    fun quickfix_preserves_chain_split_regex_map_filter() {
        val src = kotlin(
            """
            package test
            
            fun demo(): List<String> {
                return "a b c".split(Regex("\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
            }
            """
        ).indented().to("src/test/java/test/ChainedSplitFix.kt")

        lint()
            .allowMissingSdk()
            .files(src, SEARCH_STUB)
            .issues(SplitWhitespaceDetector.ISSUE)
            .run()
            .expectFixDiffs(
                """
                Fix for src/test/java/test/ChainedSplitFix.kt line 4: Replace with WS_REGEX.split(…) and add import:
                @@
                -package test
                -
                -fun demo(): List<String> {
                -    return "a b c".split(Regex("\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
                -}
                +package test
                +
                +import com.ainotebuddy.app.search.WS_REGEX
                +
                +fun demo(): List<String> {
                +    return WS_REGEX.split("a b c").map { it.trim() }.filter { it.isNotEmpty() }
                +}
                """
            )
    }
}