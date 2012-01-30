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

package org.jboss.as.clustering.infinispan;

import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.executors.ExecutorFactory;
import org.infinispan.executors.ScheduledExecutorFactory;
import org.jboss.as.clustering.concurrent.ManagedExecutorService;
import org.jboss.as.clustering.concurrent.ManagedScheduledExecutorService;

import static org.jboss.as.clustering.infinispan.InfinispanMessages.MESSAGES;

/**
 * @author Paul Ferraro
 */
public class ExecutorProvider implements ExecutorFactory, ScheduledExecutorFactory {
    private static final ExecutorProvider INSTANCE = new ExecutorProvider();
    private static final String EXECUTOR = "executor";

    public static void initListenerExecutor(GlobalConfigurationBuilder builder, Executor executor) {
        builder.asyncListenerExecutor().factory(INSTANCE).withProperties(createProperties(executor));
    }

    public static void initTransportExecutor(GlobalConfigurationBuilder builder, Executor executor) {
        builder.asyncTransportExecutor().factory(INSTANCE).withProperties(createProperties(executor));
    }

    public static void initEvictionExecutor(GlobalConfigurationBuilder builder, ScheduledExecutorService executor) {
        builder.evictionScheduledExecutor().factory(INSTANCE).withProperties(createProperties(executor));
    }

    public static void initReplicationQueueExecutor(GlobalConfigurationBuilder builder, ScheduledExecutorService executor) {
        builder.replicationQueueScheduledExecutor().factory(INSTANCE).withProperties(createProperties(executor));
    }

    private static Properties createProperties(Executor executor) {
        Properties properties = new Properties();
        properties.put(EXECUTOR, executor);
        return properties;
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.executors.ExecutorFactory#getExecutor(java.util.Properties)
     */
    @Override
    public ExecutorService getExecutor(Properties properties) {
        Executor executor = (Executor) properties.get(EXECUTOR);
        if (executor == null) {
            throw MESSAGES.invalidExecutorProperty(EXECUTOR, properties);
        }
        return new ManagedExecutorService(executor);
    }

    /**
     * {@inheritDoc}
     * @see org.infinispan.executors.ScheduledExecutorFactory#getScheduledExecutor(java.util.Properties)
     */
    @Override
    public ScheduledExecutorService getScheduledExecutor(Properties properties) {
        ScheduledExecutorService executor = (ScheduledExecutorService) properties.get(EXECUTOR);
        if (executor == null) {
            throw MESSAGES.invalidExecutorProperty(EXECUTOR, properties);
        }
        return new ManagedScheduledExecutorService(executor);
    }
}