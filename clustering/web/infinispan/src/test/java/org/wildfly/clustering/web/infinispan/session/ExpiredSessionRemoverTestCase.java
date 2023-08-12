/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.infinispan.session;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.Consumer;

import org.junit.Test;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.web.cache.session.SessionFactory;
import org.wildfly.clustering.web.cache.session.attributes.SessionAttributesFactory;
import org.wildfly.clustering.web.cache.session.metadata.SessionMetaDataFactory;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.ImmutableSessionAttributes;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Unit test for {@link ExpiredSessionRemover}.
 *
 * @author Paul Ferraro
 */
public class ExpiredSessionRemoverTestCase {
    @Test
    public void test() {
        SessionFactory<Object, UUID, UUID, Object> factory = mock(SessionFactory.class);
        SessionMetaDataFactory<UUID> metaDataFactory = mock(SessionMetaDataFactory.class);
        SessionAttributesFactory<Object, UUID> attributesFactory = mock(SessionAttributesFactory.class);
        Consumer<ImmutableSession> listener = mock(Consumer.class);
        ImmutableSessionAttributes expiredAttributes = mock(ImmutableSessionAttributes.class);
        ImmutableSessionMetaData validMetaData = mock(ImmutableSessionMetaData.class);
        ImmutableSessionMetaData expiredMetaData = mock(ImmutableSessionMetaData.class);
        ImmutableSession expiredSession = mock(ImmutableSession.class);

        String missingSessionId = "missing";
        String expiredSessionId = "expired";
        String validSessionId = "valid";

        UUID expiredMetaDataValue = UUID.randomUUID();
        UUID expiredAttributesValue = UUID.randomUUID();
        UUID validMetaDataValue = UUID.randomUUID();

        ExpiredSessionRemover<Object, UUID, UUID, Object> subject = new ExpiredSessionRemover<>(factory);

        try (Registration regisration = subject.register(listener)) {
            when(factory.getMetaDataFactory()).thenReturn(metaDataFactory);
            when(factory.getAttributesFactory()).thenReturn(attributesFactory);
            when(metaDataFactory.tryValue(missingSessionId)).thenReturn(null);
            when(metaDataFactory.tryValue(expiredSessionId)).thenReturn(expiredMetaDataValue);
            when(metaDataFactory.tryValue(validSessionId)).thenReturn(validMetaDataValue);

            when(metaDataFactory.createImmutableSessionMetaData(expiredSessionId, expiredMetaDataValue)).thenReturn(expiredMetaData);
            when(metaDataFactory.createImmutableSessionMetaData(validSessionId, validMetaDataValue)).thenReturn(validMetaData);

            when(expiredMetaData.isExpired()).thenReturn(true);
            when(validMetaData.isExpired()).thenReturn(false);

            when(attributesFactory.findValue(expiredSessionId)).thenReturn(expiredAttributesValue);
            when(attributesFactory.createImmutableSessionAttributes(expiredSessionId, expiredAttributesValue)).thenReturn(expiredAttributes);
            when(factory.createImmutableSession(same(expiredSessionId), same(expiredMetaData), same(expiredAttributes))).thenReturn(expiredSession);

            subject.remove(missingSessionId);
            subject.remove(expiredSessionId);
            subject.remove(validSessionId);

            verify(factory).remove(expiredSessionId);
            verify(factory, never()).remove(missingSessionId);
            verify(factory, never()).remove(validSessionId);

            verify(listener).accept(expiredSession);
        }
    }
}
