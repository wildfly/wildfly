package org.jboss.as.mail.extension;

import java.util.Map;

/**
 * @author Tomaz Cerar
 * @created 10.8.11 22:50
 */
class ServerConfig {
    private final String outgoingSocketBinding;
    private final Credentials credentials;
    private boolean sslEnabled = false;
    private boolean tlsEnabled = false;
    private final Map<String, String> properties;

    public ServerConfig(final String outgoingSocketBinding, final Credentials credentials, boolean ssl, boolean tls, Map<String, String> properties) {
        this.outgoingSocketBinding = outgoingSocketBinding;
        this.credentials = credentials;
        this.sslEnabled = ssl;
        this.tlsEnabled = tls;
        this.properties = properties;
    }

    public String getOutgoingSocketBinding() {
        return outgoingSocketBinding;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
