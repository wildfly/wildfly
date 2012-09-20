package org.jboss.as.host.controller.model.host;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;

import java.util.EnumSet;

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
import org.jboss.as.controller.operations.common.ValidateAddressOperationHandler;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.host.controller.DirectoryGrouping;
import org.jboss.as.host.controller.HostControllerConfigurationPersister;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.HostModelUtil;
import org.jboss.as.host.controller.HostRunningModeControl;
import org.jboss.as.host.controller.ServerInventory;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.host.controller.operations.IsMasterHandler;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.as.host.controller.operations.ResolveExpressionOnHostHandler;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.controller.resources.ServerRootResourceDefinition;
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
            .setAllowNull(true)
            .setMinSize(1)
            .build();
    static final SimpleAttributeDefinition MANAGEMENT_MINOR_VERSION = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION, ModelType.INT)
            .setAllowNull(true)
            .setMinSize(1)
            .build();
    static final SimpleAttributeDefinition MANAGEMENT_MICRO_VERSION = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION, ModelType.INT)
            .setAllowNull(true)
            .setMinSize(1)
            .build();
    static final SimpleAttributeDefinition PROCESS_STATE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PROCESS_STATE, ModelType.STRING)
            .setAllowNull(true)
            .setMinSize(1)
            .build();

    public static final SimpleAttributeDefinition HOST_STATE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.HOST_STATE, ModelType.STRING)
            .setAllowNull(true)
            .setMinSize(1)
            .build();
    public static final SimpleAttributeDefinition DIRECTORY_GROUPING = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.DIRECTORY_GROUPING, ModelType.STRING, true).
            addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES).
            setDefaultValue(DirectoryGrouping.defaultValue().toModelNode()).
            setValidator(EnumValidator.create(DirectoryGrouping.class, true, false)).
            build();
    public static final SimpleAttributeDefinition MASTER = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.MASTER, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .build();

    public static final SimpleAttributeDefinition REMOTE_DC_HOST = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.HOST, ModelType.STRING)
            .setAllowNull(false)
            .setMinSize(1)
            .build();
    public static final SimpleAttributeDefinition REMOTE_DC_PORT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.HOST, ModelType.INT)
            .build();

    public static final ObjectTypeAttributeDefinition DC_LOCAL = new ObjectTypeAttributeDefinition.Builder(ModelDescriptionConstants.LOCAL)
            .setAllowNull(true)
            .build();

    public static final ObjectTypeAttributeDefinition DC_REMOTE = new ObjectTypeAttributeDefinition.Builder(ModelDescriptionConstants.REMOTE, REMOTE_DC_HOST, REMOTE_DC_PORT)
            .setAllowNull(true)
            .build();

    public static final ObjectTypeAttributeDefinition DOMAIN_CONTROLLER = new ObjectTypeAttributeDefinition.Builder(ModelDescriptionConstants.DOMAIN_CONTROLLER, DC_LOCAL, DC_REMOTE)
            .setAllowNull(false)
            .build();

    private final HostControllerEnvironment environment;
    private final HostRunningModeControl runningModeControl;
    private final ControlledProcessState processState;

    public HostResourceDefinition(final String hostName,
            final HostControllerConfigurationPersister configurationPersister,
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
        super(PathElement.pathElement(HOST, hostName), HostModelUtil.getResourceDescriptionResolver());
        this.environment = environment;
        this.runningModeControl = runningModeControl;
        this.processState = processState;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration hostRegistration) {
        super.registerAttributes(hostRegistration);
        hostRegistration.registerReadWriteAttribute(DIRECTORY_GROUPING, null, new ReloadRequiredWriteAttributeHandler(DIRECTORY_GROUPING));
        hostRegistration.registerReadOnlyAttribute(PRODUCT_NAME, null);
        hostRegistration.registerReadOnlyAttribute(PROCESS_STATE, null);
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
        hostRegistration.registerOperationHandler(NamespaceAddHandler.OPERATION_NAME, NamespaceAddHandler.INSTANCE, NamespaceAddHandler.INSTANCE, false);
        hostRegistration.registerOperationHandler(NamespaceRemoveHandler.OPERATION_NAME, NamespaceRemoveHandler.INSTANCE, NamespaceRemoveHandler.INSTANCE, false);
        hostRegistration.registerOperationHandler(SchemaLocationAddHandler.OPERATION_NAME, SchemaLocationAddHandler.INSTANCE, SchemaLocationAddHandler.INSTANCE, false);
        hostRegistration.registerOperationHandler(SchemaLocationRemoveHandler.OPERATION_NAME, SchemaLocationRemoveHandler.INSTANCE, SchemaLocationRemoveHandler.INSTANCE, false);


        hostRegistration.registerOperationHandler(ValidateAddressOperationHandler.OPERATION_NAME, ValidateAddressOperationHandler.INSTANCE,
                ValidateAddressOperationHandler.INSTANCE, false, EnumSet.of(OperationEntry.Flag.READ_ONLY));

        hostRegistration.registerOperationHandler(ResolveExpressionHandler.OPERATION_NAME, ResolveExpressionHandler.INSTANCE,
                ResolveExpressionHandler.INSTANCE, EnumSet.of(OperationEntry.Flag.READ_ONLY));
        hostRegistration.registerOperationHandler(ResolveExpressionOnHostHandler.OPERATION_NAME, ResolveExpressionOnHostHandler.INSTANCE,
                ResolveExpressionOnHostHandler.INSTANCE, EnumSet.of(OperationEntry.Flag.READ_ONLY, OperationEntry.Flag.DOMAIN_PUSH_TO_SERVERS));
        hostRegistration.registerOperationHandler(SpecifiedInterfaceResolveHandler.DEFINITION, SpecifiedInterfaceResolveHandler.INSTANCE);
    }
}
