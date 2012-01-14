/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.threads;

import java.util.List;
import java.util.concurrent.ThreadFactory;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Provider of a default thread factory for a thread pool in case the thread pool does not have a specifically
 * configured thread factory. The absence of a specifically configured thread pool would be typical.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface DefaultThreadFactoryProvider {

    /**
     * Provides a default thread factory for a thread pool in case the thread pool does not have a specifically
     * configured thread factory.
     *
     * @param threadPoolName the name of the thread pool
     * @param threadPoolServiceName the full name of the {@link Service} that provides the thread pool
     * @param serviceTarget service target that is installing the thread pool service; can be used to install
     *                      a {@link ThreadFactoryService}
     * @param newControllers a list of {@link ServiceController}s that the {@code serviceTarget} is installing. If
     *                       the implementation adds a new service controller, it should add it to this list. May
     *                       be {@code null}
     * @param newServiceListeners {@link ServiceListener}s that should be added to any newly created service. May be
     *                            {@code null}
     *
     * @return the {@link ServiceName} of the {@link ThreadFactoryService} the thread pool should use. Cannot be
     *         {@code null}
     */
    ServiceName getDefaultThreadFactory(String threadPoolName, ServiceName threadPoolServiceName,
                                        ServiceTarget serviceTarget, List<ServiceController<?>> newControllers,
                                        ServiceListener<? super ThreadFactory>... newServiceListeners);

    /**
     * Standard implementation of {@link DefaultThreadFactoryProvider}.
     */
    DefaultThreadFactoryProvider STANDARD_PROVIDER = new DefaultThreadFactoryProvider() {

        /**
         * Installs a {@link ThreadFactoryService} whose {@link ThreadFactoryService#getThreadGroupName() thread group name}
         * is {@code threadPoolName + "-threads"} and whose {@link ServiceName} is {@code threadPoolServiceName.append(thread-factory)}.
         *
         * {@inheritDoc}
         */
        @Override
        public ServiceName getDefaultThreadFactory(final String threadPoolName, final ServiceName threadPoolServiceName,
                                                   final ServiceTarget serviceTarget, final List<ServiceController<?>> newControllers,
                                                   final ServiceListener<? super ThreadFactory>... newServiceListeners) {
            final ServiceName threadFactoryName = threadPoolServiceName.append("thread-factory");
            final ThreadFactoryService service = new ThreadFactoryService();
            service.setThreadGroupName(threadPoolName + "-threads");
            service.setNamePattern("%G - %t");
            ServiceBuilder<ThreadFactory> builder = serviceTarget.addService(threadFactoryName, service);
            if (newServiceListeners != null && newServiceListeners.length > 0) {
                for (ServiceListener<? super ThreadFactory> listener : newServiceListeners) {
                    builder.addListener(listener);
                }
            }
            ServiceController<?> sc = builder.install();
            if (newControllers != null) {
                newControllers.add(sc);
            }
            return threadFactoryName;
        }
    };
}
