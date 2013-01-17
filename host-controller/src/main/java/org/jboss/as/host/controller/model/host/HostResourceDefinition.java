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

package org.jboss.as.host.controller.model.host;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
import org.jboss.as.controller.operations.common.ProcessStateAttributeHandler;
import org.jboss.as.controller.operations.common.ResolveExpressionHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationRemoveHandler;
import org.jboss.as.controller.operations.common.SnapshotDeleteHandler;
import org.jboss.as.controller.operations.common.SnapshotListHandler;
import org.jboss.as.controller.operations.common.SnapshotTakeHandler;
import org.jboss.as.controller.operations.common.ValidateAddressOperationHandler;
import org.jboss.as.controller.operations.common.ValidateOperationHandler;
import org.jboss.as.controller.operations.common.XmlMarshallingHandler;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.resource.InterfaceDefinition;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.operations.DomainServerLifecycleHandlers;
import org.jboss.as.domain.controller.operations.DomainSocketBindingGroupRemoveHandler;
import org.jboss.as.domain.controller.operations.deployment.HostProcessReloadHandler;
import org.jboss.as.domain.management.connections.ldap.LdapConnectionResourceDefinition;
import org.jboss.as.domain.management.security.SecurityRealmResourceDefinition;
import org.jboss.as.host.controller.DirectoryGrouping;
import org.jboss.as.host.controller.HostControllerConfigurationPersister;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.HostControllerService;
import org.jboss.as.host.controller.HostModelUtil;
import org.jboss.as.host.controller.HostRunningModeControl;
import org.jboss.as.host.controller.ServerInventory;
import org.jboss.as.host.controller.descriptions.HostEnvironmentResourceDefinition;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.host.controller.model.jvm.JvmResourceDefinition;
import org.jboss.as.host.controller.operations.HostShutdownHandler;
import org.jboss.as.host.controller.operations.HostSpecifiedInterfaceAddHandler;
import org.jboss.as.host.controller.operations.HostSpecifiedInterfaceRemoveHandler;
import org.jboss.as.host.controller.operations.HostXmlMarshallingHandler;
import org.jboss.as.host.controller.operations.IsMasterHandler;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.host.controller.operations.ResolveExpressionOnHostHandler;
import org.jboss.as.host.controller.operations.StartServersHandler;
import org.jboss.as.host.controller.resources.HttpManagementResourceDefinition;
import org.jboss.as.host.controller.resources.NativeManagementResourceDefinition;
import org.jboss.as.host.controller.resources.ServerConfigResourceDefinition;
import org.jboss.as.platform.mbean.PlatformMBeanResourceRegistrar;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.controller.resources.ServerRootResourceDefinition;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition;
import org.jboss.as.server.controller.resources.VaultResourceDefinition;
import org.jboss.as.server.operations.RunningModeReadHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceResolveHandler;
import org.jboss.as.server.services.security.AbstractVaultReader;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class HostResourceDefinition extends SimpleResourceDefinition {


    public static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.NAME, ModelType.STRING)
            .setAllowNull(true)
            .setMinSize(1)
            .build();

    static final SimpleAttributeDefinition PRODUCT_NAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PRODUCT_NAME, ModelType.STRING)
            .setAllowNull(true)
            .setMinSize(1)
            .build();

    static final SimpleAttributeDefinition RELEASE_VERSION = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RELEASE_VERSION, ModelType.STRING)
            .setAllowNull(true)
            .setMinSize(1)
            .build();

    static final SimpleAttributeDefinition RELEASE_CODENAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RELEASE_CODENAME, ModelType.STRING)
            .setAllowNull(true)
            .setMinSize(1)
            .build();
    static final SimpleAttributeDefinition PRODUCT_VERSION = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PRODUCT_VERSION, ModelType.STRING)
            .setAllowNull(true)
            .setMinSize(1)
            .build();
    static final SimpleAttributeDefinition MANAGEMENT_MAJOR_VERSION = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION, ModelType.INT)
            .setMinSize(1)
            .build();
    static final SimpleAttributeDefinition MANAGEMENT_MINOR_VERSION = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION, ModelType.INT)
            .setMinSize(1)
            .build();
    static final SimpleAttributeDefinition MANAGEMENT_MICRO_VERSION = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION, ModelType.INT)
            .setMinSize(1)
            .build();
    //This is just there for bw compatibility, it had no read handler before this change
    static final SimpleAttributeDefinition SERVER_STATE = new SimpleAttributeDefinitionBuilder("server-state", ModelType.STRING)
            .setAllowNull(true)
            .setMinSize(1)
            .setStorageRuntime()
            .build();

    public static final SimpleAttributeDefinition HOST_STATE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.HOST_STATE, ModelType.STRING)
            .setMinSize(1)
            .setStorageRuntime()
            .build();
    public static final SimpleAttributeDefinition DIRECTORY_GROUPING = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.DIRECTORY_GROUPING, ModelType.STRING, true).
            addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES).
            setDefaultValue(DirectoryGrouping.defaultValue().toModelNode()).
            setValidator(EnumValidator.create(DirectoryGrouping.class, true, true)).
            setAllowExpression(true).
            build();
    public static final SimpleAttributeDefinition MASTER = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.MASTER, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setStorageRuntime()
            .setResourceOnly()
            .build();

    public static final SimpleAttributeDefinition REMOTE_DC_HOST = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.HOST, ModelType.STRING)
            .setAllowNull(false)
            .setAllowExpression(true)
            .setMinSize(1)
            .build();
    public static final SimpleAttributeDefinition REMOTE_DC_PORT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PORT, ModelType.INT)
            .setAllowNull(false)
            .setAllowExpression(true)
            .build();

    public static final ObjectTypeAttributeDefinition DC_LOCAL = new ObjectTypeAttributeDefinition.Builder(ModelDescriptionConstants.LOCAL)
            .build();

    public static final ObjectTypeAttributeDefinition DC_REMOTE = new ObjectTypeAttributeDefinition.Builder(ModelDescriptionConstants.REMOTE, REMOTE_DC_HOST, REMOTE_DC_PORT)
            .build();

    public static final ObjectTypeAttributeDefinition DOMAIN_CONTROLLER = new ObjectTypeAttributeDefinition.Builder(ModelDescriptionConstants.DOMAIN_CONTROLLER, DC_LOCAL, DC_REMOTE)
            .setAllowNull(false)
            .build();

    private final HostControllerConfigurationPersister configurationPersister;
    private final HostControllerEnvironment environment;
    private final HostRunningModeControl runningModeControl;
    private final HostFileRepository localFileRepository;
    private final LocalHostControllerInfoImpl hostControllerInfo;
    private final ServerInventory serverInventory;
    private final HostFileRepository remoteFileRepository;
    private final ContentRepository contentRepository;
    private final DomainController domainController;
    private final ExtensionRegistry extensionRegistry;
    private final AbstractVaultReader vaultReader;
    private final IgnoredDomainResourceRegistry ignoredRegistry;
    private final ControlledProcessState processState;
    private final PathManagerService pathManager;

    public HostResourceDefinition(final String hostName,
                                  final HostControllerConfigurationPersister configurationPersister,
                                  final HostControllerEnvironment environment,
                                  final HostRunningModeControl runningModeControl,
                                  final HostFileRepository localFileRepository,
                                  final LocalHostControllerInfoImpl hostControllerInfo,
                                  final ServerInventory serverInventory,
                                  final HostFileRepository remoteFileRepository,
                                  final ContentRepository contentRepository,
                                  final DomainController domainController,
                                  final ExtensionRegistry extensionRegistry,
                                  final AbstractVaultReader vaultReader,
                                  final IgnoredDomainResourceRegistry ignoredRegistry,
                                  final ControlledProcessState processState,
                                  final PathManagerService pathManager) {
        super(PathElement.pathElement(HOST, hostName), HostModelUtil.getResourceDescriptionResolver());
        this.configurationPersister = configurationPersister;
        this.environment = environment;
        this.runningModeControl = runningModeControl;
        this.localFileRepository = localFileRepository;
        this.hostControllerInfo = hostControllerInfo;
        this.serverInventory = serverInventory;
        this.remoteFileRepository = remoteFileRepository;
        this.contentRepository = contentRepository;
        this.domainController = domainController;
        this.extensionRegistry = extensionRegistry;
        this.vaultReader = vaultReader;
        this.ignoredRegistry = ignoredRegistry;
        this.processState = processState;
        this.pathManager = pathManager;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration hostRegistration) {
        super.registerAttributes(hostRegistration);
        hostRegistration.registerReadWriteAttribute(DIRECTORY_GROUPING, null, new ReloadRequiredWriteAttributeHandler(DIRECTORY_GROUPING));
        hostRegistration.registerReadOnlyAttribute(PRODUCT_NAME, null);
        hostRegistration.registerReadOnlyAttribute(SERVER_STATE, null);
        hostRegistration.registerReadOnlyAttribute(RELEASE_VERSION, null);
        hostRegistration.registerReadOnlyAttribute(RELEASE_CODENAME, null);
        hostRegistration.registerReadOnlyAttribute(PRODUCT_VERSION, null);
        hostRegistration.registerReadOnlyAttribute(MANAGEMENT_MAJOR_VERSION, null);
        hostRegistration.registerReadOnlyAttribute(MANAGEMENT_MINOR_VERSION, null);
        hostRegistration.registerReadOnlyAttribute(MANAGEMENT_MICRO_VERSION, null);
        hostRegistration.registerReadOnlyAttribute(MASTER, IsMasterHandler.INSTANCE);
        hostRegistration.registerReadOnlyAttribute(DOMAIN_CONTROLLER, null);
        hostRegistration.registerReadOnlyAttribute(ServerRootResourceDefinition.NAMESPACES, null);
        hostRegistration.registerReadOnlyAttribute(ServerRootResourceDefinition.SCHEMA_LOCATIONS, null);
        hostRegistration.registerReadWriteAttribute(HostResourceDefinition.NAME, environment.getProcessNameReadHandler(), environment.getProcessNameWriteHandler());
        hostRegistration.registerReadOnlyAttribute(HostResourceDefinition.HOST_STATE, new ProcessStateAttributeHandler(processState));
        hostRegistration.registerReadOnlyAttribute(ServerRootResourceDefinition.RUNNING_MODE, new RunningModeReadHandler(runningModeControl));
    }


    @Override
    public void registerOperations(ManagementResourceRegistration hostRegistration) {
        super.registerOperations(hostRegistration);
        hostRegistration.registerOperationHandler(NamespaceAddHandler.DEFINITION, NamespaceAddHandler.INSTANCE);
        hostRegistration.registerOperationHandler(NamespaceRemoveHandler.DEFINITION, NamespaceRemoveHandler.INSTANCE);
        hostRegistration.registerOperationHandler(SchemaLocationAddHandler.DEFINITION, SchemaLocationAddHandler.INSTANCE);
        hostRegistration.registerOperationHandler(SchemaLocationRemoveHandler.DEFINITION, SchemaLocationRemoveHandler.INSTANCE);


        hostRegistration.registerOperationHandler(ValidateAddressOperationHandler.DEFINITION, ValidateAddressOperationHandler.INSTANCE);

        hostRegistration.registerOperationHandler(ResolveExpressionHandler.DEFINITION, ResolveExpressionHandler.INSTANCE);
        hostRegistration.registerOperationHandler(ResolveExpressionOnHostHandler.DEFINITION, ResolveExpressionOnHostHandler.INSTANCE);
        hostRegistration.registerOperationHandler(SpecifiedInterfaceResolveHandler.DEFINITION, SpecifiedInterfaceResolveHandler.INSTANCE);

        XmlMarshallingHandler xmh = new HostXmlMarshallingHandler(configurationPersister.getHostPersister(), hostControllerInfo);
        hostRegistration.registerOperationHandler(XmlMarshallingHandler.DEFINITION, xmh);


        StartServersHandler ssh = new StartServersHandler(environment, serverInventory, runningModeControl);
        hostRegistration.registerOperationHandler(StartServersHandler.DEFINITION, ssh);

        HostShutdownHandler hsh = new HostShutdownHandler(domainController);
        hostRegistration.registerOperationHandler(HostShutdownHandler.DEFINITION, hsh);

        HostProcessReloadHandler reloadHandler = new HostProcessReloadHandler(HostControllerService.HC_SERVICE_NAME, runningModeControl, processState);
        hostRegistration.registerOperationHandler(HostProcessReloadHandler.getDefinition(hostControllerInfo), reloadHandler);


        DomainServerLifecycleHandlers.initializeServerInventory(serverInventory);
        DomainSocketBindingGroupRemoveHandler.INSTANCE.initializeServerInventory(serverInventory);

        ValidateOperationHandler validateOperationHandler = hostControllerInfo.isMasterDomainController() ? ValidateOperationHandler.INSTANCE : ValidateOperationHandler.SLAVE_HC_INSTANCE;
        hostRegistration.registerOperationHandler(ValidateOperationHandler.DEFINITION_PRIVATE, validateOperationHandler);


        SnapshotDeleteHandler snapshotDelete = new SnapshotDeleteHandler(configurationPersister.getHostPersister());
        hostRegistration.registerOperationHandler(SnapshotDeleteHandler.DEFINITION, snapshotDelete);
        SnapshotListHandler snapshotList = new SnapshotListHandler(configurationPersister.getHostPersister());
        hostRegistration.registerOperationHandler(SnapshotListHandler.DEFINITION, snapshotList);
        SnapshotTakeHandler snapshotTake = new SnapshotTakeHandler(configurationPersister.getHostPersister());
        hostRegistration.registerOperationHandler(SnapshotTakeHandler.DEFINITION, snapshotTake);

        ignoredRegistry.registerResources(hostRegistration);


        // Platform MBeans
        PlatformMBeanResourceRegistrar.registerPlatformMBeanResources(hostRegistration);
    }


    @Override
    public void registerChildren(ManagementResourceRegistration hostRegistration) {
        super.registerChildren(hostRegistration);


        // System Properties
        hostRegistration.registerSubModel(SystemPropertyResourceDefinition.createForDomainOrHost(SystemPropertyResourceDefinition.Location.HOST));

        /////////////////////////////////////////
        // Core Services

        //vault
        hostRegistration.registerSubModel(new VaultResourceDefinition(vaultReader));

        // Central Management
        ManagementResourceRegistration management = hostRegistration.registerSubModel(CoreServiceResourceDefinition.INSTANCE);
        management.registerSubModel(SecurityRealmResourceDefinition.INSTANCE);
        management.registerSubModel(LdapConnectionResourceDefinition.INSTANCE);
        management.registerSubModel(new NativeManagementResourceDefinition(hostControllerInfo));
        management.registerSubModel(new HttpManagementResourceDefinition(hostControllerInfo, environment));

        // Other core services
        // TODO get a DumpServicesHandler that works on the domain
        //        ManagementResourceRegistration serviceContainer = hostRegistration.registerSubModel(PathElement.pathElement(CORE_SERVICE, SERVICE_CONTAINER), CommonProviders.SERVICE_CONTAINER_PROVIDER);
        //        serviceContainer.registerOperationHandler(DumpServicesHandler.OPERATION_NAME, DumpServicesHandler.INSTANCE, DumpServicesHandler.INSTANCE, false);

        //host-environment
        hostRegistration.registerSubModel(HostEnvironmentResourceDefinition.of(environment));


        // Jvms
        final ManagementResourceRegistration jvms = hostRegistration.registerSubModel(JvmResourceDefinition.GLOBAL);

        //Paths
        hostRegistration.registerSubModel(PathResourceDefinition.createSpecified(pathManager));

        //interface
        ManagementResourceRegistration interfaces = hostRegistration.registerSubModel(new InterfaceDefinition(
                new HostSpecifiedInterfaceAddHandler(),
                new HostSpecifiedInterfaceRemoveHandler(),
                true
        ));
        interfaces.registerOperationHandler(SpecifiedInterfaceResolveHandler.DEFINITION, SpecifiedInterfaceResolveHandler.INSTANCE);

        //server configurations
        hostRegistration.registerSubModel(new ServerConfigResourceDefinition(serverInventory, pathManager));

    }
}
