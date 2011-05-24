/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

import java.util.EnumSet;
import java.util.List;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A base class for controller services.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractControllerService implements Service<NewModelController> {
    private final NewOperationContext.Type controllerType;
    private final ConfigurationPersister configurationPersister;
    private final DescriptionProvider rootDescriptionProvider;
    private volatile NewModelControllerImpl controller;

    /**
     * Construct a new instance.
     *
     * @param controllerType the controller type for the new controller
     * @param configurationPersister the configuration persister
     * @param rootDescriptionProvider the root description provider
     */
    protected AbstractControllerService(final NewOperationContext.Type controllerType, final ConfigurationPersister configurationPersister, final DescriptionProvider rootDescriptionProvider) {
        this.controllerType = controllerType;
        this.configurationPersister = configurationPersister;
        this.rootDescriptionProvider = rootDescriptionProvider;
    }

    public void start(final StartContext context) throws StartException {
        final ServiceController<?> serviceController = context.getController();
        final ServiceContainer container = serviceController.getServiceContainer();
        final NewModelControllerImpl controller = new NewModelControllerImpl(container, context.getChildTarget(), ModelNodeRegistration.Factory.create(rootDescriptionProvider), new ContainerStateMonitor(container, serviceController), configurationPersister, controllerType);
        this.controller = controller;
        try {
            boot(context);
        } catch (ConfigurationPersistenceException e) {
            throw new StartException(e);
        }
    }

    /**
     * Boot the controller.  Called during service start.
     *
     * @param context the service start context
     * @throws ConfigurationPersistenceException if the configuration failed to be loaded
     */
    protected void boot(final StartContext context) throws ConfigurationPersistenceException {
        controller.boot(configurationPersister.load(), OperationMessageHandler.logging, NewModelController.OperationTransactionControl.COMMIT);
    }

    public void stop(final StopContext context) {
        controller = null;
    }

    public NewModelController getValue() throws IllegalStateException, IllegalArgumentException {
        final NewModelController controller = this.controller;
        if (controller == null) {
            throw new IllegalStateException();
        }
        return controller;
    }
}
