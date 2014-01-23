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

import org.jboss.msc.value.Value;
import org.junit.Test;

/**
 * Unit test for {@link SimpleSessionIdentifierCodec}.
 * @author Paul Ferraro
 */
public class SimpleSessionIdentifierCodecTestCase {

    private final Value<String> route = mock(Value.class);
    private final RoutingSupport routing = mock(RoutingSupport.class);

    private final SessionIdentifierCodec codec = new SimpleSessionIdentifierCodec(this.routing, this.route);

    @Test
    public void encode() {
        String result = this.codec.encode(null);

        assertNull(result);

        String sessionId = "session";

        when(this.route.getValue()).thenReturn(null);

        result = this.codec.encode(sessionId);

        assertSame(sessionId, result);

        String route = "route";
        String encodedSessionId = "session.route";

        when(this.route.getValue()).thenReturn(route);
        when(this.routing.format(sessionId, route)).thenReturn(encodedSessionId);

        result = this.codec.encode(sessionId);

        assertSame(encodedSessionId, result);
    }

    @Test
    public void decode() {
        String result = this.codec.decode(null);

        assertNull(result);

        String sessionId = "session";

        when(this.routing.parse(sessionId)).thenReturn(new SimpleImmutableEntry<String, String>(sessionId, null));

        result = this.codec.decode(sessionId);

        assertSame(sessionId, result);

        String route = "route";
        String encodedSessionId = "session.route";

        when(this.routing.parse(encodedSessionId)).thenReturn(new SimpleImmutableEntry<>(sessionId, route));

        result = this.codec.decode(encodedSessionId);

        assertSame(sessionId, result);
    }
}
