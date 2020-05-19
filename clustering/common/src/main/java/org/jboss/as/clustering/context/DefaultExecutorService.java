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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

import org.wildfly.security.ParametricPrivilegedAction;

/**
 * {@link ExecutorService} that performs contextual execution of submitted tasks.
 * @author Paul Ferraro
 */
public class DefaultExecutorService extends ContextualExecutorService {

    public static final ParametricPrivilegedAction<Void, ExecutorService> SHUTDOWN_ACTION = new ParametricPrivilegedAction<Void, ExecutorService>() {
        @Override
        public Void run(ExecutorService executor) {
            executor.shutdown();
            return null;
        }
    };

    public static final ParametricPrivilegedAction<List<Runnable>, ExecutorService> SHUTDOWN_NOW_ACTION = new ParametricPrivilegedAction<List<Runnable>, ExecutorService>() {
        @Override
        public List<Runnable> run(ExecutorService executor) {
            return executor.shutdownNow();
        }
    };

    public DefaultExecutorService(Class<?> targetClass, Function<ThreadFactory, ExecutorService> factory) {
        super(factory.apply(new DefaultThreadFactory(targetClass)), new DefaultContextualizer(targetClass));
    }
}
