/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering.web.infinispan;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.jboss.as.clustering.web.DistributableSessionMetadata;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class SessionMapEntryTest {
    @Test
    public void test() {
        for (SessionMapEntry entry : SessionMapEntry.values()) {
            switch (entry) {
                case VERSION: {
                    this.test(entry, new Integer(1), new Object());
                    break;
                }
                case TIMESTAMP: {
                    this.test(entry, new Long(1), new Object());
                    break;
                }
                case METADATA: {
                    this.test(entry, new DistributableSessionMetadata(), new Object());
                    break;
                }
                case ATTRIBUTES: {
                    this.test(entry, new Object(), null);
                    break;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void test(SessionMapEntry entry, Object valid, Object invalid) {
        Map<Object, Object> map = mock(Map.class);

        // Test null put
        entry.put(map, null);

        verifyZeroInteractions(map);

        // Test null get
        when(map.get(Byte.valueOf((byte) entry.ordinal()))).thenReturn(null);

        assertNull(entry.get(map));

        reset(map);

        if (invalid != null) {
            // Test illegal argument put

            IllegalArgumentException iae = null;

            try {
                entry.put(map, invalid);
            } catch (IllegalArgumentException e) {
                iae = e;
            }

            assertNotNull(iae);

            reset(map);

            // Test illegal argument get
            when(map.get(Byte.valueOf((byte) entry.ordinal()))).thenReturn(invalid);

            ClassCastException cce = null;

            try {
                entry.get(map);
            } catch (ClassCastException e) {
                cce = e;
            }

            assertNotNull(cce);

            reset(map);
        }

        // Test legal argument put
        when(map.put(eq(Byte.valueOf((byte) entry.ordinal())), same(valid))).thenReturn(null);

        assertNull(entry.put(map, valid));

        reset(map);

        // Test legal argument get
        when(map.get(Byte.valueOf((byte) entry.ordinal()))).thenReturn(valid);

        assertSame(valid, entry.get(map));
    }
}
