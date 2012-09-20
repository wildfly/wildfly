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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.EnumSet;

import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.ProcessReloadHandler;
import org.jboss.as.controller.operations.common.SnapshotDeleteHandler;
import org.jboss.as.controller.operations.common.SnapshotListHandler;
import org.jboss.as.controller.operations.common.SnapshotTakeHandler;
import org.jboss.as.controller.operations.common.ValidateOperationHandler;
import org.jboss.as.controller.operations.common.XmlMarshallingHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.controller.resource.InterfaceDefinition;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.operations.DomainServerLifecycleHandlers;
import org.jboss.as.domain.controller.operations.DomainSocketBindingGroupRemoveHandler;
import org.jboss.as.domain.controller.operations.deployment.HostProcessReloadHandler;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition;
import org.jboss.as.domain.management.security.SecurityRealmResourceDefinition;
import org.jboss.as.domain.management.security.WhoAmIOperation;
import org.jboss.as.host.controller.descriptions.HostEnvironmentResourceDescription;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.host.controller.model.host.CoreServiceResourceDefinition;
import org.jboss.as.host.controller.model.host.HostResourceDefinition;
import org.jboss.as.host.controller.model.jvm.JvmResourceDefinition;
import org.jboss.as.host.controller.operations.HostModelRegistrationHandler;
import org.jboss.as.host.controller.operations.HostShutdownHandler;
import org.jboss.as.host.controller.operations.HostSpecifiedInterfaceAddHandler;
import org.jboss.as.host.controller.operations.HostSpecifiedInterfaceRemoveHandler;
import org.jboss.as.host.controller.operations.HostXmlMarshallingHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.LocalDomainControllerRemoveHandler;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.host.controller.operations.RemoteDomainControllerAddHandler;
import org.jboss.as.host.controller.operations.RemoteDomainControllerRemoveHandler;
import org.jboss.as.host.controller.operations.StartServersHandler;
import org.jboss.as.host.controller.resources.HttpManagementResourceDefinition;
import org.jboss.as.host.controller.resources.NativeManagementResourceDefinition;
import org.jboss.as.host.controller.resources.ServerConfigResourceDefinition;
import org.jboss.as.platform.mbean.PlatformMBeanResourceRegistrar;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition.Location;
import org.jboss.as.server.controller.resources.VaultResourceDefinition;
import org.jboss.as.server.services.net.SpecifiedInterfaceResolveHandler;
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
                                          final HostModelRegistrar hostModelRegistrar) {

        // Add of the host itself
        final HostModelRegistrationHandler hostModelRegistratorHandler = new HostModelRegistrationHandler(environment, ignoredDomainResourceRegistry, hostModelRegistrar);
        root.registerOperationHandler(HostModelRegistrationHandler.OPERATION_NAME, hostModelRegistratorHandler, hostModelRegistratorHandler, false, OperationEntry.EntryType.PRIVATE);

        // Global operations
        EnumSet<OperationEntry.Flag> flags = EnumSet.of(OperationEntry.Flag.READ_ONLY);
        root.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true, OperationEntry.EntryType.PUBLIC, flags);
        root.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true, OperationEntry.EntryType.PUBLIC, flags);
        root.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true, OperationEntry.EntryType.PUBLIC, flags);
        root.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true, OperationEntry.EntryType.PUBLIC, flags);
        root.registerOperationHandler(READ_CHILDREN_TYPES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_TYPES, CommonProviders.READ_CHILDREN_TYPES_PROVIDER, true, OperationEntry.EntryType.PUBLIC, flags);
        root.registerOperationHandler(READ_CHILDREN_RESOURCES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_RESOURCES, CommonProviders.READ_CHILDREN_RESOURCES_PROVIDER, true, OperationEntry.EntryType.PUBLIC, flags);
        root.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true, OperationEntry.EntryType.PUBLIC, flags);
        root.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true, OperationEntry.EntryType.PUBLIC, flags);
        root.registerOperationHandler(UNDEFINE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.UNDEFINE_ATTRIBUTE, CommonProviders.UNDEFINE_ATTRIBUTE_PROVIDER, true);
        root.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);
        root.registerOperationHandler(ValidateOperationHandler.OPERATION_NAME, ValidateOperationHandler.INSTANCE, ValidateOperationHandler.INSTANCE, false, EntryType.PUBLIC, EnumSet.of(Flag.READ_ONLY));
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

        createHostRegistry(hostName, root, hostRegistration, configurationPersister, environment, runningModeControl, localFileRepository, hostControllerInfo,
                serverInventory, remoteFileRepository, contentRepository, domainController, extensionRegistry, vaultReader, ignoredRegistry, processState, pathManager);
    }

    public static void createHostRegistry(final String hostName,
            final ManagementResourceRegistration rootRegistration,
            final ManagementResourceRegistration hostRegistration, final HostControllerConfigurationPersister configurationPersister,
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
        // Global operations
        EnumSet<OperationEntry.Flag> flags = EnumSet.of(OperationEntry.Flag.READ_ONLY);

        // Host root resource operations
        XmlMarshallingHandler xmh = new HostXmlMarshallingHandler(configurationPersister.getHostPersister(), hostControllerInfo);
        hostRegistration.registerOperationHandler(XmlMarshallingHandler.OPERATION_NAME, xmh, xmh, false, OperationEntry.EntryType.PUBLIC, flags);


        StartServersHandler ssh = new StartServersHandler(environment, serverInventory, runningModeControl);
        hostRegistration.registerOperationHandler(StartServersHandler.OPERATION_NAME, ssh, ssh, false, OperationEntry.EntryType.PRIVATE);

        HostShutdownHandler hsh = new HostShutdownHandler(domainController);
        hostRegistration.registerOperationHandler(HostShutdownHandler.OPERATION_NAME, hsh, hsh, EnumSet.of(Flag.HOST_CONTROLLER_ONLY));

        HostProcessReloadHandler reloadHandler = new HostProcessReloadHandler(HostControllerService.HC_SERVICE_NAME, runningModeControl, processState,
                getResourceDescriptionResolver(), hostControllerInfo);
        hostRegistration.registerOperationHandler(ProcessReloadHandler.OPERATION_NAME, reloadHandler, reloadHandler, EnumSet.of(Flag.HOST_CONTROLLER_ONLY));


        DomainServerLifecycleHandlers.initializeServerInventory(serverInventory);
        DomainSocketBindingGroupRemoveHandler.INSTANCE.initializeServerInventory(serverInventory);

        ValidateOperationHandler validateOperationHandler = hostControllerInfo.isMasterDomainController() ? ValidateOperationHandler.INSTANCE : ValidateOperationHandler.SLAVE_HC_INSTANCE;
        hostRegistration.registerOperationHandler(ValidateOperationHandler.OPERATION_NAME, validateOperationHandler, validateOperationHandler, false, EntryType.PRIVATE, EnumSet.of(Flag.READ_ONLY));

        LocalDomainControllerAddHandler localDcAddHandler = LocalDomainControllerAddHandler.getInstance(rootRegistration, hostControllerInfo,
                configurationPersister, localFileRepository, contentRepository, domainController, extensionRegistry, pathManager);
        hostRegistration.registerOperationHandler(LocalDomainControllerAddHandler.OPERATION_NAME, localDcAddHandler, localDcAddHandler, false);
        hostRegistration.registerOperationHandler(LocalDomainControllerRemoveHandler.OPERATION_NAME, LocalDomainControllerRemoveHandler.INSTANCE, LocalDomainControllerRemoveHandler.INSTANCE, false);
        RemoteDomainControllerAddHandler remoteDcAddHandler = new RemoteDomainControllerAddHandler(rootRegistration, hostControllerInfo,
                configurationPersister, contentRepository, remoteFileRepository, extensionRegistry, ignoredRegistry, pathManager);
        hostRegistration.registerOperationHandler(RemoteDomainControllerAddHandler.OPERATION_NAME, remoteDcAddHandler, remoteDcAddHandler, false);
        hostRegistration.registerOperationHandler(RemoteDomainControllerRemoveHandler.OPERATION_NAME, RemoteDomainControllerRemoveHandler.INSTANCE, RemoteDomainControllerRemoveHandler.INSTANCE, false);


        SnapshotDeleteHandler snapshotDelete = new SnapshotDeleteHandler(configurationPersister.getHostPersister());
        hostRegistration.registerOperationHandler(SnapshotDeleteHandler.OPERATION_NAME, snapshotDelete, snapshotDelete, false);
        SnapshotListHandler snapshotList = new SnapshotListHandler(configurationPersister.getHostPersister());
        hostRegistration.registerOperationHandler(SnapshotListHandler.OPERATION_NAME, snapshotList, snapshotList, false);
        SnapshotTakeHandler snapshotTake = new SnapshotTakeHandler(configurationPersister.getHostPersister());
        hostRegistration.registerOperationHandler(SnapshotTakeHandler.OPERATION_NAME, snapshotTake, snapshotTake, false);

        ignoredRegistry.registerResources(hostRegistration);

        // System Properties
        hostRegistration.registerSubModel(SystemPropertyResourceDefinition.createForDomainOrHost(Location.HOST));

        /////////////////////////////////////////
        // Core Services

        //vault
        hostRegistration.registerSubModel(new VaultResourceDefinition(vaultReader));

        // Central Management
        //ManagementResourceRegistration management = hostRegistration.registerSubModel(PathElement.pathElement(CORE_SERVICE, MANAGEMENT), CommonProviders.MANAGEMENT_WITH_INTERFACES_PROVIDER);
        ManagementResourceRegistration management = hostRegistration.registerSubModel(CoreServiceResourceDefinition.INSTANCE);
        management.registerSubModel(SecurityRealmResourceDefinition.INSTANCE);
        management.registerSubModel(LdapConnectionResourceDefinition.INSTANCE);
        management.registerSubModel(new NativeManagementResourceDefinition(hostControllerInfo));
        management.registerSubModel(new HttpManagementResourceDefinition(hostControllerInfo, environment));

        // Other core services
        // TODO get a DumpServicesHandler that works on the domain
//        ManagementResourceRegistration serviceContainer = hostRegistration.registerSubModel(PathElement.pathElement(CORE_SERVICE, SERVICE_CONTAINER), CommonProviders.SERVICE_CONTAINER_PROVIDER);
//        serviceContainer.registerOperationHandler(DumpServicesHandler.OPERATION_NAME, DumpServicesHandler.INSTANCE, DumpServicesHandler.INSTANCE, false);

        // Platform MBeans
        PlatformMBeanResourceRegistrar.registerPlatformMBeanResources(hostRegistration);

        //host-environment
        hostRegistration.registerSubModel(HostEnvironmentResourceDescription.of(environment));


        // Jvms
        final ManagementResourceRegistration jvms = hostRegistration.registerSubModel(JvmResourceDefinition.GLOBAL);

        //Paths
        hostRegistration.registerSubModel(PathResourceDefinition.createSpecified(pathManager));

        //interface
        //ManagementResourceRegistration interfaces = hostRegistration.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.SPECIFIED_INTERFACE_PROVIDER);
        ManagementResourceRegistration interfaces = hostRegistration.registerSubModel(new InterfaceDefinition(
                new HostSpecifiedInterfaceAddHandler(),
                new HostSpecifiedInterfaceRemoveHandler(),
                true
        ));
        /*HostSpecifiedInterfaceAddHandler hsiah = ;
        interfaces.registerOperationHandler(InterfaceAddHandler.OPERATION_NAME, hsiah, new DefaultResourceAddDescriptionProvider(interfaces, CommonDescriptions.getResourceDescriptionResolver()), false);
        HostSpecifiedInterfaceRemoveHandler sirh =
        interfaces.registerOperationHandler(InterfaceRemoveHandler.OPERATION_NAME, sirh, new DefaultResourceRemoveDescriptionProvider(CommonDescriptions.getResourceDescriptionResolver()), false);
        //InterfaceCriteriaWriteHandler.UPDATE_RUNTIME.register(interfaces);
        */
        interfaces.registerOperationHandler(SpecifiedInterfaceResolveHandler.DEFINITION, SpecifiedInterfaceResolveHandler.INSTANCE);

        //server configurations
        hostRegistration.registerSubModel(new ServerConfigResourceDefinition(serverInventory, pathManager));
    }
}
