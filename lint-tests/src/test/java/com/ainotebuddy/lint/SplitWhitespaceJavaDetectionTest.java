package com.ainotebuddy.lint;

import com.android.tools.lint.checks.infrastructure.TestFiles;
import com.android.tools.lint.checks.infrastructure.TestLintTask;
import org.junit.Test;

public class SplitWhitespaceJavaDetectionTest {

    @Test
    public void rawPatternCompile_error_java() {
        var searchStub = TestFiles.kotlin(
            """
            package com.ainotebuddy.app.search
            
            val WS_REGEX: Regex = Regex("\\s+")
            """
        ).indented().to("src/test/java/com/ainotebuddy/app/search/SearchStub.kt");

        var src = TestFiles.java(
            """
            package test;
            
            import java.util.regex.Pattern;
            
            public class J {
                public void demo() {
                    var p = Pattern.compile("[ \t]+");
                    System.out.println(p);
                }
            }
            """
        ).indented().to("src/test/java/test/J.java");

        TestLintTask.lint()
            .allowMissingSdk()
            .files(src, searchStub)
            .issues(com.ainotebuddy.lint.SplitWhitespaceDetector.ISSUE)
            .run()
            .expectErrorCount(1);
    }
}