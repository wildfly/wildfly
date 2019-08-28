package org.jboss.as.test.integration.ejb.access.log.util;

import java.util.regex.Pattern;

public enum AccessLogFormat {

    // date time ip user ejb method
    SHORT(
            "short",
            Pattern.compile("\"([^\"]+)\"(?:\\s+(\\S+)){4}")
    ),
    // date time ip user ejb method invocation event
    LONG(
            "long",
            Pattern.compile("\"([^\"]+)\"(?:\\s+(\\S+)){6}")
    ),
    CUSTOM(
            "date time timezone ip user ejb method invocation event host port protocol thread server",
            Pattern.compile("\"([^\"]+)\"(?:\\s+(\\S+)){11}")
    ),
    // same as SHORT
    DEFAULT(
            "default",
            Pattern.compile("\"([^\"]+)\"(?:\\s+(\\S+)){4}")
    ),

    // date time ip user ejb method
    SHORT_JSON(
            "short",
            Pattern.compile(".*?(\\{(?:\\s?\"?(date|time|ip|user|ejb|method)\"?\\s?:\\s?\"?([^\"]+?)\"?\\s?[,}]){6})")
    ),
    // date time ip user ejb method invocation event
    LONG_JSON(
            "long",
            Pattern.compile(".*?(\\{(?:\\s?\"?(date|time|ip|user|ejb|method|invocation|event)\"?\\s?:\\s?\"?([^\"]+?)\"?\\s?[,}]){8})")
    ),
    CUSTOM_JSON(
            "date time timezone ip user ejb method invocation event host port protocol thread server",
            Pattern.compile(".*?(\\{(?:\\s?\"?(date|time|timezone|ip|user|ejb|method|invocation|event|host|port|protocol|thread|server)\"?\\s?:\\s?\"?([^\"]+?)\"?\\s?[,}]){14})")
    ),
    // same as SHORT
    DEFAULT_JSON(
            "default",
            Pattern.compile(".*?(\\{(?:\\s?\"?(date|time|ip|user|ejb|method)\"?\\s?:\\s?\"?([^\"]+?)\"?\\s?[,}]){6})")
    )

    ;

    private String pattern;
    private Pattern regexp;

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
