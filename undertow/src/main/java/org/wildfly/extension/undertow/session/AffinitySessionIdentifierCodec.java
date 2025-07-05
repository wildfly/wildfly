/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.session;

import org.jboss.as.web.session.RoutingSupport;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.jboss.as.web.session.SimpleRoutingSupport;

/**
 * {@link SessionIdentifierCodec} that encodes/decodes a server identifier provided by a {@link SessionAffinityProvider}.
 * @author Paul Ferraro
 */
public class AffinitySessionIdentifierCodec implements SessionIdentifierCodec {

    private final SessionAffinityProvider provider;
    private final RoutingSupport routing;

    public AffinitySessionIdentifierCodec(SessionAffinityProvider provider) {
        this(provider, new SimpleRoutingSupport());
    }

    public AffinitySessionIdentifierCodec(SessionAffinityProvider provider, RoutingSupport routing) {
        this.provider = provider;
        this.routing = routing;
    }

    @Override
    public CharSequence encode(CharSequence sessionId) {
        String route = this.provider.getAffinity(sessionId.toString()).orElse(null);
        return (route != null) ? this.routing.format(sessionId, route) : sessionId;
    }

    @Override
    public CharSequence decode(CharSequence encodedSessionId) {
        return this.routing.parse(encodedSessionId).getKey();
    }
}
