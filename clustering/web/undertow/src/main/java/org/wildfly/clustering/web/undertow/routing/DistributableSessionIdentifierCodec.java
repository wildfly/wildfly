/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.routing;

import java.util.function.UnaryOperator;

import org.jboss.as.web.session.RoutingSupport;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.as.web.session.SimpleRoutingSupport;

/**
 * {@link SessionIdentifierCodec} that encodes the route determined by a {@link RouteLocator}.
 * @author Paul Ferraro
 */
public class DistributableSessionIdentifierCodec implements SessionIdentifierCodec {

    private final UnaryOperator<String> locator;
    private final RoutingSupport routing;

    public DistributableSessionIdentifierCodec(UnaryOperator<String> locator) {
        this(locator, new SimpleRoutingSupport());
    }

    public DistributableSessionIdentifierCodec(UnaryOperator<String> locator, RoutingSupport routing) {
        this.locator = locator;
        this.routing = routing;
    }

    @Override
    public CharSequence encode(CharSequence sessionId) {
        String route = this.locator.apply(sessionId.toString());
        return (route != null) ? this.routing.format(sessionId, route) : sessionId;
    }

    @Override
    public CharSequence decode(CharSequence encodedSessionId) {
        return this.routing.parse(encodedSessionId).getKey();
    }
}
