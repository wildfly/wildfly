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
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Resolves the service name of the executor service a thread pool service should use if it cannot itself accept
 * a task. Optionally provides a executor service for the thread pool to use in case the thread pool does not have a
 * specifically configured handoff executor. The absence of a specifically configured thread pool would be typical.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface HandoffExecutorResolver {

    /**
     * Resolves the service name of the handoff executor a thread pool service should use, optionally providing a default
     * executor in case the thread pool does not have a specifically configured handoff executor.
     *
     *
     * @param handoffExecutorName the simple name of the handoff executor. Typically the a reference value from
     *                          the thread pool resource's configuration. Can be {@code null} in which case a
     *                          default handoff executor may be returned.
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
     * @return the {@link ServiceName} of the executor service the thread pool should use. May be {@link null}
     */
    ServiceName resolveHandoffExecutor(String handoffExecutorName, String threadPoolName, ServiceName threadPoolServiceName,
                                       ServiceTarget serviceTarget, List<ServiceController<?>> newControllers,
                                       ServiceListener<? super ThreadFactory>... newServiceListeners);

    /**
     * Releases the handoff executor, doing any necessary cleanup, such as removing a default executor that
     * was installed by {@link #resolveHandoffExecutor(String, String, ServiceName, ServiceTarget, List, ServiceListener[])}.
     *
     * @param handoffExecutorName the simple name of the thread factory. Typically the a reference value from
     *                          the thread pool resource's configuration. Can be {@code null} in which case a
     *                          default thread factory should be released.
     * @param threadPoolName the name of the thread pool
     * @param threadPoolServiceName the full name of the {@link Service} that provides the thread pool
     * @param context the context of the current operation; can be used to perform any necessary
     *                {@link OperationContext#removeService(ServiceName) service removals}
     */
    void releaseHandoffExecutor(String handoffExecutorName, String threadPoolName, ServiceName threadPoolServiceName,
                                OperationContext context);

    /**
     * Standard implementation of {@link ThreadFactoryResolver} -- a {@link SimpleResolver} with a base service name
     * of {@link ThreadsServices#EXECUTOR}.
     */
    HandoffExecutorResolver STANDARD_RESOLVER = new SimpleResolver(ThreadsServices.EXECUTOR);

    /**
     * Base class for {@code ThreadFactoryResolver} implementations that handles the case of a null
     * {@code threadFactoryName} by installing a {@link ThreadFactoryService} whose service name is
     * the service name of the thread pool with {@code thread-factory} appended.
     */
    abstract class AbstractThreadFactoryResolver implements HandoffExecutorResolver {

        @Override
        public ServiceName resolveHandoffExecutor(String handoffExecutorName, String threadPoolName, ServiceName threadPoolServiceName, ServiceTarget serviceTarget, List<ServiceController<?>> newControllers, ServiceListener<? super ThreadFactory>... newServiceListeners) {
            ServiceName threadFactoryServiceName = null;

            if (handoffExecutorName != null) {
                threadFactoryServiceName = resolveNamedHandoffExecutor(handoffExecutorName, threadPoolName, threadPoolServiceName);
            } else {
                // Create a default
                threadFactoryServiceName = resolveDefaultHandoffExecutor(threadPoolName, threadPoolServiceName, serviceTarget, newControllers, newServiceListeners);
            }
            return threadFactoryServiceName;
        }

        @Override
        public void releaseHandoffExecutor(String handoffExecutorName, String threadPoolName, ServiceName threadPoolServiceName,
                                           OperationContext context) {
            if (handoffExecutorName != null) {
                releaseNamedHandoffExecutor(handoffExecutorName, threadPoolName, threadPoolServiceName, context);
            } else {
                releaseDefaultHandoffExecutor(threadPoolServiceName, context);
            }
        }

        /**
         * Create a service name to use for the thread factory in the case where a simple name for the factory was provided.
         *
         * @param handoffExecutorName the simple name of the thread factory. Will not be {@code null}
         * @param threadPoolName the simple name of the related thread pool
         * @param threadPoolServiceName the full service name of the thread pool
         *
         * @return the {@link ServiceName} of the {@link ThreadFactoryService} the thread pool should use. Cannot be
         *         {@code null}
         */
        protected abstract ServiceName resolveNamedHandoffExecutor(String handoffExecutorName, String threadPoolName, ServiceName threadPoolServiceName);

        /**
         * Handles the work of {@link #releaseHandoffExecutor(String, String, ServiceName, OperationContext)} for the case
         * where {@code threadFactoryName} is not {@code null}. This default implementation does nothing, assuming
         * the thread factory is independently managed from the pool.
         *
         * @param handoffExecutorName the simple name of the thread factory. Will not be {@code null}
         * @param threadPoolName    the simple name of the related thread pool
         * @param threadPoolServiceName the full service name of the thread pool
         * @param context  the context of the current operation; can be used to perform any necessary
         *                {@link OperationContext#removeService(ServiceName) service removals}
         */
        protected void releaseNamedHandoffExecutor(String handoffExecutorName, String threadPoolName, ServiceName threadPoolServiceName,
                                                   OperationContext context) {
            // no-op
        }

        /**
         * Optionally provides the service name of a default handoff executor. This implementation simply returns
         * {@code null}, meaning there is no default.
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
         * @return the {@link ServiceName} of the {@link ThreadFactoryService} the thread pool should use. May be {@code null}
         */
        protected ServiceName resolveDefaultHandoffExecutor(String threadPoolName, ServiceName threadPoolServiceName,
                                                            ServiceTarget serviceTarget, List<ServiceController<?>> newControllers,
                                                            ServiceListener<? super ThreadFactory>... newServiceListeners) {
            return null;
        }

        /**
         * Removes any default thread factory installed in {@link #resolveDefaultHandoffExecutor(String, ServiceName, ServiceTarget, List, ServiceListener[])}.
         * This default implementation does nothing, but any subclass that installs a default service should override this
         * method to remove it.
         *
         * @param threadPoolServiceName the full name of the {@link Service} that provides the thread pool
         * @param context the context of the current operation; can be used to perform any necessary
         *                {@link OperationContext#removeService(ServiceName) service removals}
         */
        protected void releaseDefaultHandoffExecutor(ServiceName threadPoolServiceName, OperationContext context) {
            // nothing to do since we didn't create anything
        }
    }

    /**
     * Extends {@link AbstractThreadFactoryResolver} to deal with named thread factories by appending their
     * simple name to a provided base name.
     */
    class SimpleResolver extends AbstractThreadFactoryResolver {

        final ServiceName handoffExecutorServiceNameBase;

        public SimpleResolver(ServiceName handoffExecutorServiceNameBase) {
            this.handoffExecutorServiceNameBase = handoffExecutorServiceNameBase;
        }

        @Override
        public ServiceName resolveNamedHandoffExecutor(String handoffExecutorName, String threadPoolName, ServiceName threadPoolServiceName) {
            return handoffExecutorServiceNameBase.append(handoffExecutorName);
        }
    }
}
