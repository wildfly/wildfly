/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session;

import java.util.function.Supplier;

import org.wildfly.clustering.web.cache.Contextual;

/**
 * Wrapper for the components of a sessions's meta-data,
 * @author Paul Ferraro
 */
public class CompositeSessionMetaDataEntry<L> implements Contextual<L> {
    private final SessionCreationMetaDataEntry<L> creationMetaDataEntry;
    private final SessionAccessMetaData accessMetaData;

    public CompositeSessionMetaDataEntry(SessionCreationMetaDataEntry<L> creationMetaDataEntry, SessionAccessMetaData accessMetaData) {
        this.creationMetaDataEntry = creationMetaDataEntry;
        this.accessMetaData = accessMetaData;
    }

    public SessionCreationMetaDataEntry<L> getCreationMetaData() {
        return this.creationMetaDataEntry;
    }

    public SessionAccessMetaData getAccessMetaData() {
        return this.accessMetaData;
    }

    @Override
    public L getContext(Supplier<L> factory) {
        return this.creationMetaDataEntry.getContext(factory);
    }
}
