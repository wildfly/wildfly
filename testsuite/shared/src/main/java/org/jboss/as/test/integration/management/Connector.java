package org.jboss.as.test.integration.management;

/**
 * Basic connectors
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
public enum Connector {

    HTTP("http", "http", "HTTP/1.1", false),
    HTTPS("http", "https", "HTTP/1.1", true),
    AJP("ajp", "http", "AJP/1.3", false),
    HTTPJIO("http", "http", "org.apache.coyote.http11.Http11Protocol", false),
    HTTPSJIO("http", "https", "org.apache.coyote.http11.Http11Protocol", true),
    AJPJIO("ajp", "http", "org.apache.coyote.ajp.AjpProtocol", false),
    HTTPNATIVE("http", "http", "org.apache.coyote.http11.Http11AprProtocol", false),
    HTTPSNATIVE("http","https", "org.apache.coyote.http11.Http11AprProtocol", true);

    private final String name;
    private final String scheme;
    private final String protocol;
    private final boolean secure;

    private Connector(String name, String scheme, String protocol, boolean secure) {
        this.name = name;
        this.scheme = scheme;
        this.protocol = protocol;
        this.secure = secure;
    }

    public final String getName() {
        return name;
    }

    public final String getScheme() {
        return scheme;
    }

    public final String getProtrocol() {
        return protocol;
    }

    public final boolean isSecure() {
        return secure;
    }

}
