package com.ainotebuddy.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import kotlin.test.Test

class SplitWhitespaceDetectorTest {

    private val SEARCH_STUB = kotlin(
        """
        package com.ainotebuddy.app.search
        
        val WS_REGEX: Regex = Regex("\\s+")
        """
    ).indented()

    private val AI_STUB = kotlin(
        """
        package com.ainotebuddy.app.ai
        
        object TextUtils {
            @JvmField
            val WS_REGEX: Regex = Regex("\\s+")
        }
        """
    ).indented()

    @Test
    fun rawSplit_literal_error() {
        val src = kotlin(
            """
            package test
            
            fun demo() {
                val parts = "a b".split("\\s+")
                println(parts)
            }
            """
        ).indented()

        lint()
            .allowMissingSdk()
            .files(src, SEARCH_STUB, AI_STUB)
            .issues(SplitWhitespaceDetector.ISSUE)
            .run()
            .expectErrorCount(1)
    }

    @Test
    fun rawPatternCompile_error() {
        val src = kotlin(
            """
            package test
            
            import java.util.regex.Pattern
            
            fun demo() {
                val p = Pattern.compile("[ \\t]+")
                println(p)
            }
            """
        ).indented()

        lint()
            .allowMissingSdk()
            .files(src, SEARCH_STUB, AI_STUB)
            .issues(SplitWhitespaceDetector.ISSUE)
            .run()
            .expectErrorCount(1)
    }

    @Test
    fun searchPackage_usesAi_warning() {
        val src = kotlin(
            """
            package com.ainotebuddy.app.search.feature
            
            import com.ainotebuddy.app.ai.TextUtils
            
            fun demo() {
                val p = TextUtils.WS_REGEX.toPattern()
                println(p)
            }
            """
        ).indented()

        lint()
            .allowMissingSdk()
            .files(src, SEARCH_STUB, AI_STUB)
            .issues(
                SplitWhitespaceDetector.ISSUE,
                SplitWhitespaceDetector.ISSUE_SEARCH_PACKAGE_CONSISTENCY,
            )
            .run()
            .expectWarningCount(1)
    }

    @Test
    fun aiPackage_usesSearch_warning() {
        val src = kotlin(
            """
            package com.ainotebuddy.app.ai.feature
            
            import com.ainotebuddy.app.search.WS_REGEX
            
            fun demo() {
                val p = WS_REGEX.toPattern()
                println(p)
            }
            """
        ).indented()

        lint()
            .allowMissingSdk()
            .files(src, SEARCH_STUB, AI_STUB)
            .issues(
                SplitWhitespaceDetector.ISSUE,
                SplitWhitespaceDetector.ISSUE_AI_PACKAGE_CONSISTENCY,
            )
            .run()
            .expectWarningCount(1)
    }

    @Test
    fun outsidePackages_eitherConstant_allowed() {
        val src1 = kotlin(
            """
            package test
            
            import com.ainotebuddy.app.search.WS_REGEX
            
            fun demo1() {
                val p = WS_REGEX.toPattern()
            }
            """
        ).indented()
        val src2 = kotlin(
            """
            package test
            
            import com.ainotebuddy.app.ai.TextUtils
            
            fun demo2() {
                val p = TextUtils.WS_REGEX.toPattern()
            }
            """
        ).indented()

        lint()
            .allowMissingSdk()
            .files(src1, src2, SEARCH_STUB, AI_STUB)
            .issues(
                SplitWhitespaceDetector.ISSUE,
                SplitWhitespaceDetector.ISSUE_SEARCH_PACKAGE_CONSISTENCY,
                SplitWhitespaceDetector.ISSUE_AI_PACKAGE_CONSISTENCY,
            )
            .run()
            .expectClean()
    }

    @Test
    fun multipleBadPatterns_detected() {
        val src = kotlin(
            """
            package test
            
            import java.util.regex.Pattern
            
            fun demo() {
                val a = "x y".split("\\s")
                val b = "x y".split(Regex("\\p{Zs}+"))
                val c = "x\ty".split("[ \\t]+".toRegex())
                val d = Pattern.compile("\\s+")
                println(a); println(b); println(c); println(d)
            }
            """
        ).indented()

        lint()
            .allowMissingSdk()
            .files(src, SEARCH_STUB, AI_STUB)
            .issues(SplitWhitespaceDetector.ISSUE)
            .run()
            .expectErrorCount(4)
    }

    @Test
    fun quickFix_presence_for_split_and_pattern() {
        val src1 = kotlin(
            """
            package test
            
            fun demo1() {
                val parts = "a b".split("\\s+")
            }
            """
        ).indented()
        val src2 = kotlin(
            """
            package test
            
            import java.util.regex.Pattern
            
            fun demo2() {
                val p = Pattern.compile("\\s+")
            }
            """
        ).indented()

        lint()
            .allowMissingSdk()
            .files(src1, src2, SEARCH_STUB, AI_STUB)
            .issues(SplitWhitespaceDetector.ISSUE)
            .run()
            .expectErrorCount(2)
            .expectFixes().let { /* Ensure fixes are offered; detailed diff assertions can be added if needed */ }
    }
}