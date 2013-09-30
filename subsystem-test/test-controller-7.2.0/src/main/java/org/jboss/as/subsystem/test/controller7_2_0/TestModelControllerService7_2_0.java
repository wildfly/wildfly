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

package org.jboss.as.subsystem.test.controller7_2_0;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jboss.as.controller.BootContext;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.model.test.ModelTestModelControllerService;
import org.jboss.as.model.test.ModelTestOperationValidatorFilter;
import org.jboss.as.model.test.StringConfigurationPersister;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.controller.resources.ServerDeploymentResourceDefinition;
import org.jboss.as.server.operations.RootResourceHack;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.AdditionalInitializationUtil;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.dmr.ModelNode;
import org.jboss.vfs.VirtualFile;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class TestModelControllerService7_2_0 extends ModelTestModelControllerService {

    private final ExtensionRegistry extensionRegistry;
    private final AdditionalInitialization additionalInit;
    private final ControllerInitializer controllerInitializer;
    private final Extension mainExtension;

    TestModelControllerService7_2_0(final Extension mainExtension, final ControllerInitializer controllerInitializer,
                                    final AdditionalInitialization additionalInit, final RunningModeControl runningModeControl, final ExtensionRegistry extensionRegistry,
                                    final StringConfigurationPersister persister, final ModelTestOperationValidatorFilter validateOpsFilter, final boolean registerTransformers) {
        super(AdditionalInitializationUtil.getProcessType(additionalInit), runningModeControl, extensionRegistry.getTransformerRegistry(), persister, validateOpsFilter,
                ModelTestModelControllerService.DESC_PROVIDER, new ControlledProcessState(true), Controller72x.INSTANCE);
        this.extensionRegistry = extensionRegistry;
        this.additionalInit = additionalInit;
        this.controllerInitializer = controllerInitializer;
        this.mainExtension = mainExtension;
    }

    @Override
    protected void initExtraModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        rootResource.getModel().get(SUBSYSTEM);

        ManagementResourceRegistration deployments = rootRegistration.registerSubModel(ServerDeploymentResourceDefinition.create(new ContentRepository() {

            @Override
            public boolean syncContent(byte[] hash) {
                return false;
            }

            @Override
            public void removeContent(byte[] hash, Object reference) {
            }

            @Override
            public boolean hasContent(byte[] hash) {
                return false;
            }

            @Override
            public VirtualFile getContent(byte[] hash) {
                return null;
            }

            @Override
            public void addContentReference(byte[] hash, Object reference) {
            }

            @Override
            public byte[] addContent(InputStream stream) throws IOException {
                return null;
            }
        }, null));

        //Hack to be able to access the registry for the jmx facade

        rootRegistration.registerOperationHandler(RootResourceHack.DEFINITION, RootResourceHack.INSTANCE);

        extensionRegistry.setSubsystemParentResourceRegistrations(rootRegistration, deployments);
        AdditionalInitializationUtil.doExtraInitialization(additionalInit, controllerInitializer, extensionRegistry, rootResource, rootRegistration);
    }


    @Override
    protected void boot(BootContext context) throws ConfigurationPersistenceException {
        try {
            super.boot(context);
        } finally {
            countdownDoneLatch();
        }
    }

    @Override
    protected void preBoot(List<ModelNode> bootOperations, boolean rollbackOnRuntimeFailure) {
        mainExtension.initialize(extensionRegistry.getExtensionContext("Test", true));
    }

}
