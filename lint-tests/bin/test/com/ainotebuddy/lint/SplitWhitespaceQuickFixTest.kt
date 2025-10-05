package com.ainotebuddy.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import kotlin.test.Test

class SplitWhitespaceQuickFixTest {

    private val SEARCH_STUB = kotlin(
        """
        package com.ainotebuddy.app.search
        
        val WS_REGEX: Regex = Regex("\\s+")
        """
    ).indented().to("src/test/java/com/ainotebuddy/app/search/SearchStub.kt")

    private val AI_STUB = kotlin(
        """
        package com.ainotebuddy.app.ai
        
        object TextUtils {
            @JvmField
            val WS_REGEX: Regex = Regex("\\s+")
        }
        """
    ).indented().to("src/test/java/com/ainotebuddy/app/ai/AiStub.kt")

    @Test
    fun quickfix_split_literal_to_WS_REGEX_split() {
        val src = kotlin(
            """
            package test
            
            fun demo() {
                val parts = "a b".split("\\s+")
            }
            """
        ).indented().to("src/test/java/test/SplitFix.kt")

        lint()
            .allowMissingSdk()
            .files(src, SEARCH_STUB, AI_STUB)
            .issues(SplitWhitespaceDetector.ISSUE)
            .run()
            .expectFixDiffs(
                """
                Fix for src/test/java/test/SplitFix.kt line 4: Replace with WS_REGEX.split(…) and add import:
                @@
                -package test
                -
                -fun demo() {
                -    val parts = "a b".split("\\s+")
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
    fun quickfix_pattern_compile_in_search_to_WS_REGEX_toPattern() {
        val src = kotlin(
            """
            package com.ainotebuddy.app.search.feature
            
            import java.util.regex.Pattern
            
            fun demo() {
                val p = Pattern.compile("[ \\t]+")
            }
            """
        ).indented().to("src/test/java/com/ainotebuddy/app/search/feature/PatternFix.kt")

        lint()
            .allowMissingSdk()
            .files(src, SEARCH_STUB, AI_STUB)
            .issues(SplitWhitespaceDetector.ISSUE)
            .run()
            .expectFixDiffs(
                """
                Fix for src/test/java/com/ainotebuddy/app/search/feature/PatternFix.kt line 6: Replace with WS_REGEX.toPattern() and add import:
                @@
                -package com.ainotebuddy.app.search.feature
                -
                -import java.util.regex.Pattern
                -
                -fun demo() {
                -    val p = Pattern.compile("[ \\t]+")
                -}
                +package com.ainotebuddy.app.search.feature
                +
                +import com.ainotebuddy.app.search.WS_REGEX
                +import java.util.regex.Pattern
                +
                +fun demo() {
                +    val p = WS_REGEX.toPattern()
                +}
                """
            )
    }

    @Test
    fun quickfix_ai_package_uses_search_replace_with_TextUtils_WS_REGEX() {
        val src = kotlin(
            """
            package com.ainotebuddy.app.ai.feature
            
            import com.ainotebuddy.app.search.WS_REGEX
            
            fun demo() {
                val p = WS_REGEX.toPattern()
            }
            """
        ).indented().to("src/test/java/com/ainotebuddy/app/ai/feature/AiConsistencyFix.kt")

        lint()
            .allowMissingSdk()
            .files(src, SEARCH_STUB, AI_STUB)
            .issues(SplitWhitespaceDetector.ISSUE_AI_PACKAGE_CONSISTENCY)
            .run()
            .expectFixDiffs(
                """
                Fix for src/test/java/com/ainotebuddy/app/ai/feature/AiConsistencyFix.kt line 6: Use ai.TextUtils.WS_REGEX for consistency:
                @@
                -package com.ainotebuddy.app.ai.feature
                -
                -import com.ainotebuddy.app.search.WS_REGEX
                -
                -fun demo() {
                -    val p = WS_REGEX.toPattern()
                -}
                +package com.ainotebuddy.app.ai.feature
                +
                +import com.ainotebuddy.app.ai.TextUtils
                +
                +fun demo() {
                +    val p = TextUtils.WS_REGEX.toPattern()
                +}
                """
            )
    }

    @Test
    fun quickfix_search_package_uses_ai_replace_with_search_WS_REGEX() {
        val src = kotlin(
            """
            package com.ainotebuddy.app.search.feature
            
            import com.ainotebuddy.app.ai.TextUtils
            
            fun demo() {
                val p = TextUtils.WS_REGEX.toPattern()
            }
            """
        ).indented().to("src/test/java/com/ainotebuddy/app/search/feature/SearchConsistencyFix.kt")

        lint()
            .allowMissingSdk()
            .files(src, SEARCH_STUB, AI_STUB)
            .issues(SplitWhitespaceDetector.ISSUE_SEARCH_PACKAGE_CONSISTENCY)
            .run()
            .expectFixDiffs(
                """
                Fix for src/test/java/com/ainotebuddy/app/search/feature/SearchConsistencyFix.kt line 6: Use search.WS_REGEX for consistency:
                @@
                -package com.ainotebuddy.app.search.feature
                -
                -import com.ainotebuddy.app.ai.TextUtils
                -
                -fun demo() {
                -    val p = TextUtils.WS_REGEX.toPattern()
                -}
                +package com.ainotebuddy.app.search.feature
                +
                +import com.ainotebuddy.app.search.WS_REGEX
                +
                +fun demo() {
                +    val p = WS_REGEX.toPattern()
                +}
                """
            )
    }
}