package org.jboss.as.clustering.infinispan;

import org.infinispan.AbstractDelegatingAdvancedCache;
import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;

/**
 * Workaround for ISPN-1583.
 * @author Paul Ferraro
 * @param <K> Cache key type
 * @param <V> Cache value type
 */
public abstract class AbstractAdvancedCache<K, V> extends AbstractDelegatingAdvancedCache<K, V> {

    protected AbstractAdvancedCache(AdvancedCache<K, V> cache) {
        super(cache);
    }

    protected abstract AdvancedCache<K, V> wrap(AdvancedCache<K, V> cache);

    @Override
    public AdvancedCache<K, V> withFlags(Flag... flags) {
        return this.wrap(this.cache.withFlags(flags));
    }

    @Override
    public AdvancedCache<K, V> with(ClassLoader classLoader) {
        return this.wrap(this.cache.with(classLoader));
    }
}
