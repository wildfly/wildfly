/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.web.session;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.AbstractMap.SimpleImmutableEntry;

import org.junit.Test;

/**
 * Unit test for {@link SimpleSessionIdentifierCodec}.
 * @author Paul Ferraro
 */
public class SimpleSessionIdentifierCodecTestCase {

    @Test
    public void encode() {
        RoutingSupport routing = mock(RoutingSupport.class);
        SessionIdentifierCodec codec = new SimpleSessionIdentifierCodec(routing, null);
        String sessionId = "session";

        CharSequence result = codec.encode(sessionId);

        assertNull(sessionId, null);

        String route = "route";
        codec = new SimpleSessionIdentifierCodec(routing, route);

        result = codec.encode(null);

        assertNull(result);

        String encodedSessionId = "session.route";

        when(routing.format(sessionId, route)).thenReturn(encodedSessionId);

        result = codec.encode(sessionId);

        assertSame(encodedSessionId, result);
    }

    @Test
    public void decode() {
        RoutingSupport routing = mock(RoutingSupport.class);
        String route = "route";
        SessionIdentifierCodec codec = new SimpleSessionIdentifierCodec(routing, route);

        CharSequence result = codec.decode(null);

        assertNull(result);

        String sessionId = "session";

        when(routing.parse(sessionId)).thenReturn(new SimpleImmutableEntry<>(sessionId, null));

        result = codec.decode(sessionId);

        assertSame(sessionId, result);

        String encodedSessionId = "session.route";

        when(routing.parse(encodedSessionId)).thenReturn(new SimpleImmutableEntry<>(sessionId, route));

        result = codec.decode(encodedSessionId);

        assertSame(sessionId, result);
    }
}
