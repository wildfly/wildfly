/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.infinispan.commons.executors.ThreadPoolExecutorFactory;
import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.infinispan.configuration.global.ThreadPoolConfigurationBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Radoslav Husar
 */
public class ScheduledThreadPoolServiceConfigurator extends GlobalComponentServiceConfigurator<ThreadPoolConfiguration> {

    private final ThreadPoolConfigurationBuilder builder = new ThreadPoolConfigurationBuilder(null);
    private final ScheduledThreadPoolDefinition definition;

    ScheduledThreadPoolServiceConfigurator(ScheduledThreadPoolDefinition definition, PathAddress address) {
        super(definition, address);
        this.definition = definition;
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {

        int minThreads = this.definition.getMinThreads().resolveModelAttribute(context, model).asInt();
        long keepAliveTime = this.definition.getKeepAliveTime().resolveModelAttribute(context, model).asLong();

        ThreadPoolExecutorFactory<?> factory = new ThreadPoolExecutorFactory<ScheduledExecutorService>() {
            @Override
            public ScheduledExecutorService createExecutor(ThreadFactory factory) {
                // Use fixed size, based on maxThreads
                ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(minThreads, new DefaultThreadFactory(factory));
                executor.setKeepAliveTime(keepAliveTime, TimeUnit.MILLISECONDS);
                executor.setRemoveOnCancelPolicy(true);
                executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
                return executor;
            }

            @Override
            public void validate() {
                // Do nothing
            }
        };
        this.builder.threadPoolFactory(factory);

        return this;
    }

    @Override
    public ThreadPoolConfiguration get() {
        return this.builder.create();
    }
}

