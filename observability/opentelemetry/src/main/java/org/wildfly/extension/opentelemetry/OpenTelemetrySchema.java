package org.wildfly.extension.opentelemetry;

import java.util.Locale;

public enum OpenTelemetrySchema {
    VERSION_1_0(1, 0), // WildFly 25
    ;

    public static final OpenTelemetrySchema CURRENT = VERSION_1_0;


    private final int major;
    private final int minor;

    OpenTelemetrySchema(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public int major() {
        return this.major;
    }

    public int minor() {
        return this.minor;
    }

    public String getNamespaceUri() {
        return String.format(Locale.ROOT, "urn:wildfly:opentelemetry:%d.%d", this.major, this.minor);
    }
}
