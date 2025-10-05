package com.ainotebuddy.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import kotlin.test.Test

class SplitWhitespaceVariantsQuickFixTest {

    private val SEARCH_STUB = kotlin(
        """
        package com.ainotebuddy.app.search
        
        val WS_REGEX: Regex = Regex("\\s+")
        """
    ).indented().to("src/test/java/com/ainotebuddy/app/search/SearchStub.kt")

    @Test
    fun quickfix_split_literal_single_s() {
        val src = kotlin(
            """
            package test
            
            fun demo() {
                val parts = "a b".split("\\s")
            }
            """
        ).indented().to("src/test/java/test/VariantSplitSingleS.kt")

        lint()
            .allowMissingSdk()
            .files(src, SEARCH_STUB)
            .issues(SplitWhitespaceDetector.ISSUE)
            .run()
            .expectFixDiffs(
                """
                Fix for src/test/java/test/VariantSplitSingleS.kt line 4: Replace with WS_REGEX.split(…) and add import:
                @@
                -package test
                -
                -fun demo() {
                -    val parts = "a b".split("\\s")
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
    fun quickfix_split_literal_unicode_space_separator_plus() {
        val src = kotlin(
            """
            package test
            
            fun demo() {
                val parts = "a b".split("\\p{Zs}+")
            }
            """
        ).indented().to("src/test/java/test/VariantSplitUnicodeZs.kt")

        lint()
            .allowMissingSdk()
            .files(src, SEARCH_STUB)
            .issues(SplitWhitespaceDetector.ISSUE)
            .run()
            .expectFixDiffs(
                """
                Fix for src/test/java/test/VariantSplitUnicodeZs.kt line 4: Replace with WS_REGEX.split(…) and add import:
                @@
                -package test
                -
                -fun demo() {
                -    val parts = "a b".split("\\p{Zs}+")
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
    fun quickfix_split_char_class_tab_space_toRegex() {
        val src = kotlin(
            """
            package test
            
            fun demo() {
                val parts = "a b".split("[\\t ]+".toRegex())
            }
            """
        ).indented().to("src/test/java/test/VariantSplitCharClassToRegex.kt")

        lint()
            .allowMissingSdk()
            .files(src, SEARCH_STUB)
            .issues(SplitWhitespaceDetector.ISSUE)
            .run()
            .expectFixDiffs(
                """
                Fix for src/test/java/test/VariantSplitCharClassToRegex.kt line 4: Replace with WS_REGEX.split(…) and add import:
                @@
                -package test
                -
                -fun demo() {
                -    val parts = "a b".split("[\\t ]+".toRegex())
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
}