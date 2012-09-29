/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.host.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;

import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.ValidateOperationHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.management.security.WhoAmIOperation;
import org.jboss.as.host.controller.descriptions.HostEnvironmentResourceDescription;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.host.controller.model.host.HostResourceDefinition;
import org.jboss.as.host.controller.operations.HostModelRegistrationHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerRemoveHandler;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.host.controller.operations.RemoteDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.RemoteDomainControllerRemoveHandler;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.services.security.AbstractVaultReader;

/**
 * Utility for creating the root element and populating the {@link org.jboss.as.controller.registry.ManagementResourceRegistration}
 * for an individual host's portion of the model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:jperkins@jboss.com">James R. Perkins</a>
 */
public class HostModelUtil {

    public static interface HostModelRegistrar {
        void registerHostModel(final String hostName, final ManagementResourceRegistration root);
    }

    public static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(HOST);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), HostEnvironmentResourceDescription.class.getPackage().getName() + ".LocalDescriptions", HostModelUtil.class.getClassLoader(), true, false);
    }


    public static void createRootRegistry(final ManagementResourceRegistration root, final HostControllerEnvironment environment,
                                          final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry,
                                          final HostModelRegistrar hostModelRegistrar, ProcessType processType) {

        // Add of the host itself
        final HostModelRegistrationHandler hostModelRegistratorHandler = new HostModelRegistrationHandler(environment, ignoredDomainResourceRegistry, hostModelRegistrar);
        root.registerOperationHandler(HostModelRegistrationHandler.OPERATION_NAME, hostModelRegistratorHandler, hostModelRegistratorHandler, false, OperationEntry.EntryType.PRIVATE);

        // Global operations
        GlobalOperationHandlers.registerGlobalOperations(root, processType);


        //root.registerOperationHandler(ValidateOperationHandler.DEFINITION, ValidateOperationHandler.INSTANCE);
        root.registerOperationHandler(WhoAmIOperation.OPERATION_NAME, WhoAmIOperation.INSTANCE, WhoAmIOperation.INSTANCE, true);

        // Other root resource operations
        root.registerOperationHandler(CompositeOperationHandler.NAME, CompositeOperationHandler.INSTANCE, CompositeOperationHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
    }

    public static void createHostRegistry(final String hostName,
                                          final ManagementResourceRegistration root, final HostControllerConfigurationPersister configurationPersister,
                                          final HostControllerEnvironment environment, final HostRunningModeControl runningModeControl,
                                          final HostFileRepository localFileRepository,
                                          final LocalHostControllerInfoImpl hostControllerInfo, final ServerInventory serverInventory,
                                          final HostFileRepository remoteFileRepository,
                                          final ContentRepository contentRepository,
                                          final DomainController domainController,
                                          final ExtensionRegistry extensionRegistry,
                                          final AbstractVaultReader vaultReader,
                                          final IgnoredDomainResourceRegistry ignoredRegistry,
                                          final ControlledProcessState processState,
                                          final PathManagerService pathManager) {
        // Add of the host itself
        //ManagementResourceRegistration hostRegistration = root.registerSubModel(PathElement.pathElement(HOST, hostName), HostDescriptionProviders.HOST_ROOT_PROVIDER);
        ManagementResourceRegistration hostRegistration = root.registerSubModel(
                new HostResourceDefinition(hostName, configurationPersister,
                        environment, runningModeControl, localFileRepository,
                        hostControllerInfo, serverInventory, remoteFileRepository,
                        contentRepository, domainController, extensionRegistry,
                        vaultReader, ignoredRegistry, processState, pathManager));

        //TODO See if some of all these parameters can come from domain controller
        LocalDomainControllerAddHandler localDcAddHandler = LocalDomainControllerAddHandler.getInstance(root, hostControllerInfo,
                configurationPersister, localFileRepository, contentRepository, domainController, extensionRegistry, pathManager);
        hostRegistration.registerOperationHandler(LocalDomainControllerAddHandler.OPERATION_NAME, localDcAddHandler, localDcAddHandler, false);
        hostRegistration.registerOperationHandler(LocalDomainControllerRemoveHandler.OPERATION_NAME, LocalDomainControllerRemoveHandler.INSTANCE, LocalDomainControllerRemoveHandler.INSTANCE, false);
        RemoteDomainControllerAddHandler remoteDcAddHandler = new RemoteDomainControllerAddHandler(root, hostControllerInfo, domainController,
                configurationPersister, contentRepository, remoteFileRepository, extensionRegistry, ignoredRegistry, pathManager);
        hostRegistration.registerOperationHandler(RemoteDomainControllerAddHandler.OPERATION_NAME, remoteDcAddHandler, remoteDcAddHandler, false);
        hostRegistration.registerOperationHandler(RemoteDomainControllerRemoveHandler.OPERATION_NAME, RemoteDomainControllerRemoveHandler.INSTANCE, RemoteDomainControllerRemoveHandler.INSTANCE, false);
    }
}
