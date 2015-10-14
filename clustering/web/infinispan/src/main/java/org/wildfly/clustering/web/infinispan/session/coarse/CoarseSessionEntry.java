/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.web.infinispan.session.coarse;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.wildfly.clustering.ee.infinispan.MutableCacheEntry;
import org.wildfly.clustering.web.infinispan.session.SessionAccessMetaData;
import org.wildfly.clustering.web.infinispan.session.SessionCreationMetaData;

/**
 * Wrapper for session cache entry and session attributes cache entry.
 * @author Paul Ferraro
 */
public class CoarseSessionEntry<L> {
    private final MutableCacheEntry<SessionCreationMetaData> creationMetaDataEntry;
    private final MutableCacheEntry<SessionAccessMetaData> accessMetaDataEntry;
    private final MutableCacheEntry<Map<String, Object>> attributesEntry;
    private final AtomicReference<L> localContext;

    public CoarseSessionEntry(MutableCacheEntry<SessionCreationMetaData> creationMetaDataEntry, MutableCacheEntry<SessionAccessMetaData> accessMetaDataEntry, MutableCacheEntry<Map<String, Object>> attributesEntry, AtomicReference<L> localContext) {
        this.creationMetaDataEntry = creationMetaDataEntry;
        this.accessMetaDataEntry = accessMetaDataEntry;
        this.attributesEntry = attributesEntry;
        this.localContext = localContext;
    }

    public MutableCacheEntry<SessionCreationMetaData> getMutableSessionCreationMetaDataEntry() {
        return this.creationMetaDataEntry;
    }

    public MutableCacheEntry<SessionAccessMetaData> getMutableSessionAccessMetaDataEntry() {
        return this.accessMetaDataEntry;
    }

    public MutableCacheEntry<Map<String, Object>> getMutableAttributesEntry() {
        return this.attributesEntry;
    }

    public AtomicReference<L> getLocalContext() {
        return this.localContext;
    }
}
