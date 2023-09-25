/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.faulttolerance.deployment;

import java.util.OptionalInt;
import java.util.concurrent.ThreadFactory;

import javax.naming.InitialContext;

import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

import io.smallrye.faulttolerance.DefaultAsyncExecutorProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Subclass of {@link DefaultAsyncExecutorProvider} that provides a {@link ThreadFactory} as
 * configured in the server.
 *
 * @author Radoslav Husar
 * @author Jason Lee
 */
@Alternative
public class FaultToleranceContainerExecutorFactory extends DefaultAsyncExecutorProvider {

    @Inject
    public FaultToleranceContainerExecutorFactory(
            @ConfigProperty(name = "io.smallrye.faulttolerance.mainThreadPoolSize") OptionalInt mainThreadPoolSize,
            @ConfigProperty(name = "io.smallrye.faulttolerance.mainThreadPoolQueueSize") OptionalInt mainThreadPoolQueueSize,
            @ConfigProperty(name = "io.smallrye.faulttolerance.globalThreadPoolSize") OptionalInt globalThreadPoolSize
    ) {
        super(mainThreadPoolSize, mainThreadPoolQueueSize, globalThreadPoolSize);
    }

    @Override
    protected ThreadFactory threadFactory() {
        try {
            InitialContext initialContext = new InitialContext();
            return (ManagedThreadFactory) initialContext.lookup("java:jboss/ee/concurrency/factory/default");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
