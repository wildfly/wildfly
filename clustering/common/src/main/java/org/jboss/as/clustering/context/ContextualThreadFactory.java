/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.context;

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
