/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Paul Ferraro
 */
public class DefaultChannelFactoryRegistry implements ChannelFactoryRegistry {
    private final AtomicReference<String> defaultStack = new AtomicReference<String>();
    private final ConcurrentMap<String, ChannelFactory> stacks = new ConcurrentHashMap<String, ChannelFactory>();

    /**
     * {@inheritDoc}
     * @see org.jboss.as.clustering.jgroups.ChannelFactoryRegistry#getDefaultStack()
     */
    @Override
    public String getDefaultStack() {
        return this.defaultStack.get();
    }

    public void setDefaultStack(String stack) {
        this.defaultStack.set(stack);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.clustering.jgroups.ChannelFactoryRegistry#getStacks()
     */
    @Override
    public Set<String> getStacks() {
        return Collections.unmodifiableSet(this.stacks.keySet());
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.clustering.jgroups.ChannelFactoryRegistry#getChannelFactory(java.lang.String)
     */
    @Override
    public ChannelFactory getChannelFactory(String stack) {
        ChannelFactory factory = this.stacks.get((stack != null) ? stack : this.getDefaultStack());
        if (factory == null) {
            throw new IllegalArgumentException(String.format("No channel factory found for %s stack", stack));
        }
        return factory;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.clustering.jgroups.ChannelFactoryRegistry#addChannelFactory(java.lang.String, org.jboss.as.clustering.jgroups.ChannelFactory)
     */
    @Override
    public boolean addChannelFactory(String stack, ChannelFactory factory) {
        this.defaultStack.compareAndSet(null, stack);
        return this.stacks.putIfAbsent(stack, factory) == null;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.clustering.jgroups.ChannelFactoryRegistry#removeChannelFactory(java.lang.String)
     */
    @Override
    public boolean removeChannelFactory(String stack) {
        return this.stacks.remove(stack) != null;
    }
}
