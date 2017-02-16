/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.Builder;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractThreadPoolFactoryBuilder<T> extends ThreadPoolServiceNameProvider implements ResourceServiceBuilder<T>, ThreadPoolConfiguration {

    private final ThreadPoolDefinition definition;

    private volatile int minThreads;
    private volatile int maxThreads;
    private volatile int queueLength;
    private volatile long keepAliveTime;

    protected AbstractThreadPoolFactoryBuilder(ThreadPoolDefinition definition, PathAddress address) {
        super(address);
        this.definition = definition;
    }

    @Override
    public Builder<T> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.minThreads = this.definition.getMinThreads().resolveModelAttribute(context, model).asInt();
        this.maxThreads = this.definition.getMaxThreads().resolveModelAttribute(context, model).asInt();
        this.queueLength = this.definition.getQueueLength().resolveModelAttribute(context, model).asInt();
        this.keepAliveTime = this.definition.getKeepAliveTime().resolveModelAttribute(context, model).asLong();
        return this;
    }

    @Override
    public String getThreadGroupPrefix() {
        return this.definition.getThreadGroupPrefix();
    }

    @Override
    public int getMinThreads() {
        return this.minThreads;
    }

    @Override
    public int getMaxThreads() {
        return this.maxThreads;
    }

    @Override
    public int getQueueLength() {
        return this.queueLength;
    }

    @Override
    public long getKeepAliveTime() {
        return this.keepAliveTime;
    }
}
