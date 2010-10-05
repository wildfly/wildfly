/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.deployment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service that represents a deployment.  Should be used as a dependency for all services registered for the deployment.
 * The life-cycle of this service should be used to control the life-cycle of the deployment.
 *
 * @author John E. Bailey
 */
public class DeploymentService implements Service<Void> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("deployment");
    private static Logger logger = Logger.getLogger("org.jboss.as.deployment");

    private final Map<ServiceName, ServiceController<?>> dependents = new HashMap<ServiceName, ServiceController<?>>();

    /**
     * Start the deployment.  This will re-mount the deployment root if service is restarted.
     *
     * @param context The start context
     * @throws StartException if any problems occur
     */
    public void start(StartContext context) throws StartException {
    }

    /**
     * Stop the deployment.  This will close the virtual file mount.
     *
     * @param context The stop context
     */
    public void stop(StopContext context) {
    }

    /** {@inheritDoc} **/
    public Void getValue() throws IllegalStateException {
        return null;
    }

    /**
     * Gets the names of any services associated with the deployment.
     *
     * @return the service names. Will not return <code>null</code>
     */
    public Set<ServiceName> getDependentServiceNames() {
        synchronized(dependents) {
            return new HashSet<ServiceName>(dependents.keySet());
        }
    }

    /**
     * Gets any exceptions that occurred during start of the services that
     * are associated with this deployment.
     *
     * @return the exceptions keyed by the name of the service. Will not be <code>null</code>
     */
    public Map<ServiceName, StartException> getDependentStartupExceptions() {
        synchronized(dependents) {
            Map<ServiceName, StartException> result = new HashMap<ServiceName, StartException>();
            for (Map.Entry<ServiceName, ServiceController<?>> entry : dependents.entrySet()) {
                StartException se = entry.getValue().getStartException();
                if (se != null)
                    result.put(entry.getKey(), se);
            }
            return result;
        }
    }

    /**
     * Gets the {@link ServiceController.State state} of the services that
     * are associated with this deployment.
     *
     * @return the services and their current state. Will not be <code>null</code>
     */
    public Map<ServiceName, ServiceController.State> getDependentStates() {
        synchronized(dependents) {
            Map<ServiceName, ServiceController.State> result = new HashMap<ServiceName, ServiceController.State>(dependents.size());
            for (Map.Entry<ServiceName, ServiceController<?>> entry : dependents.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getState());
            }
            return result;
        }
    }

    /**
     * Gets a {@link ServiceListener} that can track startup events for
     * services associated with the deployment this service represents. This
     * listener should
     * be associated with a {@link BatchBuilder#subBatchBuilder() sub-batch}
     * of this services batch that encapsulates the creation of services that
     * are associated with the deployment.
     *
     * @return the service listener
     */
    public ServiceListener<Object> getDependentStartupListener() {
        return new DependentServiceListener();
    }

    private class DependentServiceListener extends AbstractServiceListener<Object> {

        /**
         * This will be called for all dependent services before the
         * BatchBuilder.install() call returns. So at that point we know what
         * the dependent services are.
         */
        @Override
        public void listenerAdded(ServiceController<?> controller) {
            synchronized(dependents) {
                dependents.put(controller.getName(), controller);
            }
        }

    }

}
