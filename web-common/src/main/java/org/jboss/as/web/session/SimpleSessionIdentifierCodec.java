/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.web.session;

/**
 * Simple codec implementation that uses a static route source.
 * @author Paul Ferraro
 */
public class SimpleSessionIdentifierCodec implements SessionIdentifierCodec {

    private final RoutingSupport routing;
    private final String route;

    public SimpleSessionIdentifierCodec(RoutingSupport routing, String route) {
        this.routing = routing;
        this.route = route;
    }

    @Override
    public CharSequence encode(CharSequence sessionId) {
        return (this.route != null) ? this.routing.format(sessionId, this.route) : sessionId;
    }

    @Override
    public CharSequence decode(CharSequence encodedSessionId) {
        return (encodedSessionId != null) ? this.routing.parse(encodedSessionId).getKey() : null;
    }
}
