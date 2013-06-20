package org.wildfly.clustering.web.infinispan.sso;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.Remover;
import org.wildfly.clustering.web.infinispan.sso.InfinispanSSO;
import org.wildfly.clustering.web.sso.Credentials;
import org.wildfly.clustering.web.sso.SSO;
import org.wildfly.clustering.web.sso.Sessions;

@SuppressWarnings("unchecked")
public class InfinispanSSOTestCase {
    private final String id = "id";
    private final Credentials credentials = mock(Credentials.class);
    private final Sessions sessions = mock(Sessions.class);
    private final AtomicReference<Object> localContext = new AtomicReference<>();
    private final LocalContextFactory<Object> localContextFactory = mock(LocalContextFactory.class);
    private final Remover<String> remover = mock(Remover.class);

    private final SSO<Object> sso = new InfinispanSSO<>(this.id, this.credentials, this.sessions, this.localContext, this.localContextFactory, this.remover);
    
    @Test
    public void getId() {
        assertSame(this.id, this.sso.getId());
    }
    @Test
    public void getCredentials() {
        assertSame(this.credentials, this.sso.getCredentials());
    }
    
    @Test
    public void getSessions() {
        assertSame(this.sessions, this.sso.getSessions());
    }
    
    @Test
    public void invalidate() {
        this.sso.invalidate();
        
        verify(this.remover).remove(this.id);
    }
    
    @Test
    public void getLocalContext() {
        Object expected = new Object();
        when(this.localContextFactory.createLocalContext()).thenReturn(expected);
        
        Object result = this.sso.getLocalContext();
        
        assertSame(expected, result);
        
        reset(this.localContextFactory);
        
        result = this.sso.getLocalContext();
        
        verifyZeroInteractions(this.localContextFactory);
        
        assertSame(expected, result);
    }
}
