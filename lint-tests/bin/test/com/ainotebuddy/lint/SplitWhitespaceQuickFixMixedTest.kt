package com.ainotebuddy.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import kotlin.test.Test

class SplitWhitespaceQuickFixMixedTest {

    private val SEARCH_STUB = kotlin(
        """
        package com.ainotebuddy.app.search
        
        val WS_REGEX: Regex = Regex("\\s+")
        """
    ).indented().to("src/test/java/com/ainotebuddy/app/search/SearchStub.kt")

    @Test
    fun quickfix_split_with_Regex_constructor() {
        val src = kotlin(
            """
            package test
            
            fun demo() {
                val parts = "a b".split(Regex("\\s+"))
            }
            """
        ).indented().to("src/test/java/test/SplitRegexCtorFix.kt")

        lint()
            .allowMissingSdk()
            .files(src, SEARCH_STUB)
            .issues(SplitWhitespaceDetector.ISSUE)
            .run()
            .expectFixDiffs(
                """
                Fix for src/test/java/test/SplitRegexCtorFix.kt line 4: Replace with WS_REGEX.split(…) and add import:
                @@
                -package test
                -
                -fun demo() {
                -    val parts = "a b".split(Regex("\\s+"))
                -}
                +package test
                +
                +import com.ainotebuddy.app.search.WS_REGEX
                +
                +fun demo() {
                +    val parts = WS_REGEX.split("a b")
                +}
                """
            )
    }

    @Test
    fun quickfix_split_with_toRegex_chain_and_existing_imports_ordering() {
        val src = kotlin(
            """
            package test
            
            import android.util.Log
            import java.util.Locale
            
            fun demo() {
                val parts = "a b".split("\\s+".toRegex())
                Log.d("x", parts.toString())
            }
            """
        ).indented().to("src/test/java/test/SplitToRegexFix.kt")

        lint()
            .allowMissingSdk()
            .files(src, SEARCH_STUB)
            .issues(SplitWhitespaceDetector.ISSUE)
            .run()
            .expectFixDiffs(
                """
                Fix for src/test/java/test/SplitToRegexFix.kt line 7: Replace with WS_REGEX.split(…) and add import:
                @@
                -package test
                -
                -import android.util.Log
                -import java.util.Locale
                -
                -fun demo() {
                -    val parts = "a b".split("\\s+".toRegex())
                -    Log.d("x", parts.toString())
                -}
                +package test
                +
                +import android.util.Log
                +import com.ainotebuddy.app.search.WS_REGEX
                +import java.util.Locale
                +
                +fun demo() {
                +    val parts = WS_REGEX.split("a b")
                +    Log.d("x", parts.toString())
                +}
                """
            )
    }
}