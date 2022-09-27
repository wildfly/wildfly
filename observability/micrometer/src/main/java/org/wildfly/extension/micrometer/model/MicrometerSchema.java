package org.wildfly.extension.micrometer.model;

import java.util.Locale;

public enum MicrometerSchema {
    VERSION_1_0(1, 0), // WildFly 26
    ;

    public static final MicrometerSchema CURRENT = VERSION_1_0;


    private final int major;
    private final int minor;

    MicrometerSchema(int major, int minor) {
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
        return String.format(Locale.ROOT, "urn:wildfly:micrometer:%d.%d", this.major, this.minor);
    }
}
