/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.web.infinispan.session;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.wildfly.clustering.ee.Key;
import org.wildfly.clustering.web.cache.session.CompositeSessionMetaDataEntry;
import org.wildfly.clustering.web.cache.session.SessionAccessMetaData;
import org.wildfly.clustering.web.cache.session.SessionCreationMetaDataEntry;

/**
 * {@link org.wildfly.clustering.web.cache.session.SessionMetaDataFactory} implementation for read-committed and non-transactional caches.
 * @author Paul Ferraro
 */
public class InfinispanSessionMetaDataFactory<L> extends AbstractInfinispanSessionMetaDataFactory<L> {

    private final Cache<Key<String>, Object> cache;

    public InfinispanSessionMetaDataFactory(InfinispanSessionMetaDataFactoryConfiguration configuration) {
        super(configuration);
        this.cache = configuration.getCache();
    }

    @Override
    public CompositeSessionMetaDataEntry<L> apply(String id, Set<Flag> flags) {
        SessionCreationMetaDataKey creationMetaDataKey = new SessionCreationMetaDataKey(id);
        SessionAccessMetaDataKey accessMetaDataKey = new SessionAccessMetaDataKey(id);
        Set<Key<String>> keys = new HashSet<>(3);
        keys.add(creationMetaDataKey);
        keys.add(accessMetaDataKey);
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
