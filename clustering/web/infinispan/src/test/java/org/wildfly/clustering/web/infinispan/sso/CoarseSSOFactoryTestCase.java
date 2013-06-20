package org.wildfly.clustering.web.infinispan.sso;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.SimpleCacheInvoker;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSSOCacheEntry;
import org.wildfly.clustering.web.infinispan.sso.coarse.CoarseSSOFactory;

public class CoarseSSOFactoryTestCase {
    private final CacheInvoker invoker = new SimpleCacheInvoker();
    private final AdvancedCache<String, CoarseSSOCacheEntry<Object>> cache = mock(AdvancedCache.class);
    private final LocalContextFactory<Object> localContextFactory = mock(LocalContextFactory.class);
    
    private CoarseSSOFactory<Object> factory = new CoarseSSOFactory<>(this.cache, this.invoker, this.localContextFactory);

    @SuppressWarnings("rawtypes")
    @Test
    public void createValue() {
        ArgumentCaptor<CoarseSSOCacheEntry> capturedEntry = ArgumentCaptor.forClass(CoarseSSOCacheEntry.class);
        String newId = "new";
        String existingId = "existing";
        CoarseSSOCacheEntry<Object> entry = new CoarseSSOCacheEntry<>();
        
        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.withFlags()).thenReturn(this.cache);
        when(this.cache.putIfAbsent(same(newId), capturedEntry.capture())).thenReturn(null);
        when(this.cache.putIfAbsent(same(existingId), any(CoarseSSOCacheEntry.class))).thenReturn(entry);
        
        CoarseSSOCacheEntry<Object> result = this.factory.createValue(newId);
        
        assertSame(capturedEntry.getValue(), result);

        result = this.factory.createValue(existingId);
        
        assertSame(entry, result);
    }

    @Test
    public void getValue() {
        String missingId = "missing";
        String existingId = "existing";
        CoarseSSOCacheEntry<Object> entry = new CoarseSSOCacheEntry<>();
        
        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.withFlags()).thenReturn(this.cache);
        when(this.cache.get(missingId)).thenReturn(null);
        when(this.cache.get(existingId)).thenReturn(entry);
        
        CoarseSSOCacheEntry<Object> result = this.factory.findValue(missingId);
        
        assertNull(result);
        
        result = this.factory.findValue(existingId);
        
        assertSame(entry, result);
    }
    
    @Test
    public void remove() {
        String id = "id";
        when(this.cache.getAdvancedCache()).thenReturn(this.cache);
        when(this.cache.withFlags(Flag.IGNORE_RETURN_VALUES)).thenReturn(this.cache);
        
        this.factory.remove(id);
        
        verify(this.cache).remove(id);
    }
}
