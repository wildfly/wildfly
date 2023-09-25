/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session;

import java.util.Map;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.web.cache.session.CompositeSessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.SessionAccessMetaData;
import org.wildfly.clustering.web.cache.session.SessionCreationMetaDataEntry;

/**
 * {@link org.wildfly.clustering.web.cache.session.SessionMetaDataFactory} implementation for read-committed and non-transactional caches.
 * @author Paul Ferraro
 */
public class BulkReadInfinispanSessionMetaDataFactory<L> extends AbstractInfinispanSessionMetaDataFactory<L> {

    private final Cache<Key<String>, Object> cache;

    public BulkReadInfinispanSessionMetaDataFactory(InfinispanConfiguration configuration) {
        super(configuration);
        this.cache = configuration.getCache();
    }

    @Override
    public CompositeSessionMetaDataEntry<L> apply(String id, Set<Flag> flags) {
        SessionCreationMetaDataKey creationMetaDataKey = new SessionCreationMetaDataKey(id);
        SessionAccessMetaDataKey accessMetaDataKey = new SessionAccessMetaDataKey(id);
        Set<Key<String>> keys = Set.of(creationMetaDataKey, accessMetaDataKey);
        // Use bulk read
        Map<Key<String>, Object> entries = this.cache.getAdvancedCache().withFlags(flags).getAll(keys);
        @SuppressWarnings("unchecked")
        SessionCreationMetaDataEntry<L> creationMetaDataEntry = (SessionCreationMetaDataEntry<L>) entries.get(creationMetaDataKey);
        SessionAccessMetaData accessMetaData = (SessionAccessMetaData) entries.get(accessMetaDataKey);
        if ((creationMetaDataEntry != null) && (accessMetaData != null)) {
            return new CompositeSessionMetaDataEntry<>(creationMetaDataEntry, accessMetaData);
        }
        if (flags.isEmpty() && ((creationMetaDataEntry != null) || (accessMetaData != null))) {
            this.purge(id);
        }
        return null;
    }
}
