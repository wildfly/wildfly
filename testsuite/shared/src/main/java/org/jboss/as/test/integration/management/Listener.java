package org.jboss.as.test.integration.management;

/**
 * Basic connectors
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
public enum Listener {

    HTTP("http", "http", false),
    HTTPS("http", "https", true),
    AJP("ajp", "http", false);

    private final String name;
    private final String scheme;
    private final boolean secure;

    private Listener(String name, String scheme, boolean secure) {
        this.name = name;
        this.scheme = scheme;
        this.secure = secure;
    }

    public final String getName() {
        return name;
    }

    public final String getScheme() {
        return scheme;
    }

    public final boolean isSecure() {
        return secure;
    }

}
