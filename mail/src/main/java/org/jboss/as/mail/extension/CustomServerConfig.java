/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import java.util.Map;
import java.util.function.Supplier;

import org.jboss.as.network.OutboundSocketBinding;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
final class CustomServerConfig extends ServerConfig {
    private String protocol;

    public CustomServerConfig(final String protocol, final Supplier<OutboundSocketBinding> socketBinding, Credentials credentials, boolean ssl, boolean tls, Map<String, String> properties) {
        super(socketBinding, credentials, ssl, tls, properties);
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
    }
}
