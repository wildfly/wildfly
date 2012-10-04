package org.jboss.as.mail.extension;

import java.util.Map;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
final class CustomServerConfig extends ServerConfig {
    private String protocol;

    public CustomServerConfig(final String protocol, final String socketBinding, Credentials credentials, boolean ssl, boolean tls, Map<String, String> properties) {
        super(socketBinding, credentials, ssl, tls, properties);
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }
}
