package org.jboss.as.clustering.infinispan.invoker;

import org.infinispan.Cache;

public class BatchOperation<K, V, R> implements CacheInvoker.Operation<K, V, R> {

    private final CacheInvoker.Operation<K, V, R> operation;

    public BatchOperation(CacheInvoker.Operation<K, V, R> operation) {
        this.operation = operation;
    }

    @Override
    public R invoke(Cache<K, V> cache) {
        boolean started = cache.startBatch();
        boolean success = false;

        try {
            R result = this.operation.invoke(cache);

            success = true;

            return result;
        } finally {
            if (started) {
                cache.endBatch(success);
            }
        }
    }
}
