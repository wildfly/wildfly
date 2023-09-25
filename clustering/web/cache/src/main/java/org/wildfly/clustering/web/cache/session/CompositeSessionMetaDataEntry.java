/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Wrapper for the components of a sessions's meta-data,
 * @author Paul Ferraro
 */
public class CompositeSessionMetaDataEntry<L> {
    private final SessionCreationMetaData creationMetaData;
    private final SessionAccessMetaData accessMetaData;
    private final AtomicReference<L> localContext;

    public CompositeSessionMetaDataEntry(SessionCreationMetaDataEntry<L> creationMetaDataEntry, SessionAccessMetaData accessMetaData) {
        this(creationMetaDataEntry.getMetaData(), accessMetaData, creationMetaDataEntry.getLocalContext());
    }

    public CompositeSessionMetaDataEntry(SessionCreationMetaData creationMetaData, SessionAccessMetaData accessMetaData, AtomicReference<L> localContext) {
        this.creationMetaData = creationMetaData;
        this.accessMetaData = accessMetaData;
        this.localContext = localContext;
    }

    public SessionCreationMetaData getCreationMetaData() {
        return this.creationMetaData;
    }

    public SessionAccessMetaData getAccessMetaData() {
        return this.accessMetaData;
    }

    public AtomicReference<L> getLocalContext() {
        return this.localContext;
    }
}
