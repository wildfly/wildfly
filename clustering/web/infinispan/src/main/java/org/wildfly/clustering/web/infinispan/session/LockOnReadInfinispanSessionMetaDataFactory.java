/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
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

    public LockOnReadInfinispanSessionMetaDataFactory(InfinispanSessionMetaDataFactoryConfiguration configuration) {
        super(configuration);
        Cache<SessionCreationMetaDataKey, SessionCreationMetaDataEntry<L>> creationMetaDataCache = configuration.getCache();
        this.creationMetaDataCache = creationMetaDataCache.getAdvancedCache().withFlags(Flag.FORCE_WRITE_LOCK);
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
