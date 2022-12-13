package org.wildfly.clustering.server.infinispan.group;

import org.wildfly.clustering.server.group.Group;

/**
 * A {@link Group} with a specific lifecycle (i.e. that must be closed).
 * @author Paul Ferraro
 * @param <A> the address type
 */
public interface AutoCloseableGroup<A> extends Group<A>, AutoCloseable {
    @Override
    void close();
}
