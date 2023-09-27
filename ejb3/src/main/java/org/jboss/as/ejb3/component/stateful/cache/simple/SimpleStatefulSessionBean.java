/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.component.stateful.cache.simple;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBean;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanInstance;

/**
 * A simple stateful session bean cache.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public class SimpleStatefulSessionBean<K, V extends StatefulSessionBeanInstance<K>> implements StatefulSessionBean<K, V> {
    enum State {
        VALID, DISCARDED, REMOVED, CLOSED;
    }

    private final V instance;
    private final Consumer<K> remover;
    private final Consumer<StatefulSessionBean<K, V>> closeTask;
    private final AtomicReference<State> state = new AtomicReference<>(State.VALID);

    public SimpleStatefulSessionBean(V instance, Consumer<K> remover, Consumer<StatefulSessionBean<K, V>> closeTask) {
        this.instance = instance;
        this.remover = remover;
        this.closeTask = closeTask;
    }

    @Override
    public V getInstance() {
        return this.instance;
    }

    @Override
    public boolean isClosed() {
        return this.state.get() == State.CLOSED;
    }

    @Override
    public boolean isDiscarded() {
        return this.state.get() == State.DISCARDED;
    }

    @Override
    public boolean isRemoved() {
        return this.state.get() == State.REMOVED;
    }

    @Override
    public void discard() {
        if (this.state.compareAndSet(State.VALID, State.DISCARDED)) {
            this.remover.accept(this.instance.getId());
        }
    }

    @Override
    public void remove() {
        if (this.state.compareAndSet(State.VALID, State.REMOVED)) {
            this.remover.accept(this.instance.getId());
            this.instance.removed();
        }
    }

    @Override
    public void close() {
        if (this.state.compareAndSet(State.VALID, State.CLOSED)) {
            this.closeTask.accept(this);
        }
    }
}