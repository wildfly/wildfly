/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.web.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.CharBuffer;
import java.util.Map;

import org.junit.Test;

/**
 * Unit test for {@link SimpleRoutingSupport}.
 * @author Paul Ferraro
 */
public class SimpleRoutingSupportTestCase {

    private final RoutingSupport routing = new SimpleRoutingSupport();

    @Test
    public void parse() {
        Map.Entry<CharSequence, CharSequence> result = this.routing.parse("session1.route1");
        assertEquals("session1", result.getKey().toString());
        assertEquals("route1", result.getValue().toString());

        result = this.routing.parse("session2");
        assertEquals("session2", result.getKey().toString());
        assertNull(result.getValue());

        result = this.routing.parse(null);
        assertNull(result.getKey());
        assertNull(result.getValue());

        result = this.routing.parse(CharBuffer.wrap("session1.route1"));
        assertEquals("session1", result.getKey().toString());
        assertEquals("route1", result.getValue().toString());

        result = this.routing.parse(new StringBuilder("session1.route1"));
        assertEquals("session1", result.getKey().toString());
        assertEquals("route1", result.getValue().toString());
    }

    @Test
    public void format() {
        assertEquals("session1.route1", this.routing.format("session1", "route1").toString());
        assertEquals("session2", this.routing.format("session2", "").toString());
        assertEquals("session3", this.routing.format("session3", null).toString());
        assertEquals("session1.route1", this.routing.format(CharBuffer.wrap("session1"), CharBuffer.wrap("route1")).toString());
        assertEquals("session1.route1", this.routing.format(new StringBuilder("session1"), new StringBuilder("route1")).toString());
        assertNull(this.routing.format(null, null));
    }
}
