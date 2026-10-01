package org.jboss.as.test.integration.ejb.access.log.util;

/**
 * Holder for a single EJB access log line.
 *
 * <p>In v1 all records are emitted as JSON by {@code JsonEventFormatter}. The raw line may
 * carry a prefix (ANSI escape codes from the console handler, or a log-record header when
 * routed through the logging subsystem). {@link #getLine()} strips everything before the
 * first {@code {} so that the result can be parsed as JSON.
 */
public class AccessLog {
    private final String line;

    public AccessLog(String line) {
        this.line = line;
    }

    /**
     * Returns the JSON portion of the access log line — everything from the first '{' onwards.
     */
    public String getLine() {
        int idx = line.indexOf('{');
        return idx >= 0 ? line.substring(idx) : line;
    }
}
