/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.cache.session;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;
import org.wildfly.clustering.web.session.Session;

/**
 * Unit test for {@link CompositeSessionFactory}.
 *
 * @author Paul Ferraro
 */
public class CompositeSessionFactoryTestCase {
    private final SessionMetaDataFactory<CompositeSessionMetaDataEntry<Object>> metaDataFactory = mock(SessionMetaDataFactory.class);
    private final SessionAttributesFactory<Object, Object> attributesFactory = mock(SessionAttributesFactory.class);
    private final LocalContextFactory<Object> localContextFactory = mock(LocalContextFactory.class);

    private final SessionFactory<Object, CompositeSessionMetaDataEntry<Object>, Object, Object> factory = new CompositeSessionFactory<>(this.metaDataFactory, this.attributesFactory, this.localContextFactory);

    @Test
    public void createValue() {
        SessionCreationMetaData creationMetaData = mock(SessionCreationMetaData.class);
        SessionAccessMetaData accessMetaData = mock(SessionAccessMetaData.class);
        AtomicReference<Object> localContext = new AtomicReference<>();
        CompositeSessionMetaDataEntry<Object> metaData = new CompositeSessionMetaDataEntry<>(creationMetaData, accessMetaData, localContext);
        Object attributes = new Object();
        String id = "id";

        when(this.metaDataFactory.createValue(id, null)).thenReturn(metaData);
        when(this.attributesFactory.createValue(id, null)).thenReturn(attributes);

        Map.Entry<CompositeSessionMetaDataEntry<Object>, Object> result = this.factory.createValue(id, null);

        assertNotNull(result);
        assertSame(metaData, result.getKey());
        assertSame(attributes, result.getValue());
    }

    @Test
    public void findValue() {
        String missingMetaDataSessionId = "no-meta-data";
        String missingAttributesSessionId = "no-attributes";
        String existingSessionId = "existing";
        SessionCreationMetaData creationMetaData = mock(SessionCreationMetaData.class);
        SessionAccessMetaData accessMetaData = mock(SessionAccessMetaData.class);
        AtomicReference<Object> localContext = new AtomicReference<>();
        CompositeSessionMetaDataEntry<Object> metaData = new CompositeSessionMetaDataEntry<>(creationMetaData, accessMetaData, localContext);
        Object attributes = new Object();

        when(this.metaDataFactory.findValue(missingMetaDataSessionId)).thenReturn(null);
        when(this.metaDataFactory.findValue(missingAttributesSessionId)).thenReturn(metaData);
        when(this.metaDataFactory.findValue(existingSessionId)).thenReturn(metaData);
        when(this.attributesFactory.findValue(missingAttributesSessionId)).thenReturn(null);
        when(this.attributesFactory.findValue(existingSessionId)).thenReturn(attributes);

        Map.Entry<CompositeSessionMetaDataEntry<Object>, Object> missingMetaDataResult = this.factory.findValue(missingMetaDataSessionId);
        Map.Entry<CompositeSessionMetaDataEntry<Object>, Object> missingAttributesResult = this.factory.findValue(missingAttributesSessionId);
        Map.Entry<CompositeSessionMetaDataEntry<Object>, Object> existingSessionResult = this.factory.findValue(existingSessionId);

        assertNull(missingMetaDataResult);
        assertNull(missingAttributesResult);
        assertNotNull(existingSessionResult);
        assertSame(metaData, existingSessionResult.getKey());
        assertSame(attributes, existingSessionResult.getValue());
    }

    @Test
    public void remove() {
        String id = "id";

        when(this.metaDataFactory.remove(id)).thenReturn(false);

        boolean removed = this.factory.remove(id);

        verify(this.attributesFactory).remove(id);

        assertFalse(removed);

        reset(this.attributesFactory);

        when(this.metaDataFactory.remove(id)).thenReturn(true);

        removed = this.factory.remove(id);

        verify(this.attributesFactory).remove(id);

        assertTrue(removed);
    }

    @Test
    public void getMetaDataFactory() {
        assertSame(this.metaDataFactory, this.factory.getMetaDataFactory());
    }

    @Test
    public void createSession() {
        Map.Entry<CompositeSessionMetaDataEntry<Object>, Object> entry = mock(Map.Entry.class);
        SessionCreationMetaData creationMetaData = mock(SessionCreationMetaData.class);
        SessionAccessMetaData accessMetaData = mock(SessionAccessMetaData.class);
        Object localContext = new Object();
        CompositeSessionMetaDataEntry<Object> metaDataValue = new CompositeSessionMetaDataEntry<>(creationMetaData, accessMetaData, new AtomicReference<>(localContext));
        Object attributesValue = new Object();
        InvalidatableSessionMetaData metaData = mock(InvalidatableSessionMetaData.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Object context = new Object();
        String id = "id";

        when(entry.getKey()).thenReturn(metaDataValue);
        when(entry.getValue()).thenReturn(attributesValue);
        when(this.metaDataFactory.createSessionMetaData(id, metaDataValue)).thenReturn(metaData);
        when(this.attributesFactory.createSessionAttributes(same(id), same(attributesValue), same(metaData), same(context))).thenReturn(attributes);

        Session<Object> result = this.factory.createSession(id, entry, context);

        assertSame(id, result.getId());
        assertSame(metaData, result.getMetaData());
        assertSame(attributes, result.getAttributes());
        assertSame(localContext, result.getLocalContext());
    }

    @Test
    public void createImmutableSession() {
        Map.Entry<CompositeSessionMetaDataEntry<Object>, Object> entry = mock(Map.Entry.class);
        SessionCreationMetaData creationMetaData = mock(SessionCreationMetaData.class);
        SessionAccessMetaData accessMetaData = mock(SessionAccessMetaData.class);
        CompositeSessionMetaDataEntry<Object> metaDataValue = new CompositeSessionMetaDataEntry<>(creationMetaData, accessMetaData, null);
        Object attributesValue = new Object();
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        ImmutableSessionAttributes attributes = mock(ImmutableSessionAttributes.class);
        String id = "id";

        when(entry.getKey()).thenReturn(metaDataValue);
        when(entry.getValue()).thenReturn(attributesValue);
        when(this.metaDataFactory.createImmutableSessionMetaData(id, metaDataValue)).thenReturn(metaData);
        when(this.attributesFactory.createImmutableSessionAttributes(id, attributesValue)).thenReturn(attributes);

        ImmutableSession result = this.factory.createImmutableSession(id, entry);

        assertSame(id, result.getId());
        assertSame(metaData, result.getMetaData());
        assertSame(attributes, result.getAttributes());
    }

    @Test
    public void close() {
        this.factory.close();

        verify(this.metaDataFactory).close();
        verify(this.attributesFactory).close();
    }
}
