/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals.logs;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses OpenTelemetry Collector text output into log records, preserving each resource block's attributes.
 */
public class CollectorLogRecordParser {
    // Pattern for parsing key-value fields (e.g., "Field: Value")
    private static final Pattern FIELD_PATTERN = Pattern.compile("^([A-Za-z]+[\\sA-Za-z]*):\\s*(.*)$");
    // Pattern for extracting the numerical severity number from text like "Info(9)"
    private static final Pattern SEVERITY_NUMBER_PATTERN = Pattern.compile("(\\d+)");
    // Pattern for parsing attributes like " -> bridge.name: Str(...)"
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("^\\s*->\\s*([^:]+):\\s*Str\\((.*)\\)$");
    // Formatter to parse the specific date format: 2025-11-26 22:23:39.754885 +0000 UTC
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS Z");

    /**
     * Parses collector output and associates each log record with its enclosing resource attributes.
     *
     * @param lines the collector output lines
     * @return the parsed log records in input order
     */
    public List<OpenTelemetryLogRecord> retrieveLogRecords(String[] lines) {
        // Strip unneeded metadata from the start of each line
        List<String> cleaned = Arrays.stream(lines).map(line ->
                        line.replace("[OpenTelemetryCollector] ", "")
                                .replaceAll("\\d{4}-\\d{2}-\\d{2}T.*Z\t", "")
                                .trim())
                .toList();
        List<OpenTelemetryLogRecord> logRecords = new ArrayList<>();
        Map<String, String> resourceAttributes = new HashMap<>();
        List<String> logLines = null;
        boolean inResourceAttributes = false;

        for (String line : cleaned) {
            if (line.startsWith("ResourceLogs")) {
                if (logLines != null) {
                    logRecords.add(buildLogRecord(logLines, resourceAttributes));
                    logLines = null;
                }
                resourceAttributes = new HashMap<>();
                inResourceAttributes = false;
                continue;
            }

            if (line.startsWith("Resource attributes:")) {
                inResourceAttributes = true;
                continue;
            }

            if (inResourceAttributes) {
                if (line.startsWith("->")) {
                    parseAttribute(line, resourceAttributes);
                    continue;
                }
                inResourceAttributes = false;
            }

            if (line.startsWith("LogRecord")) {
                if (logLines != null) {
                    logRecords.add(buildLogRecord(logLines, resourceAttributes));
                }
                logLines = new ArrayList<>();
                logLines.add(line);
            } else if (line.startsWith("{\"resource\":")) {
                if (logLines != null) {
                    logRecords.add(buildLogRecord(logLines, resourceAttributes));
                    logLines = null;
                }
            } else if (logLines != null) {
                logLines.add(line);
            }
        }

        if (logLines != null) {
            logRecords.add(buildLogRecord(logLines, resourceAttributes));
        }

        return logRecords;
    }

    /**
     * Parses the List<String> log entry into an OpenTelemetryLogRecord instance.
     *
     * @param logLines The list of strings containing the log record data.
     * @param resourceAttributes the attributes from the enclosing resource block
     * @return A fully populated OpenTelemetryLogRecord.
     */
    private OpenTelemetryLogRecord buildLogRecord(List<String> logLines, Map<String, String> resourceAttributes) {
        Map<String, String> parsedFields = new HashMap<>();
        Map<String, String> attributes = new HashMap<>();

        boolean inAttributesSection = false;

        for (String line : logLines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("LogRecord #")) {
                continue;
            }

            // 1. Detect the start of the Attributes section
            if (trimmedLine.startsWith("Attributes:")) {
                inAttributesSection = true;
                continue;
            }

            if (inAttributesSection) {
                // 2. Parse attributes until a new top-level field is detected
                if (trimmedLine.startsWith("->")) {
                    parseAttribute(trimmedLine, attributes);
                } else {
                    // This line is no longer an attribute, check if it's a new top-level field
                    inAttributesSection = false;
                }
            }

            // 3. Parse top-level fields
            if (!inAttributesSection) {
                Matcher fieldMatcher = FIELD_PATTERN.matcher(trimmedLine);
                if (fieldMatcher.matches()) {
                    parsedFields.put(fieldMatcher.group(1).trim(), fieldMatcher.group(2).trim());
                }
            }
        }

        // Extract severity number from "Info(9)" or similar
        int severityNumber = 0;
        Matcher severityMatcher = SEVERITY_NUMBER_PATTERN.matcher(
                parsedFields.getOrDefault("SeverityNumber", "0"));
        if (severityMatcher.find()) {
            try {
                severityNumber = Integer.parseInt(severityMatcher.group(1));
            } catch (NumberFormatException e) {
                // Keep default of 0
            }
        }

        // Build the final record instance
        return new OpenTelemetryLogRecord(
                parseDateToUnixNano(parsedFields.getOrDefault("Timestamp", "")),
                parseDateToUnixNano(parsedFields.getOrDefault("ObservedTimestamp", "")),
                severityNumber,
                parsedFields.getOrDefault("SeverityText", ""),
                parsedFields.getOrDefault("Body", "Str()")
                        .replaceAll("^Str\\(|\\)$", ""), // Removes "Str(" and ")"
                resourceAttributes,
                attributes,
                Integer.parseInt(parsedFields.getOrDefault("Flags", "0")), // Flags is simple int
                parsedFields.getOrDefault("Trace ID", ""),
                parsedFields.getOrDefault("Span ID", "")
        );
    }

    /**
     * Adds a collector-formatted string attribute to the target map when the line is valid.
     *
     * @param line the collector attribute line
     * @param attributes the target attribute map
     */
    private void parseAttribute(String line, Map<String, String> attributes) {
        Matcher attributeMatcher = ATTRIBUTE_PATTERN.matcher(line);
        if (attributeMatcher.matches()) {
            attributes.put(attributeMatcher.group(1).trim(), attributeMatcher.group(2).trim());
        }
    }

    /**
     * Parses the date string (e.g., "2025-11-26 22:23:39.755362 +0000 UTC")
     * into a Unix timestamp in nanoseconds as a String.
     *
     * @param dateString The observed date string.
     * @return The Unix time in nanoseconds as a String.
     */
    private String parseDateToUnixNano(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return "0"; // Default to 0 if missing
        }
        try {
            // Remove the ' UTC' suffix as it's handled by the 'Z' pattern
            OffsetDateTime odt = OffsetDateTime.parse(dateString.replace(" UTC", "").trim(), DATE_FORMATTER);

            // Calculate nanoseconds: seconds * 10^9 + nanoseconds part of the second
            long nanoSeconds = odt.toInstant().getEpochSecond() * 1_000_000_000L + odt.toInstant().getNano();

            return String.valueOf(nanoSeconds);
        } catch (Exception e) {
            System.err.println("Error parsing date '" + dateString + "'. Returning 0. Error: " + e.getMessage());
            return "0";
        }
    }
}
