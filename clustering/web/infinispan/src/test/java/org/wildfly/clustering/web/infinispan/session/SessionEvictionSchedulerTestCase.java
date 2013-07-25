package org.wildfly.clustering.web.infinispan.session;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutorService;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.web.Batcher;
import org.wildfly.clustering.web.infinispan.Evictor;
import org.wildfly.clustering.web.infinispan.Scheduler;
import org.wildfly.clustering.web.session.Session;

public class SessionEvictionSchedulerTestCase {
    @Test
    public void test() {
        String evictedSessionId = "evicted";
        String activeSessionId = "active";
        Session<Void> evictedSession = mock(Session.class);
        Session<Void> activeSession = mock(Session.class);
        Batcher batcher = mock(Batcher.class);
        Evictor<String> evictor = mock(Evictor.class);
        ExecutorService executor = mock(ExecutorService.class);
        ArgumentCaptor<Runnable> capturedTask = ArgumentCaptor.forClass(Runnable.class);

        try (Scheduler<Session<Void>> scheduler = new SessionEvictionScheduler<>(batcher, evictor, 1, executor)) {
            when(evictedSession.getId()).thenReturn(evictedSessionId);
            when(activeSession.getId()).thenReturn(activeSessionId);
            
            scheduler.schedule(evictedSession);

            verifyZeroInteractions(executor);

            scheduler.schedule(activeSession);

            verify(executor).submit(capturedTask.capture());
            
            when(batcher.startBatch()).thenReturn(true);
            
            capturedTask.getValue().run();
            
            verify(evictor).evict(evictedSessionId);
            verify(evictor, never()).evict(activeSessionId);
            verify(batcher).endBatch(true);
        }
    }
}
