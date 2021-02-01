/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.faulttolerance;

import java.util.OptionalInt;
import java.util.concurrent.ThreadFactory;

import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.naming.InitialContext;

import io.smallrye.faulttolerance.DefaultAsyncExecutorProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Subclass of {@link io.smallrye.faulttolerance.DefaultAsyncExecutorProvider} that provides a ThreadFactory as
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

    protected ThreadFactory threadFactory() {
        try {
            InitialContext initialContext = new InitialContext();
            return (ManagedThreadFactory) initialContext.lookup("java:jboss/ee/concurrency/factory/default");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
