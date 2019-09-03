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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

/**
 * Set of factory implementations for creating an {@link ExecutorService} from a {@link ThreadFactory}.
 * @author Paul Ferraro
 */
public enum ExecutorServiceFactory implements Function<ThreadFactory, ExecutorService> {

    SINGLE_THREAD() {
        @Override
        public ExecutorService apply(ThreadFactory factory) {
            return Executors.newSingleThreadExecutor(factory);
        }
    },
    CACHED_THREAD() {
        @Override
        public ExecutorService apply(ThreadFactory factory) {
            return Executors.newCachedThreadPool(factory);
        }
    },
    ;
}
