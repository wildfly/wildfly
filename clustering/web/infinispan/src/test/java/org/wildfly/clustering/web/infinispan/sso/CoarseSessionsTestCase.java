package org.wildfly.clustering.web.infinispan.sso;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.wildfly.clustering.web.infinispan.Mutator;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSessions;
import org.wildfly.clustering.web.sso.Sessions;
import org.wildfly.clustering.web.sso.WebApplication;

public class CoarseSessionsTestCase {
    private Mutator mutator = mock(Mutator.class);
    private Map<WebApplication, String> map = mock(Map.class);
    private Sessions sessions = new CoarseSessions(this.map, this.mutator);

    @Test
    public void getApplications() {
        Set<WebApplication> expected = Collections.emptySet();
        when(this.map.keySet()).thenReturn(expected);
        
        Set<WebApplication> result = this.sessions.getApplications();
        
        assertSame(expected, result);
        
        verify(this.mutator, never()).mutate();
    }

    @Test
    public void getSession() {
        String expected = "id";
        WebApplication application = new WebApplication("context1", "host1");
        WebApplication missingApplication = new WebApplication("context2", "host1");
        
        when(this.map.get(application)).thenReturn(expected);
        when(this.map.get(missingApplication)).thenReturn(null);
        
        assertSame(expected, this.sessions.getSession(application));
        assertNull(this.sessions.getSession(missingApplication));
        
        verify(this.mutator, never()).mutate();
    }

    @Test
    public void addSession() {
        String id = "id";
        WebApplication application = new WebApplication("", "");
        
        when(this.map.put(application, id)).thenReturn(null);
        
        this.sessions.addSession(application, id);
        
        verify(this.mutator).mutate();
        
        reset(this.map, this.mutator);
        
        when(this.map.put(application, id)).thenReturn(id);
        
        this.sessions.addSession(application, id);
        
        verify(this.mutator, never()).mutate();
    }

    @Test
    public void removeSession() {
        WebApplication application = new WebApplication("", "");

        when(this.map.remove(application)).thenReturn("id");
        
        this.sessions.removeSession(application);
        
        verify(this.mutator).mutate();
        
        reset(this.map, this.mutator);
        
        when(this.map.remove(application)).thenReturn(null);
        
        this.sessions.removeSession(application);
        
        verify(this.mutator, never()).mutate();
    }
}
