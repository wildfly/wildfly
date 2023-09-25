/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.util.function.Consumer;

/**
 * An injector whose source can be defined after service installation, but before the service is started.
 * @author Paul Ferraro
 */
public class DeferredInjector<T> implements Consumer<T> {

    private volatile Consumer<? super T> consumer;

    @Override
    public void accept(T value) {
        this.consumer.accept(value);
    }

    void setConsumer(Consumer<? super T> consumer) {
        this.consumer = consumer;
    }
}
