/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.subsystem.test;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.validation.OperationValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.model.test.ModelTestModelControllerService;
import org.jboss.as.model.test.StringConfigurationPersister;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class LegacyKernelServicesImpl extends AbstractKernelServicesImpl implements KernelServicesInternal {

    public LegacyKernelServicesImpl(ServiceContainer container, ModelTestModelControllerService controllerService,
            StringConfigurationPersister persister, ManagementResourceRegistration rootRegistration,
            OperationValidator operationValidator, String mainSubsystemName, ExtensionRegistry extensionRegistry,
            ModelVersion legacyModelVersion, boolean successfulBoot, Throwable bootError, boolean registerTransformers) {
        // FIXME LegacyKernelServicesImpl constructor
        super(container, controllerService, persister, rootRegistration, operationValidator, mainSubsystemName,
                extensionRegistry, legacyModelVersion, successfulBoot, bootError, registerTransformers);
    }

    @Override
    public TransformedOperation transformOperation(ModelVersion modelVersion, ModelNode operation)
            throws OperationFailedException {
        //Will throw an error since we are not the main controller
        checkIsMainController();
        return null;
    }

    @Override
    public ModelNode readTransformedModel(ModelVersion modelVersion) {
        //Will throw an error since we are not the main controller
        checkIsMainController();
        return null;
    }

    @Override
    public ModelNode executeOperation(ModelVersion modelVersion, TransformedOperation op) {
        //Will throw an error since we are not the main controller
        checkIsMainController();
        return null;
    }
}
