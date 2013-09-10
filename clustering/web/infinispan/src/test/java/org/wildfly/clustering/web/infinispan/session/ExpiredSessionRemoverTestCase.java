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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jboss.as.clustering.infinispan.invoker.Remover;
import org.junit.Test;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionMetaData;

public class ExpiredSessionRemoverTestCase {
    @Test
    public void test() {
        SessionFactory<Object, Object> factory = mock(SessionFactory.class);
        Remover<String> remover = new ExpiredSessionRemover<>(factory);
        Session<Object> validSession = mock(Session.class);
        Session<Object> expiredSession = mock(Session.class);
        Session<Object> invalidSession = mock(Session.class);
        SessionMetaData validMetaData = mock(SessionMetaData.class);
        SessionMetaData expiredMetaData = mock(SessionMetaData.class);
        String missingSessionId = "missing";
        String expiredSessionId = "expired";
        String validSessionId = "valid";
        String invalidSessionId = "invalid";
        Object expiredValue = new Object();
        Object validValue = new Object();
        Object invalidValue = new Object();
        
        when(factory.findValue(missingSessionId)).thenReturn(null);
        when(factory.findValue(expiredSessionId)).thenReturn(expiredValue);
        when(factory.findValue(validSessionId)).thenReturn(validValue);
        when(factory.findValue(invalidSessionId)).thenReturn(invalidValue);
        
        when(factory.createSession(expiredSessionId, expiredValue)).thenReturn(expiredSession);
        when(factory.createSession(validSessionId, validValue)).thenReturn(validSession);
        when(factory.createSession(invalidSessionId, invalidValue)).thenReturn(invalidSession);
        
        when(expiredSession.isValid()).thenReturn(true);
        when(validSession.isValid()).thenReturn(true);
        when(invalidSession.isValid()).thenReturn(false);
        
        when(expiredSession.getMetaData()).thenReturn(expiredMetaData);
        when(validSession.getMetaData()).thenReturn(validMetaData);
        when(invalidSession.getMetaData()).thenReturn(validMetaData);
        
        when(expiredMetaData.isExpired()).thenReturn(true);
        when(validMetaData.isExpired()).thenReturn(false);
        
        remover.remove(missingSessionId);
        remover.remove(expiredSessionId);
        remover.remove(validSessionId);
        
        verify(expiredSession).invalidate();
        verify(validSession, never()).invalidate();
    }
}
