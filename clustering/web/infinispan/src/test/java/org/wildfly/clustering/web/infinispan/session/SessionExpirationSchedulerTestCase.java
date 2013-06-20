package org.wildfly.clustering.web.infinispan.session;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.infinispan.Remover;
import org.wildfly.clustering.web.infinispan.Scheduler;
import org.wildfly.clustering.web.session.Session;
import org.wildfly.clustering.web.session.SessionMetaData;

public class SessionExpirationSchedulerTestCase {
    @Test
    public void test() throws InterruptedException {
        Batcher batcher = mock(Batcher.class);
        Remover<String> remover = mock(Remover.class);
        Session<Void> immortalSession = mock(Session.class);
        Session<Void> expiringSession = mock(Session.class);
        Session<Void> canceledSession = mock(Session.class);
        SessionMetaData immortalMetaData = mock(SessionMetaData.class);
        SessionMetaData shortTimeoutMetaData = mock(SessionMetaData.class);
        SessionMetaData longTimeoutMetaData = mock(SessionMetaData.class);
        String expiringSessionId = "expiring";
        String canceledSessionId = "canceled";

        when(batcher.startBatch()).thenReturn(true);

        when(immortalSession.isValid()).thenReturn(true);
        when(expiringSession.isValid()).thenReturn(true);
        when(canceledSession.isValid()).thenReturn(true);

        when(immortalSession.getMetaData()).thenReturn(immortalMetaData);
        when(expiringSession.getMetaData()).thenReturn(shortTimeoutMetaData);
        when(canceledSession.getMetaData()).thenReturn(longTimeoutMetaData);
        
        when(immortalMetaData.getMaxInactiveInterval(TimeUnit.MILLISECONDS)).thenReturn(0L);
        when(shortTimeoutMetaData.getMaxInactiveInterval(TimeUnit.MILLISECONDS)).thenReturn(1L);
        when(longTimeoutMetaData.getMaxInactiveInterval(TimeUnit.MILLISECONDS)).thenReturn(10000L);
        
        when(expiringSession.getId()).thenReturn(expiringSessionId);
        when(canceledSession.getId()).thenReturn(canceledSessionId);
        
        try (Scheduler<Session<Void>> scheduler = new SessionExpirationScheduler<>(batcher, remover)) {
            scheduler.schedule(immortalSession);
            scheduler.schedule(canceledSession);
            scheduler.schedule(expiringSession);

            Thread.sleep(1000);

            scheduler.cancel(canceledSession);
            scheduler.schedule(canceledSession);
        }

        verify(remover).remove(expiringSessionId);
        verify(remover, never()).remove(canceledSessionId);
        verify(batcher).endBatch(true);
    }

}
