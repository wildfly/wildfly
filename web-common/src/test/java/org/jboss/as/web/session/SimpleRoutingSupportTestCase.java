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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
        Map.Entry<String, String> result = this.routing.parse("session1.route1");
        assertEquals("session1", result.getKey());
        assertEquals("route1", result.getValue());

        result = this.routing.parse("session2");
        assertEquals("session2", result.getKey());
        assertNull(result.getValue());

        result = this.routing.parse(null);
        assertNull(result.getKey());
        assertNull(result.getValue());
    }

    @Test
    public void format() {
        assertEquals("session1.route1", this.routing.format("session1", "route1"));
        assertEquals("session2", this.routing.format("session2", ""));
        assertEquals("session3", this.routing.format("session3", null));
        assertNull(this.routing.format(null, null));
    }
}
