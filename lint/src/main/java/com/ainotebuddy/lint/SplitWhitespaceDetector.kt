package com.ainotebuddy.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement

/**
 * Flags raw usages of String.split/Regex("…")/toRegex and Pattern.compile with whitespace regex
 * that bypass centralized WS_REGEX, and offers IDE quickfixes.
 */
class SplitWhitespaceDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> = listOf("split", "compile", "toPattern")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val methodName = method.name
        val inSearchPackage = context.uastFile?.packageName?.startsWith("com.ainotebuddy.app.search") == true
        val inAiPackage = context.uastFile?.packageName?.startsWith("com.ainotebuddy.app.ai") == true

        // Style-only: discourage cross-package WS_REGEX usage when inside domain packages
        node.receiver?.let { recv ->
            val recvSrc = recv.asSourceString()

            // Inside search package: prefer search.WS_REGEX
            val usesAiWsRegex = recvSrc.contains("ai.TextUtils.WS_REGEX") ||
                recvSrc.contains("com.ainotebuddy.app.ai.TextUtils.WS_REGEX")
            if (inSearchPackage && usesAiWsRegex) {
                val fix = LintFix.create()
                    .replace()
                    .range(context.getLocation(recv))
                    .with("com.ainotebuddy.app.search.WS_REGEX")
                    .shortenNames()
                    .reformat(true)
                    .name("Use search.WS_REGEX for consistency")
                    .autoFix(true, false)
                    .build()

                context.report(
                    ISSUE_SEARCH_PACKAGE_CONSISTENCY,
                    node as UElement,
                    context.getLocation(recv),
                    "Use com.ainotebuddy.app.search.WS_REGEX in search package for consistency.",
                    fix
                )
            }

            // Inside AI package: prefer ai.TextUtils.WS_REGEX
            val usesSearchWsRegex = recvSrc.contains("com.ainotebuddy.app.search.WS_REGEX") ||
                recvSrc.contains("search.WS_REGEX")
            if (inAiPackage && usesSearchWsRegex) {
                val fix = LintFix.create()
                    .replace()
                    .range(context.getLocation(recv))
                    .with("com.ainotebuddy.app.ai.TextUtils.WS_REGEX")
                    .shortenNames()
                    .reformat(true)
                    .name("Use ai.TextUtils.WS_REGEX for consistency")
                    .autoFix(true, false)
                    .build()

                context.report(
                    ISSUE_AI_PACKAGE_CONSISTENCY,
                    node as UElement,
                    context.getLocation(recv),
                    "Use com.ainotebuddy.app.ai.TextUtils.WS_REGEX in AI packages for consistency.",
                    fix
                )
            }
        }

        // 1) String.split("…") or split(Regex("…")) or split("…".toRegex())
        if (methodName == "split") {
            val receiverType = node.receiverType?.canonicalText ?: return
            if (!receiverType.equals("java.lang.String", ignoreCase = true)) return

            val args = node.valueArguments
            if (args.isEmpty()) return

            val arg0 = args[0]
            val argSource = arg0.asSourceString().trim()

            // Extract literal if string literal
            val literal = extractStringLiteral(argSource)

            val isRawWhitespaceSplit = when {
                // split("…") with literal
                literal != null && isBadWhitespaceLiteral(literal) -> true
                // split(Regex("…"))
                argSource.contains("Regex(") && extractRegexConstructorLiteral(argSource)?.let { isBadWhitespaceLiteral(it) } == true -> true
                // split("…".toRegex())
                argSource.contains("toRegex()") && extractToRegexLiteral(argSource)?.let { isBadWhitespaceLiteral(it) } == true -> true
                else -> false
            }

            if (isRawWhitespaceSplit) {
                val receiver = node.receiver?.asSourceString() ?: "this"
                val replacement = "com.ainotebuddy.app.search.WS_REGEX.split($receiver)"
                val fix: LintFix = LintFix.create()
                    .replace()
                    .range(context.getLocation(node))
                    .with(replacement)
                    .shortenNames()
                    .reformat(true)
                    .name("Replace with WS_REGEX.split(…) and add import")
                    .autoFix(true, false)
                    .build()

                context.report(
                    ISSUE,
                    node as UElement,
                    context.getLocation(node),
                    "Avoid raw split with whitespace regex. Use WS_REGEX from TextSearchUtils.",
                    fix
                )
            }
            return
        }

        // 2) Pattern.compile("…")
        if (methodName == "compile" && context.evaluator.isMemberInClass(method, "java.util.regex.Pattern")) {
            val args = node.valueArguments
            if (args.isEmpty()) return

            val firstArg = args[0]
            val raw = firstArg.asSourceString().trim()

            val literal = extractStringLiteral(raw) ?: return // only handle string literals

            if (isBadWhitespaceLiteral(literal)) {
                val replacement = "com.ainotebuddy.app.search.WS_REGEX.toPattern()"
                val fix: LintFix = LintFix.create()
                    .replace()
                    .range(context.getLocation(node))
                    .with(replacement)
                    .shortenNames()
                    .reformat(true)
                    .name("Replace with WS_REGEX.toPattern() and add import")
                    .autoFix(true, false)
                    .build()

                context.report(
                    ISSUE,
                    node as UElement,
                    context.getLocation(node),
                    "Avoid Pattern.compile with whitespace regex. Use WS_REGEX.toPattern().",
                    fix
                )
            }
            return
        }

        // 3) WS_REGEX.toPattern() style check in search package (receiver handled above)
        if (methodName == "toPattern") {
            // The style warning for ai.TextUtils.WS_REGEX is already reported via receiver block
            return
        }
    }

    private fun extractStringLiteral(argSource: String): String? {
        if (argSource.length >= 2 && argSource.first() == '"' && argSource.last() == '"') {
            return argSource.substring(1, argSource.length - 1)
        }
        return null
    }

    private fun extractRegexConstructorLiteral(source: String): String? {
        // Expect something like: Regex("…") possibly with spaces
        val start = source.indexOf("Regex(")
        if (start == -1) return null
        val open = source.indexOf('(', start)
        val close = source.lastIndexOf(')')
        if (open == -1 || close == -1 || close <= open + 1) return null
        val inner = source.substring(open + 1, close).trim()
        return extractStringLiteral(inner)
    }

    private fun extractToRegexLiteral(source: String): String? {
        // Expect something like: "…".toRegex()
        val dot = source.indexOf(".toRegex()")
        if (dot == -1) return null
        val prefix = source.substring(0, dot)
        return extractStringLiteral(prefix.trim())
    }

    private fun isBadWhitespaceLiteral(literal: String): Boolean {
        // Exact matches to avoid false positives in larger regexes
        return literal in BAD_WHITESPACE_LITERALS
    }

    companion object {
        private val BAD_WHITESPACE_LITERALS: Set<String> = setOf(
            "\\s+",      // one-or-more whitespace
            "\\s",       // single whitespace
            "\\p{Zs}+", // Unicode space separator class, one-or-more
            "\\p{Zs}",   // single Unicode space separator
            "[ \\t]+",   // class with space or tab, one-or-more
            "[\\t ]+"    // class with tab or space, one-or-more (alternate order)
        )

        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "RawWhitespaceSplit",
            briefDescription = "Raw whitespace regex usage detected",
            explanation = "Use centralized WS_REGEX for whitespace splitting/regex to avoid drift and maintain consistency across the codebase.",
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(
                SplitWhitespaceDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        ).setAndroidSpecific(true)

        @JvmField
        val ISSUE_SEARCH_PACKAGE_CONSISTENCY: Issue = Issue.create(
            id = "SearchPackageWsRegexConsistency",
            briefDescription = "Use search.WS_REGEX inside search package",
            explanation = "Within com.ainotebuddy.app.search, prefer com.ainotebuddy.app.search.WS_REGEX over aiTextUtils.WS_REGEX for consistency.",
            category = Category.CORRECTNESS,
            priority = 3,
            severity = Severity.WARNING,
            implementation = Implementation(
                SplitWhitespaceDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        ).setAndroidSpecific(true)

        @JvmField
        val ISSUE_AI_PACKAGE_CONSISTENCY: Issue = Issue.create(
            id = "AiPackageWsRegexConsistency",
            briefDescription = "Use aiTextUtils.WS_REGEX inside AI packages",
            explanation = "Within com.ainotebuddy.app.ai, prefer com.ainotebuddy.app.ai.TextUtils.WS_REGEX over com.ainotebuddy.app.search.WS_REGEX for consistency.",
            category = Category.CORRECTNESS,
            priority = 3,
            severity = Severity.WARNING,
            implementation = Implementation(
                SplitWhitespaceDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        ).setAndroidSpecific(true)
    }
}
