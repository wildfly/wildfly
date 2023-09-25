/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Cache entry containing the session creation meta data and local context.
 * @author Paul Ferraro
 */
public class SessionCreationMetaDataEntry<L> {

    private final SessionCreationMetaData metaData;
    private final AtomicReference<L> localContext;

    public SessionCreationMetaDataEntry(SessionCreationMetaData metaData) {
        this(metaData, new AtomicReference<>());
    }

    public SessionCreationMetaDataEntry(SessionCreationMetaData metaData, AtomicReference<L> localContext) {
        this.metaData = metaData;
        this.localContext = localContext;
    }

    public SessionCreationMetaData getMetaData() {
        return this.metaData;
    }

    public AtomicReference<L> getLocalContext() {
        return this.localContext;
    }

    @Override
    public String toString() {
        return this.metaData.toString();
    }
}
