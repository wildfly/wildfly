/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.model.test.ModelTestModelControllerService;
import org.jboss.as.model.test.OperationValidation;
import org.jboss.as.model.test.StringConfigurationPersister;
import org.jboss.as.server.DeployerChainAddHandler;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironment.LaunchType;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.as.server.operations.RootResourceHack;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class TestModelControllerService extends ModelTestModelControllerService {
    private final Extension mainExtension;
    private final AdditionalInitialization additionalInit;
    private final ControllerInitializer controllerInitializer;
    private final ExtensionRegistry extensionRegistry;
    private final RunningModeControl runningModeControl;

    protected TestModelControllerService(final Extension mainExtension, final ControllerInitializer controllerInitializer,
            final AdditionalInitialization additionalInit, RunningModeControl runningModeControl, final ExtensionRegistry extensionRegistry,
            final StringConfigurationPersister persister, boolean validateOps) {
        super(additionalInit.getProcessType(), runningModeControl, extensionRegistry.getTransformerRegistry(), persister, validateOps ? OperationValidation.EXIT_ON_VALIDATION_ERROR : OperationValidation.NONE, new ControlledProcessState(true));
        this.mainExtension = mainExtension;
        this.additionalInit = additionalInit;
        this.controllerInitializer = controllerInitializer;
        this.extensionRegistry = extensionRegistry;
        this.runningModeControl = runningModeControl;
    }

    static TestModelControllerService create(final Extension mainExtension, final ControllerInitializer controllerInitializer,
            final AdditionalInitialization additionalInit, final ExtensionRegistry extensionRegistry,
            final StringConfigurationPersister persister, boolean validateOps) {
        return new TestModelControllerService(mainExtension, controllerInitializer, additionalInit, new RunningModeControl(additionalInit.getRunningMode()), extensionRegistry, persister, validateOps);
    }

    @Override
    protected void initExtraModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        rootResource.getModel().get(SUBSYSTEM);

        ManagementResourceRegistration deployments = rootRegistration.registerSubModel(PathElement.pathElement(DEPLOYMENT), ServerDescriptionProviders.DEPLOYMENT_PROVIDER);

        //Hack to be able to access the registry for the jmx facade
        rootRegistration.registerOperationHandler(RootResourceHack.NAME, RootResourceHack.INSTANCE, RootResourceHack.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        extensionRegistry.setSubsystemParentResourceRegistrations(rootRegistration, deployments);
        controllerInitializer.setTestModelControllerService(this);
        controllerInitializer.initializeModel(rootResource, rootRegistration);
        additionalInit.initializeExtraSubystemsAndModel(extensionRegistry, rootResource, rootRegistration);

    }

    @Override
    protected void preBoot(List<ModelNode> bootOperations, boolean rollbackOnRuntimeFailure) {
        mainExtension.initialize(extensionRegistry.getExtensionContext("Test"));
    }

    protected void postBoot() {
        DeployerChainAddHandler.INSTANCE.clearDeployerMap();
    }

    private void delete(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                delete(child);
            }
        }
        file.delete();
    }

    ServerEnvironment getServerEnvironment() {
            Properties props = new Properties();
            File home = new File("target/jbossas");
            delete(home);
            home.mkdir();
            props.put(ServerEnvironment.HOME_DIR, home.getAbsolutePath());

            File standalone = new File(home, "standalone");
            standalone.mkdir();
            props.put(ServerEnvironment.SERVER_BASE_DIR, standalone.getAbsolutePath());

            File configuration = new File(standalone, "configuration");
            configuration.mkdir();
            props.put(ServerEnvironment.SERVER_CONFIG_DIR, configuration.getAbsolutePath());

            File xml = new File(configuration, "standalone.xml");
            try {
                xml.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            props.put(ServerEnvironment.JBOSS_SERVER_DEFAULT_CONFIG, "standalone.xml");

            return new ServerEnvironment(null, props, new HashMap<String, String>(), "standalone.xml", null, LaunchType.STANDALONE, runningModeControl.getRunningMode(), null);
    }
}
