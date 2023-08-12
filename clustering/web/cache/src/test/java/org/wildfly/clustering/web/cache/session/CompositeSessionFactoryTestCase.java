/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.Test;
import org.wildfly.clustering.web.cache.Contextual;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributes;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.metadata.InvalidatableSessionMetaData;
import org.wildfly.clustering.web.cache.session.metadata.SessionMetaDataFactory;
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
    private final SessionMetaDataFactory<Contextual<Object>> metaDataFactory = mock(SessionMetaDataFactory.class);
    private final SessionAttributesFactory<Object, Object> attributesFactory = mock(SessionAttributesFactory.class);
    private final Supplier<Object> localContextFactory = mock(Supplier.class);

    private final SessionFactory<Object, Contextual<Object>, Object, Object> factory = new CompositeSessionFactory<>(this.metaDataFactory, this.attributesFactory, this.localContextFactory);

    @Test
    public void createValue() {
        Contextual<Object> contextual = mock(Contextual.class);
        Object attributes = new Object();
        String id = "id";

        when(this.metaDataFactory.createValue(id, null)).thenReturn(contextual);
        when(this.attributesFactory.createValue(id, null)).thenReturn(attributes);

        Map.Entry<Contextual<Object>, Object> result = this.factory.createValue(id, null);

        assertNotNull(result);
        assertSame(contextual, result.getKey());
        assertSame(attributes, result.getValue());
    }

    @Test
    public void findValue() {
        String missingMetaDataSessionId = "no-meta-data";
        String missingAttributesSessionId = "no-attributes";
        String existingSessionId = "existing";
        Contextual<Object> contextual = mock(Contextual.class);
        Object attributes = new Object();

        when(this.metaDataFactory.findValue(missingMetaDataSessionId)).thenReturn(null);
        when(this.metaDataFactory.findValue(missingAttributesSessionId)).thenReturn(contextual);
        when(this.metaDataFactory.findValue(existingSessionId)).thenReturn(contextual);
        when(this.attributesFactory.findValue(missingAttributesSessionId)).thenReturn(null);
        when(this.attributesFactory.findValue(existingSessionId)).thenReturn(attributes);

        Map.Entry<Contextual<Object>, Object> missingMetaDataResult = this.factory.findValue(missingMetaDataSessionId);
        Map.Entry<Contextual<Object>, Object> missingAttributesResult = this.factory.findValue(missingAttributesSessionId);
        Map.Entry<Contextual<Object>, Object> existingSessionResult = this.factory.findValue(existingSessionId);

        assertNull(missingMetaDataResult);
        assertNull(missingAttributesResult);
        assertNotNull(existingSessionResult);
        assertSame(contextual, existingSessionResult.getKey());
        assertSame(attributes, existingSessionResult.getValue());
    }

    @SuppressWarnings("unchecked")
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
        Object localContext = new Object();
        Contextual<Object> contextual = mock(Contextual.class);
        Object attributesValue = new Object();
        InvalidatableSessionMetaData metaData = mock(InvalidatableSessionMetaData.class);
        SessionAttributes attributes = mock(SessionAttributes.class);
        Object context = new Object();
        String id = "id";

        when(this.metaDataFactory.createSessionMetaData(id, contextual)).thenReturn(metaData);
        when(this.attributesFactory.createSessionAttributes(same(id), same(attributesValue), same(metaData), same(context))).thenReturn(attributes);
        when(contextual.getContext(this.localContextFactory)).thenReturn(localContext);

        Session<Object> result = this.factory.createSession(id, Map.entry(contextual, attributesValue), context);

        assertSame(id, result.getId());
        assertSame(metaData, result.getMetaData());
        assertSame(attributes, result.getAttributes());
        assertSame(localContext, result.getLocalContext());
    }

    @Test
    public void createImmutableSession() {
        Contextual<Object> contextual = mock(Contextual.class);
        Object attributesValue = new Object();
        ImmutableSessionMetaData metaData = mock(ImmutableSessionMetaData.class);
        ImmutableSessionAttributes attributes = mock(ImmutableSessionAttributes.class);
        String id = "id";

        when(this.metaDataFactory.createImmutableSessionMetaData(id, contextual)).thenReturn(metaData);
        when(this.attributesFactory.createImmutableSessionAttributes(id, attributesValue)).thenReturn(attributes);

        ImmutableSession result = this.factory.createImmutableSession(id, Map.entry(contextual, attributesValue));

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
