package ch.admin.bit.jeap.messaging.kafka.errorhandling;


import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class StackTraceParser {

    private static final Pattern STACK_TRACE_LINE_PATTERN = Pattern.compile(
            "\\s*+at\\s++(?<coderef>[^(]++)\\((?<source>[^)]*+)\\)\\s*+");

    private static final Pattern SOURCE_PATTERN = Pattern.compile(
            "(?<filename>[^:]++):(?<linenumber>\\d++)");

    static StackTraceElement[] parseStackTrace(String stackTrace) {
        return stackTrace.lines().
                map(StackTraceParser::parseStackTraceLine).
                filter(Objects::nonNull).
                toArray(StackTraceElement[]::new);
    }

    private static StackTraceElement parseStackTraceLine(String line) {
        Matcher lineMatcher = STACK_TRACE_LINE_PATTERN.matcher(line);
        if (lineMatcher.matches()) {
            String codeRef = lineMatcher.group("coderef");
            int methodSplit = codeRef.lastIndexOf(".");
            if (methodSplit > 0) {
                String methodName = codeRef.substring(methodSplit + 1);
                String fqcn = codeRef.substring(0, methodSplit);
                String source = lineMatcher.group("source");
                Matcher sourceMatcher = SOURCE_PATTERN.matcher(source);
                if (sourceMatcher.matches()) {
                    String filename = sourceMatcher.group("filename");
                    int lineNumber = Integer.parseInt(sourceMatcher.group("linenumber"));
                    return new StackTraceElement(fqcn, methodName, filename, lineNumber);
                } else {
                    return new StackTraceElement(fqcn, methodName, null, -1);
                }
            }
        }
        // not a stack trace line
        return null;
    }

}
