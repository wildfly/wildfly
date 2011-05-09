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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.clustering.web.OutgoingAttributeGranularitySessionData;
import org.jboss.as.clustering.web.SessionAttributeMarshaller;
import org.junit.After;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
public class FineSessionAttributeStorageTest {
    private SessionAttributeMarshaller marshaller = mock(SessionAttributeMarshaller.class);
    private SessionAttributeStorage<OutgoingAttributeGranularitySessionData> storage = new FineSessionAttributeStorage(this.marshaller);;

    @After
    public void before() {
        reset(this.marshaller);
    }

    @Test
    public void store() throws IOException {
        @SuppressWarnings("unchecked")
        Map<Object, Object> map = mock(Map.class);
        OutgoingAttributeGranularitySessionData data = mock(OutgoingAttributeGranularitySessionData.class);
        Map<String, Object> modifiedAttributes = Collections.singletonMap("key", (Object) "value");
        Set<String> removedAttributes = Collections.singleton("removed");
        Object marshalledAttribute = new Object();

        when(data.getModifiedSessionAttributes()).thenReturn(modifiedAttributes);
        when(this.marshaller.marshal("value")).thenReturn(marshalledAttribute);
        when(data.getRemovedSessionAttributes()).thenReturn(removedAttributes);

        this.storage.store(map, data);

        verify(map).put(eq("key"), same(marshalledAttribute));
        verify(map).remove("removed");
    }

    @Test
    public void storeNull() throws IOException {
        @SuppressWarnings("unchecked")
        Map<Object, Object> map = mock(Map.class);
        OutgoingAttributeGranularitySessionData data = mock(OutgoingAttributeGranularitySessionData.class);

        when(data.getModifiedSessionAttributes()).thenReturn(null);
        when(data.getRemovedSessionAttributes()).thenReturn(null);

        this.storage.store(map, data);

        verifyZeroInteractions(map);
    }

    @Test
    public void load() throws Exception {
        @SuppressWarnings("unchecked")
        Map<Object, Object> map = mock(Map.class);
        Object marshalledAttribute = new Object();

        Map.Entry<Object, Object> nonAttributeEntry = new AbstractMap.SimpleImmutableEntry<Object, Object>(new Object(), new Object());
        Map.Entry<Object, Object> attributeEntry = new AbstractMap.SimpleImmutableEntry<Object, Object>("key", marshalledAttribute);
        @SuppressWarnings("unchecked")
        List<Map.Entry<Object, Object>> entries = Arrays.asList(nonAttributeEntry, attributeEntry);

        when(map.entrySet()).thenReturn(new HashSet<Map.Entry<Object, Object>>(entries));
        when(this.marshaller.unmarshal(same(marshalledAttribute))).thenReturn("value");

        Map<String, Object> result = this.storage.load(map);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.toString(), result.containsKey("key"));
        assertEquals("value", result.get("key"));
    }

}
