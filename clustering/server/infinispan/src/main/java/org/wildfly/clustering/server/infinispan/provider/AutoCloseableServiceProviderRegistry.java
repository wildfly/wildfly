package org.wildfly.clustering.server.infinispan.provider;

import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.server.group.Group;

/**
 * A {@link Group} with a specific lifecycle (i.e. that must be closed).
 * @author Paul Ferraro
 * @param <T> the service type
 */
public interface AutoCloseableServiceProviderRegistry<T> extends ServiceProviderRegistry<T>, AutoCloseable {
    @Override
    void close();
}
