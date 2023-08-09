/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.cache.session;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.wildfly.clustering.web.cache.Contextual;

/**
 * Cache entry containing the session creation meta data and local context.
 * @author Paul Ferraro
 */
public class SessionCreationMetaDataEntry<L> implements Contextual<L> {

    private final SessionCreationMetaData metaData;
    private final AtomicReference<L> context = new AtomicReference<>();

    public SessionCreationMetaDataEntry(SessionCreationMetaData metaData) {
        this.metaData = metaData;
    }

    public SessionCreationMetaData getMetaData() {
        return this.metaData;
    }

    @Override
    public L getContext(Supplier<L> factory) {
        return this.context.updateAndGet(context -> Optional.ofNullable(context).orElseGet(factory));
    }

    @Override
    public String toString() {
        return this.metaData.toString();
    }
}
