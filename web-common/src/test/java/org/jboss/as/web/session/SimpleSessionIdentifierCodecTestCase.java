/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

        String result = codec.encode(sessionId);

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

        String result = codec.decode(null);

        assertNull(result);

        String sessionId = "session";

        when(routing.parse(sessionId)).thenReturn(new SimpleImmutableEntry<String, String>(sessionId, null));

        result = codec.decode(sessionId);

        assertSame(sessionId, result);

        String encodedSessionId = "session.route";

        when(routing.parse(encodedSessionId)).thenReturn(new SimpleImmutableEntry<>(sessionId, route));

        result = codec.decode(encodedSessionId);

        assertSame(sessionId, result);
    }
}
