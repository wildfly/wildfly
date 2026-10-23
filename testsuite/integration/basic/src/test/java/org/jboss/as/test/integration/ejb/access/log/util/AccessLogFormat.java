package org.jboss.as.test.integration.ejb.access.log.util;

import java.util.regex.Pattern;

/**
 * Patterns for matching EJB access log lines.
 *
 * <p>The v1 format is always JSON produced by {@code JsonEventFormatter}. Every emitted record is
 * a single JSON object on one line. All enum constants that represent JSON output share the same
 * pattern; the legacy text-format constants use a non-matching pattern so that negative tests
 * (which assert no records appear) work correctly: a JSON record will never match a
 * space-delimited text pattern.
 */
public enum AccessLogFormat {

    /**
     * Plain text pattern – never emitted in v1; used only to confirm <em>absence</em> of records
     * in {@link org.jboss.as.test.integration.ejb.access.log.AccessLogNegativeTestCase}.
     */
    SHORT("short",
            Pattern.compile("\"([^\"]+)\"(?:\\s+(\\S+)){4}")),

    LONG("long",
            Pattern.compile("\"([^\"]+)\"(?:\\s+(\\S+)){6}")),

    CUSTOM("date time timezone ip user ejb method invocation event host port protocol thread server",
            Pattern.compile("\"([^\"]+)\"(?:\\s+(\\S+)){11}")),

    DEFAULT("default",
            Pattern.compile("\"([^\"]+)\"(?:\\s+(\\S+)){4}")),

    /**
     * JSON format – matches any single-line JSON object produced by {@code JsonEventFormatter}.
     * The line may carry an optional prefix (e.g. ANSI escape codes prepended by the console
     * handler, or a log-record header when routed through the logging subsystem).
     * All test cases that expect JSON records use one of these constants.
     */
    SHORT_JSON("short", Pattern.compile(".*\\{.*}")),

    LONG_JSON("long", Pattern.compile(".*\\{.*}")),

    CUSTOM_JSON("date time timezone ip user ejb method invocation event host port protocol thread server",
            Pattern.compile(".*\\{.*}")),

    DEFAULT_JSON("default", Pattern.compile(".*\\{.*}"))

    ;

    private final String pattern;
    private final Pattern regexp;

    AccessLogFormat(String pattern, Pattern regexp) {
        this.pattern = pattern;
        this.regexp = regexp;
    }

    public String getPattern() {
        return pattern;
    }

    public Pattern getRegexp() {
        return regexp;
    }
}
