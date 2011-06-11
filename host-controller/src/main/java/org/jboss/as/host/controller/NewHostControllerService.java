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

package org.jboss.as.host.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;

import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.BootContext;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.domain.controller.DomainModelUtil;
import org.jboss.as.domain.controller.descriptions.DomainDescriptionProviders;
import org.jboss.dmr.ModelNode;

/**
 * Creates the service that acts as the {@link org.jboss.as.controller.NewModelController} for a host.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class NewHostControllerService extends AbstractControllerService {

    private final DelegatingConfigurationPersister configurationPersister;
    private final HostControllerEnvironment environment;
    private final DomainModelProxy domainModelProxy;

    /**
     * Construct a new instance.
     *
     * @param configurationPersister  the configuration persister
     * @param prepareStep             the prepare step to prepend to operation execution
     */
    protected NewHostControllerService(final DelegatingConfigurationPersister configurationPersister,
                                       final HostControllerEnvironment environment, final DomainModelProxy domainModelProxy,
                                       final ControlledProcessState processState, final NewStepHandler prepareStep) {
        super(NewOperationContext.Type.HOST, configurationPersister, processState, DomainDescriptionProviders.ROOT_PROVIDER, prepareStep);
        this.configurationPersister = configurationPersister;
        this.environment = environment;
        this.domainModelProxy = domainModelProxy;
    }

    @Override
    protected ModelNode createCoreModel() {
        final ModelNode coreModel = new ModelNode();
        DomainModelUtil.updateCoreModel(coreModel);
        return coreModel;
    }

    @Override
    protected void initModel(ModelNodeRegistration rootRegistration) {
        NewHostModelUtil.createHostRegistry(rootRegistration, configurationPersister, environment, domainModelProxy);
    }

    // See superclass start. This method is invoked from a separate non-MSC thread after start. So we can do a fair
    // bit of stuff
    @Override
    protected void boot(final BootContext context) throws ConfigurationPersistenceException {
        super.boot(context); // This parses the host.xml and invokes all ops
        // TODO access the model and see if we need to register with the master.
        //  if so,
        // 1) register with the master
        // 2) get back the domain model, somehow store it (perhaps using an op invoked by the DC?)
        // if not,
        // parse the domain.xml and run the steps
        // then, execute ops to start the servers (do this whether or not we are master)
    }
}
