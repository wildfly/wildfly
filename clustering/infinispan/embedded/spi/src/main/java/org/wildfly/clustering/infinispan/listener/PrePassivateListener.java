/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.listener;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryPassivatedEvent;

/**
 * Generic non-blocking pre-passivation listener that delegates to a generic cache event listener.
 * @author Paul Ferraro
 */
@Listener(observation = Listener.Observation.PRE)
public class PrePassivateListener<K, V> {

    private final Function<CacheEntryEvent<K, V>, CompletionStage<Void>> listener;

    public PrePassivateListener(Function<CacheEntryEvent<K, V>, CompletionStage<Void>> listener) {
        this.listener = listener;
    }

    @CacheEntryPassivated
    public CompletionStage<Void> prePassivate(CacheEntryPassivatedEvent<K, V> event) {
        return this.listener.apply(event);
    }
}
