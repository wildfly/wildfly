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

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.jboss.as.clustering.infinispan.invoker.Remover;
import org.junit.Test;
import org.wildfly.clustering.web.Batch;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.SessionMetaData;

public class SessionExpirationSchedulerTestCase {
    @Test
    public void test() throws InterruptedException {
        Batcher batcher = mock(Batcher.class);
        Batch batch = mock(Batch.class);
        Remover<String> remover = mock(Remover.class);
        ImmutableSession immortalSession = mock(ImmutableSession.class);
        ImmutableSession expiringSession = mock(ImmutableSession.class);
        ImmutableSession canceledSession = mock(ImmutableSession.class);
        SessionMetaData immortalMetaData = mock(SessionMetaData.class);
        SessionMetaData shortTimeoutMetaData = mock(SessionMetaData.class);
        SessionMetaData longTimeoutMetaData = mock(SessionMetaData.class);
        String immortalSessionId = "immortal";
        String expiringSessionId = "expiring";
        String canceledSessionId = "canceled";

        when(batcher.startBatch()).thenReturn(batch);

        when(immortalSession.isValid()).thenReturn(true);
        when(expiringSession.isValid()).thenReturn(true);
        when(canceledSession.isValid()).thenReturn(true);

        when(immortalSession.getMetaData()).thenReturn(immortalMetaData);
        when(expiringSession.getMetaData()).thenReturn(shortTimeoutMetaData);
        when(canceledSession.getMetaData()).thenReturn(longTimeoutMetaData);
        
        when(immortalMetaData.getMaxInactiveInterval(TimeUnit.MILLISECONDS)).thenReturn(0L);
        when(shortTimeoutMetaData.getMaxInactiveInterval(TimeUnit.MILLISECONDS)).thenReturn(1L);
        when(longTimeoutMetaData.getMaxInactiveInterval(TimeUnit.MILLISECONDS)).thenReturn(10000L);

        Date now = new Date();
        when(shortTimeoutMetaData.getLastAccessedTime()).thenReturn(now);
        when(longTimeoutMetaData.getLastAccessedTime()).thenReturn(now);
        
        when(immortalSession.getId()).thenReturn(immortalSessionId);
        when(expiringSession.getId()).thenReturn(expiringSessionId);
        when(canceledSession.getId()).thenReturn(canceledSessionId);
        
        try (Scheduler scheduler = new SessionExpirationScheduler(batcher, remover)) {
            scheduler.schedule(immortalSession);
            scheduler.schedule(canceledSession);
            scheduler.schedule(expiringSession);

            Thread.sleep(1000);

            scheduler.cancel(canceledSessionId);
            scheduler.schedule(canceledSession);
        }

        verify(remover, never()).remove(immortalSessionId);
        verify(remover).remove(expiringSessionId);
        verify(remover, never()).remove(canceledSessionId);
        verify(batch).close();
    }

}
