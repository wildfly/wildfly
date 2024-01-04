/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

/**
 * @author Paul Ferraro
 */
public class DefaultSessionMetaDataEntry<C> implements SessionMetaDataEntry<C> {

    private final SessionCreationMetaDataEntry<C> creationMetaDataEntry;
    private final SessionAccessMetaDataEntry accessMetaDataEntry;

    public DefaultSessionMetaDataEntry(SessionCreationMetaDataEntry<C> creationMetaDataEntry, SessionAccessMetaDataEntry accessMetaDataEntry) {
        this.creationMetaDataEntry = creationMetaDataEntry;
        this.accessMetaDataEntry = accessMetaDataEntry;
    }

    @Override
    public SessionCreationMetaDataEntry<C> getCreationMetaDataEntry() {
        return this.creationMetaDataEntry;
    }

    @Override
    public SessionAccessMetaDataEntry getAccessMetaDataEntry() {
        return this.accessMetaDataEntry;
    }
}
