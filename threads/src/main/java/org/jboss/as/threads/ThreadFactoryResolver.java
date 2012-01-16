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

import org.jboss.as.controller.OperationContext;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Resolves the service name of the thread factory a thread pool service should use. Provides a default thread factory
 * for a thread pool in case the thread pool does not have a specifically configured thread factory. The absence of a
 * specifically configured thread pool would be typical.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface ThreadFactoryResolver {

    /**
     * Resolves the service name of the thread factory a thread pool service should use, providing a default thread
     * factory in case the thread pool does not have a specifically configured thread factory.
     *
     *
     * @param threadFactoryName the simple name of the thread factory. Typically a reference value from
     *                          the thread pool resource's configuration. Can be {@code null} in which case a
     *                          default thread factory should be returned.
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
    ServiceName resolveThreadFactory(String threadFactoryName, String threadPoolName, ServiceName threadPoolServiceName,
                                     ServiceTarget serviceTarget, List<ServiceController<?>> newControllers,
                                     ServiceListener<? super ThreadFactory>... newServiceListeners);

    /**
     * Releases the thread factory, doing any necessary cleanup, such as removing a default thread factory that
     * was installed by {@link #resolveThreadFactory(String, String, ServiceName, ServiceTarget, List, ServiceListener[])}.
     *
     * @param threadFactoryName the simple name of the thread factory. Typically a reference value from
     *                          the thread pool resource's configuration. Can be {@code null} in which case a
     *                          default thread factory should be released.
     * @param threadPoolName the name of the thread pool
     * @param threadPoolServiceName the full name of the {@link Service} that provides the thread pool
     * @param context the context of the current operation; can be used to perform any necessary
     *                {@link OperationContext#removeService(ServiceName) service removals}
     */
    void releaseThreadFactory(String threadFactoryName, String threadPoolName, ServiceName threadPoolServiceName,
                              OperationContext context);

    /**
     * Base class for {@code ThreadFactoryResolver} implementations that handles the case of a null
     * {@code threadFactoryName} by installing a {@link ThreadFactoryService} whose service name is
     * the service name of the thread pool with {@code thread-factory} appended.
     */
    abstract class AbstractThreadFactoryResolver implements ThreadFactoryResolver {

        @Override
        public ServiceName resolveThreadFactory(String threadFactoryName, String threadPoolName, ServiceName threadPoolServiceName, ServiceTarget serviceTarget, List<ServiceController<?>> newControllers, ServiceListener<? super ThreadFactory>... newServiceListeners) {
            ServiceName threadFactoryServiceName;

            if (threadFactoryName != null) {
                threadFactoryServiceName = resolveNamedThreadFactory(threadFactoryName, threadPoolName, threadPoolServiceName);
            } else {
                // Create a default
                threadFactoryServiceName = resolveDefaultThreadFactory(threadPoolName, threadPoolServiceName, serviceTarget, newControllers, newServiceListeners);
            }
            return threadFactoryServiceName;
        }

        @Override
        public void releaseThreadFactory(String threadFactoryName, String threadPoolName, ServiceName threadPoolServiceName,
                              OperationContext context) {
            if (threadFactoryName != null) {
                releaseNamedThreadFactory(threadFactoryName, threadPoolName, threadPoolServiceName, context);
            } else {
                releaseDefaultThreadFactory(threadPoolServiceName, context);
            }
        }

        /**
         * Create a service name to use for the thread factory in the case where a simple name for the factory was provided.
         *
         * @param threadFactoryName the simple name of the thread factory. Will not be {@code null}
         * @param threadPoolName the simple name of the related thread pool
         * @param threadPoolServiceName the full service name of the thread pool
         *
         * @return the {@link ServiceName} of the {@link ThreadFactoryService} the thread pool should use. Cannot be
         *         {@code null}
         */
        protected abstract ServiceName resolveNamedThreadFactory(String threadFactoryName, String threadPoolName, ServiceName threadPoolServiceName);

        /**
         * Handles the work of {@link #releaseThreadFactory(String, String, ServiceName, OperationContext)} for the case
         * where {@code threadFactoryName} is not {@code null}. This default implementation does nothing, assuming
         * the thread factory is independently managed from the pool.
         *
         * @param threadFactoryName the simple name of the thread factory. Will not be {@code null}
         * @param threadPoolName    the simple name of the related thread pool
         * @param threadPoolServiceName the full service name of the thread pool
         * @param context  the context of the current operation; can be used to perform any necessary
     *                {@link OperationContext#removeService(ServiceName) service removals}
         */
        protected void releaseNamedThreadFactory(String threadFactoryName, String threadPoolName, ServiceName threadPoolServiceName,
                              OperationContext context) {
            // no-op
        }

        /**
         * Installs a {@link ThreadFactoryService} whose service name is the service name of the thread pool with {@code thread-factory} appended.
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
        private ServiceName resolveDefaultThreadFactory(String threadPoolName, ServiceName threadPoolServiceName,
                                     ServiceTarget serviceTarget, List<ServiceController<?>> newControllers,
                                     ServiceListener<? super ThreadFactory>... newServiceListeners) {
            final ServiceName threadFactoryServiceName = threadPoolServiceName.append("thread-factory");
            final ThreadFactoryService service = new ThreadFactoryService();
            service.setThreadGroupName(threadPoolName + "-threads");
            service.setNamePattern("%G - %t");
            ServiceBuilder<ThreadFactory> builder = serviceTarget.addService(threadFactoryServiceName, service);
            if (newServiceListeners != null && newServiceListeners.length > 0) {
                for (ServiceListener<? super ThreadFactory> listener : newServiceListeners) {
                    builder.addListener(listener);
                }
            }
            ServiceController<?> sc = builder.install();
            if (newControllers != null) {
                newControllers.add(sc);
            }
            return threadFactoryServiceName;
        }

        /**
         * Removes any default thread factory installed in {@link #resolveDefaultThreadFactory(String, ServiceName, ServiceTarget, List, ServiceListener[])}.
         *
         * @param threadPoolServiceName the full name of the {@link Service} that provides the thread pool
         * @param context the context of the current operation; can be used to perform any necessary
     *                {@link OperationContext#removeService(ServiceName) service removals}
         */
        private void releaseDefaultThreadFactory(ServiceName threadPoolServiceName, OperationContext context) {
            final ServiceName threadFactoryServiceName = threadPoolServiceName.append("thread-factory");
            context.removeService(threadFactoryServiceName);
        }
    }

    /**
     * Extends {@link AbstractThreadFactoryResolver} to deal with named thread factories by appending their
     * simple name to a provided base name.
     */
    class SimpleResolver extends AbstractThreadFactoryResolver {

        final ServiceName threadFactoryServiceNameBase;

        public SimpleResolver(ServiceName threadFactoryServiceNameBase) {
            this.threadFactoryServiceNameBase = threadFactoryServiceNameBase;
        }

        @Override
        public ServiceName resolveNamedThreadFactory(String threadFactoryName, String threadPoolName, ServiceName threadPoolServiceName) {
            return threadFactoryServiceNameBase.append(threadFactoryName);
        }
    }
}
