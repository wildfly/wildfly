/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.session;

import static org.mockito.Mockito.*;

import org.junit.Test;
import org.wildfly.clustering.ee.infinispan.Remover;
import org.wildfly.clustering.web.session.ImmutableSessionMetaData;

/**
 * Unit test for {@link ExpiredSessionRemover}.
 * @author Paul Ferraro
 */
public class ExpiredSessionRemoverTestCase {
    @Test
    public void test() {
        SessionMetaDataFactory<Object, Object> factory = mock(SessionMetaDataFactory.class);
        Remover<String> remover = mock(Remover.class);
        ImmutableSessionMetaData validMetaData = mock(ImmutableSessionMetaData.class);
        ImmutableSessionMetaData expiredMetaData = mock(ImmutableSessionMetaData.class);
        String missingSessionId = "missing";
        String expiredSessionId = "expired";
        String validSessionId = "valid";
        Object expiredValue = new Object();
        Object validValue = new Object();

        Remover<String> subject = new ExpiredSessionRemover<>(factory, remover);

        when(factory.tryValue(missingSessionId)).thenReturn(null);
        when(factory.tryValue(expiredSessionId)).thenReturn(expiredValue);
        when(factory.tryValue(validSessionId)).thenReturn(validValue);
        
        when(factory.createImmutableSessionMetaData(expiredSessionId, expiredValue)).thenReturn(expiredMetaData);
        when(factory.createImmutableSessionMetaData(validSessionId, validValue)).thenReturn(validMetaData);

        when(expiredMetaData.isExpired()).thenReturn(true);
        when(validMetaData.isExpired()).thenReturn(false);

        subject.remove(missingSessionId);
        subject.remove(expiredSessionId);
        subject.remove(validSessionId);

        verify(remover).remove(expiredSessionId);
        verify(remover, never()).remove(validSessionId);
    }
}
