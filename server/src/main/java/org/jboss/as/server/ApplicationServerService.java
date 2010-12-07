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

package org.jboss.as.server;

import java.util.List;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.ServerModel;
import org.jboss.logging.Logger;
import org.jboss.msc.service.DelegatingServiceRegistry;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.TrackingServiceTarget;

/**
 * The root service of the JBoss Application Server.  Stopping this
 * service will shut down the whole works.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ApplicationServerService implements Service<ServerController> {

    private ServerController serverController;
    private TrackingServiceTarget serviceTarget;

    private final Bootstrap.Configuration configuration;
    private final List<AbstractServerModelUpdate<?>> initialUpdates;
    private final List<ServiceActivator> services;

    public ApplicationServerService(final Bootstrap.Configuration configuration, final List<AbstractServerModelUpdate<?>> initialUpdates, final List<ServiceActivator> services) {
        this.configuration = configuration;
        this.initialUpdates = initialUpdates;
        this.services = services;
    }

    public synchronized void start(final StartContext context) throws StartException {
        serviceTarget = new TrackingServiceTarget(context.getController().getServiceContainer());
        final ServiceActivatorContext serviceActivatorContext = new ServiceActivatorContext() {
            public ServiceTarget getServiceTarget() {
                return serviceTarget;
            }

            public ServiceRegistry getServiceRegistry() {
                return new DelegatingServiceRegistry(context.getController().getServiceContainer());
            }
        };
        serverController = new ServerControllerImpl(new ServerModel(configuration.getName(), configuration.getPortOffset()), context.getController().getServiceContainer(), configuration.getServerEnvironment().isStandalone());
        for (ServiceActivator activator : services) {
            activator.activate(serviceActivatorContext);
        }
        // todo: apply boot updates here, but don't bother waiting for completion - the updates should install new services
        
    }

    public synchronized void stop(final StopContext context) {
        serverController = null;
        Logger.getLogger("org.jboss.as").infof("Stopped JBoss AS in %dms", Integer.valueOf((int) (context.getElapsedTime() / 1000000L)));
        // service cannot be restarted.
        context.getController().setMode(ServiceController.Mode.REMOVE);
    }

    public ServerController getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }
}
