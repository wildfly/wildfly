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

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.jboss.as.clustering.web.OutgoingSessionGranularitySessionData;
import org.jboss.as.clustering.web.SessionAttributeMarshaller;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class CoarseSessionAttributeStorageTest {
    private SessionAttributeMarshaller marshaller = mock(SessionAttributeMarshaller.class);
    private SessionAttributeStorage<OutgoingSessionGranularitySessionData> storage = new CoarseSessionAttributeStorage(this.marshaller);

    @After
    public void before() {
        reset(this.marshaller);
    }

    @Test
    public void store() throws IOException {
        @SuppressWarnings("unchecked")
        Map<Object, Object> map = mock(Map.class);
        OutgoingSessionGranularitySessionData data = mock(OutgoingSessionGranularitySessionData.class);
        Map<String, Object> attributes = Collections.emptyMap();
        Object marshalledAttributes = new Object();

        when(data.getSessionAttributes()).thenReturn(attributes);
        when(this.marshaller.marshal(attributes)).thenReturn(marshalledAttributes);

        this.storage.store(map, data);

        verify(map).put(eq((byte) SessionMapEntry.ATTRIBUTES.ordinal()), same(marshalledAttributes));
    }

    @Test
    public void storeNull() throws IOException {
        @SuppressWarnings("unchecked")
        Map<Object, Object> map = mock(Map.class);
        OutgoingSessionGranularitySessionData data = mock(OutgoingSessionGranularitySessionData.class);

        when(data.getSessionAttributes()).thenReturn(null);

        this.storage.store(map, data);

        verifyZeroInteractions(map);
    }

    @Test
    public void load() throws Exception {
        @SuppressWarnings("unchecked")
        Map<Object, Object> map = mock(Map.class);
        Object marshalledAttributes = new Object();
        Map<String, Object> attributes = Collections.emptyMap();

        when(map.get(Byte.valueOf((byte) SessionMapEntry.ATTRIBUTES.ordinal()))).thenReturn(marshalledAttributes);
        when(this.marshaller.unmarshal(same(marshalledAttributes))).thenReturn(attributes);

        Map<String, Object> result = this.storage.load(map);

        Assert.assertSame(attributes, result);
    }
}
