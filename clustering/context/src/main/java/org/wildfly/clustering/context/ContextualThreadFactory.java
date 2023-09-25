/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.context;

import java.util.concurrent.ThreadFactory;

/**
 * {@link ThreadFactory} decorator that contextualizes its threads.
 * @author Paul Ferraro
 */
public class ContextualThreadFactory<C> implements ThreadFactory {
    private final ThreadFactory factory;
    private final C targetContext;
    private final ThreadContextReference<C> reference;
    private final Contextualizer contextualizer;

    public ContextualThreadFactory(ThreadFactory factory, C targetContext, ThreadContextReference<C> reference) {
        this(factory, targetContext, reference, new ContextReferenceExecutor<>(targetContext, reference));
    }

    ContextualThreadFactory(ThreadFactory factory, C targetContext, ThreadContextReference<C> reference, Contextualizer contextualizer) {
        this.factory = factory;
        this.targetContext = targetContext;
        this.reference = reference;
        this.contextualizer = contextualizer;
    }

    @Override
    public Thread newThread(Runnable task) {
        Thread thread = this.factory.newThread(this.contextualizer.contextualize(task));
        this.reference.accept(thread, this.targetContext);
        return thread;
    }
}
