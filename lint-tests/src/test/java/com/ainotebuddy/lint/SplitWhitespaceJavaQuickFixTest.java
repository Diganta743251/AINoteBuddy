package com.ainotebuddy.lint;

import static com.android.tools.lint.checks.infrastructure.TestFiles.java;
import static com.android.tools.lint.checks.infrastructure.TestFiles.kotlin;
import static com.android.tools.lint.checks.infrastructure.TestLintTask.lint;

import org.junit.Test;

public class SplitWhitespaceJavaQuickFixTest {

    @Test
    public void quickfix_basic_pattern_compile_java() {
        var searchStub = kotlin(
            """
            package com.ainotebuddy.app.search
            
            val WS_REGEX: Regex = Regex("\\s+")
            """
        ).indented().to("src/test/java/com/ainotebuddy/app/search/SearchStub.kt");

        var src = java(
            """
            package test;
            
            import java.util.regex.Pattern;
            
            class J1 {
                void demo() {
                    var p = Pattern.compile("\\s+");
                    System.out.println(p);
                }
            }
            """
        ).indented().to("src/test/java/test/J1.java");

        lint()
            .allowMissingSdk()
            .files(src, searchStub)
            .issues(com.ainotebuddy.lint.SplitWhitespaceDetector.ISSUE)
            .run()
            .expectFixDiffs(
                """
                Fix for src/test/java/test/J1.java line 7: Replace with WS_REGEX.toPattern() and add import:
                @@
                -package test;
                -
                -import java.util.regex.Pattern;
                -
                -class J1 {
                -    void demo() {
                -        var p = Pattern.compile("\\s+");
                -        System.out.println(p);
                -    }
                -}
                +package test;
                +
                +import java.util.regex.Pattern;
                +
                +class J1 {
                +    void demo() {
                +        var p = com.ainotebuddy.app.search.WS_REGEX.toPattern();
                +        System.out.println(p);
                +    }
                +}
                """
            );
    }

    @Test
    public void quickfix_chained_pattern_usage_java() {
        var searchStub = kotlin(
            """
            package com.ainotebuddy.app.search
            
            val WS_REGEX: Regex = Regex("\\s+")
            """
        ).indented().to("src/test/java/com/ainotebuddy/app/search/SearchStub.kt");

        var src = java(
            """
            package test;
            
            import java.util.regex.Pattern;
            
            class J2 {
                boolean isWs(String text) {
                    return Pattern.compile("[ \\t]+").matcher(text).matches();
                }
            }
            """
        ).indented().to("src/test/java/test/J2.java");

        lint()
            .allowMissingSdk()
            .files(src, searchStub)
            .issues(com.ainotebuddy.lint.SplitWhitespaceDetector.ISSUE)
            .run()
            .expectFixDiffs(
                """
                Fix for src/test/java/test/J2.java line 7: Replace with WS_REGEX.toPattern() and add import:
                @@
                -package test;
                -
                -import java.util.regex.Pattern;
                -
                -class J2 {
                -    boolean isWs(String text) {
                -        return Pattern.compile("[ \\t]+").matcher(text).matches();
                -    }
                -}
                +package test;
                +
                +import java.util.regex.Pattern;
                +
                +class J2 {
                +    boolean isWs(String text) {
                +        return com.ainotebuddy.app.search.WS_REGEX.toPattern().matcher(text).matches();
                +    }
                +}
                """
            );
    }

    @Test
    public void quickfix_same_file_no_import_needed_java() {
        // Provide a same-package Kotlin stub so the FQ reference is still valid; fix should remain surgical
        var searchStub = kotlin(
            """
            package com.ainotebuddy.app.search
            
            val WS_REGEX: Regex = Regex("\\s+")
            """
        ).indented().to("src/test/java/com/ainotebuddy/app/search/SearchStub.kt");

        var src = java(
            """
            package com.ainotebuddy.app.search;
            
            import java.util.regex.Pattern;
            
            class J3 {
                Pattern provide() {
                    return Pattern.compile("\\s+");
                }
            }
            """
        ).indented().to("src/test/java/com/ainotebuddy/app/search/J3.java");

        lint()
            .allowMissingSdk()
            .files(src, searchStub)
            .issues(com.ainotebuddy.lint.SplitWhitespaceDetector.ISSUE)
            .run()
            .expectFixDiffs(
                """
                Fix for src/test/java/com/ainotebuddy/app/search/J3.java line 7: Replace with WS_REGEX.toPattern() and add import:
                @@
                -package com.ainotebuddy.app.search;
                -
                -import java.util.regex.Pattern;
                -
                -class J3 {
                -    Pattern provide() {
                -        return Pattern.compile("\\s+");
                -    }
                -}
                +package com.ainotebuddy.app.search;
                +
                +import java.util.regex.Pattern;
                +
                +class J3 {
                +    Pattern provide() {
                +        return com.ainotebuddy.app.search.WS_REGEX.toPattern();
                +    }
                +}
                """
            );
    }
}