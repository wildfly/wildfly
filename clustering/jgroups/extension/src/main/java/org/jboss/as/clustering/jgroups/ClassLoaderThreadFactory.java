/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups;

import org.jgroups.util.ThreadFactory;
import org.wildfly.clustering.context.Contextualizer;
import org.wildfly.clustering.context.ThreadContextClassLoaderReference;

/**
 * {@link ThreadFactory} decorator that associates a specific class loader to created threads.
 * @author Paul Ferraro
 */
public class ClassLoaderThreadFactory implements org.jgroups.util.ThreadFactory {
    private final ThreadFactory factory;
    private final Contextualizer contextualizer;

    public ClassLoaderThreadFactory(ThreadFactory factory, ClassLoader targetLoader) {
        this.factory = factory;
        this.contextualizer = Contextualizer.withContextProvider(ThreadContextClassLoaderReference.CURRENT.provide(targetLoader));
    }

    @Override
    public Thread newThread(Runnable runner) {
        return this.newThread(runner, null);
    }

    @Override
    public Thread newThread(final Runnable runner, String name) {
        return this.factory.newThread(this.contextualizer.contextualize(runner), name);
    }

    @Override
    public void setPattern(String pattern) {
        this.factory.setPattern(pattern);
    }

    @Override
    public void setIncludeClusterName(boolean includeClusterName) {
        this.factory.setIncludeClusterName(includeClusterName);
    }

    @Override
    public void setClusterName(String channelName) {
        this.factory.setClusterName(channelName);
    }

    @Override
    public void setAddress(String address) {
        this.factory.setAddress(address);
    }

    @Override
    public void renameThread(String base_name, Thread thread) {
        this.factory.renameThread(base_name, thread);
    }

    @Override
    public boolean useVirtualThreads() {
        return this.factory.useVirtualThreads();
    }
}
