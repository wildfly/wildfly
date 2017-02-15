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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.as.clustering.jgroups.ClassLoaderThreadFactory;
import org.jboss.as.clustering.jgroups.JChannelFactory;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jgroups.util.DefaultThreadFactory;
import org.jgroups.util.ShutdownRejectedExecutionHandler;
import org.jgroups.util.ThreadFactory;

/**
 * @author Paul Ferraro
 */
public class ThreadPoolFactoryBuilder extends AbstractThreadPoolFactoryBuilder<ThreadPoolFactory> {

    public ThreadPoolFactoryBuilder(ThreadPoolDefinition definition, PathAddress address) {
        super(definition, address);
    }

    @Override
    public ServiceBuilder<ThreadPoolFactory> build(ServiceTarget target) {
        int queueLength = this.getQueueLength();
        BlockingQueue<Runnable> queue = (queueLength > 0) ? new ArrayBlockingQueue<>(queueLength) : new SynchronousQueue<>();
        ClassLoader loader = JChannelFactory.class.getClassLoader();
        ThreadFactory threadFactory = new ClassLoaderThreadFactory(new DefaultThreadFactory(this.getThreadGroupPrefix(), false, true), loader);
        RejectedExecutionHandler handler = new ShutdownRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        ThreadPoolFactory factory = () -> new ThreadPoolExecutor(this.getMinThreads(), this.getMaxThreads(), this.getKeepAliveTime(), TimeUnit.MILLISECONDS, queue, threadFactory, handler);
        return target.addService(this.getServiceName(), new ValueService<>(new ImmediateValue<>(factory)));
    }
}
