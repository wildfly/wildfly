/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session;

import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.web.cache.session.CompositeSessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.SessionAccessMetaData;
import org.wildfly.clustering.web.cache.session.SessionCreationMetaDataEntry;

/**
 * {@link org.wildfly.clustering.web.cache.session.SessionMetaDataFactory} implementation for lock-on-read caches.
 * @author Paul Ferraro
 */
public class LockOnReadInfinispanSessionMetaDataFactory<L> extends AbstractInfinispanSessionMetaDataFactory<L> {

    private final Cache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> creationMetaDataCache;
    private final Cache<SessionAccessMetaDataKey, SessionAccessMetaData> accessMetaDataCache;

    public LockOnReadInfinispanSessionMetaDataFactory(InfinispanConfiguration configuration) {
        super(configuration);
        this.creationMetaDataCache = configuration.getReadForUpdateCache();
        this.accessMetaDataCache = configuration.getCache();
    }

    @Override
    public CompositeSessionMetaDataEntry<L> apply(String id, Set<Flag> flags) {
        SessionCreationMetaDataEntry<L> creationMetaDataEntry = this.creationMetaDataCache.getAdvancedCache().withFlags(flags).get(new SessionCreationMetaDataKey(id));
        if (creationMetaDataEntry != null) {
            SessionAccessMetaData accessMetaData = this.accessMetaDataCache.get(new SessionAccessMetaDataKey(id));
            if (accessMetaData != null) {
                return new CompositeSessionMetaDataEntry<>(creationMetaDataEntry, accessMetaData);
            }
            if (flags.isEmpty()) {
                // Purge orphaned entry
                this.purge(id);
            }
        }
        return null;
    }
}
