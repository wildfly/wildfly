/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.routing;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.function.UnaryOperator;

import org.jboss.as.web.session.RoutingSupport;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.junit.Test;

/**
 * Unit test for {@link DistributableSessionIdentifierCodec}.
 *
 * @author Paul Ferraro
 */
public class DistributableSessionIdentifierCodecTestCase {
    private final RoutingSupport routing = mock(RoutingSupport.class);
    private final UnaryOperator<String> locator = mock(UnaryOperator.class);

    private SessionIdentifierCodec codec = new DistributableSessionIdentifierCodec(this.locator, this.routing);

    @Test
    public void encode() {
        String sessionId = "session";

        when(this.locator.apply(sessionId)).thenReturn(null);

        CharSequence result = this.codec.encode(sessionId);

        assertSame(sessionId, result);

        String route = "route";
        String encodedSessionId = "session.route";

        when(this.locator.apply(sessionId)).thenReturn(route);
        when(this.routing.format(sessionId, route)).thenReturn(encodedSessionId);

        result = this.codec.encode(sessionId);

        assertSame(encodedSessionId, result);
    }

    @Test
    public void decode() {
        String encodedSessionId = "session.route";
        String sessionId = "session";
        String route = "route";

        when(this.routing.parse(encodedSessionId)).thenReturn(new SimpleImmutableEntry<>(sessionId, route));

        CharSequence result = this.codec.decode(encodedSessionId);

        assertSame(sessionId, result);
    }
}
