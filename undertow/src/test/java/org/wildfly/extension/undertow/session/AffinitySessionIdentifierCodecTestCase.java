/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow.session;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Optional;

import org.jboss.as.web.session.RoutingSupport;
import org.jboss.as.web.session.SessionIdentifierCodec;
import org.junit.Test;

/**
 * Unit test for {@link AffinitySessionIdentifierCodec}.
 *
 * @author Paul Ferraro
 */
public class AffinitySessionIdentifierCodecTestCase {
    private final SessionAffinityProvider provider = mock(SessionAffinityProvider.class);
    private final RoutingSupport routing = mock(RoutingSupport.class);

    private SessionIdentifierCodec codec = new AffinitySessionIdentifierCodec(this.provider, this.routing);

    @Test
    public void encode() {
        String sessionId = "session";

        when(this.provider.getAffinity(sessionId)).thenReturn(Optional.empty());

        CharSequence result = this.codec.encode(sessionId);

        assertSame(sessionId, result);

        String route = "route";
        String encodedSessionId = "session.route";

        when(this.provider.getAffinity(sessionId)).thenReturn(Optional.of(route));
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

        verifyNoInteractions(this.provider);

        assertSame(sessionId, result);
    }
}
