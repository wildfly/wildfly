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
package org.jboss.as.domain.controller.resources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN;

import java.util.EnumSet;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationRemoveHandler;
import org.jboss.as.controller.operations.common.SnapshotDeleteHandler;
import org.jboss.as.controller.operations.common.SnapshotListHandler;
import org.jboss.as.controller.operations.common.SnapshotTakeHandler;
import org.jboss.as.controller.operations.common.ValidateAddressOperationHandler;
import org.jboss.as.controller.operations.common.XmlMarshallingHandler;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.transform.SubsystemDescriptionDump;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.descriptions.DomainRootDescription;
import org.jboss.as.domain.controller.operations.ApplyExtensionsHandler;
import org.jboss.as.domain.controller.operations.ApplyRemoteMasterDomainModelHandler;
import org.jboss.as.domain.controller.operations.DomainServerLifecycleHandlers;
import org.jboss.as.domain.controller.operations.LocalHostNameOperationHandler;
import org.jboss.as.domain.controller.operations.ProcessTypeHandler;
import org.jboss.as.domain.controller.operations.ResolveExpressionOnDomainHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentFullReplaceHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadBytesHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadStreamAttachmentHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadURLHandler;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironment.LaunchType;
import org.jboss.as.server.controller.descriptions.ServerDescriptionConstants;
import org.jboss.as.server.controller.resources.DeploymentAttributes;
import org.jboss.as.server.operations.LaunchTypeHandler;
import org.jboss.as.server.operations.ServerVersionOperations.DefaultEmptyListAttributeHandler;
import org.jboss.as.server.operations.ServerVersionOperations.ManagementVersionAttributeHandler;
import org.jboss.as.server.operations.ServerVersionOperations.ProductInfoAttributeHandler;
import org.jboss.as.server.operations.ServerVersionOperations.ReleaseVersionAttributeHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainRootDefinition extends SimpleResourceDefinition {

    private static final ParameterValidator NOT_NULL_STRING_LENGTH_ONE_VALIDATOR = new StringLengthValidator(1, false, false);

    public static final AttributeDefinition NAMESPACES = new SimpleMapAttributeDefinition.Builder(
            new PropertiesAttributeDefinition.Builder(ModelDescriptionConstants.NAMESPACES, false)
            .build()
        )
        .build();

    public static final AttributeDefinition SCHEMA_LOCATIONS = new SimpleMapAttributeDefinition.Builder(
        new PropertiesAttributeDefinition.Builder(ModelDescriptionConstants.SCHEMA_LOCATIONS, false).build()
        )
        .build();
    public static final SimpleAttributeDefinition NAME = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.NAME, ModelType.STRING, true)
        .setValidator(new StringLengthValidator(1, true))
        .setDefaultValue(new ModelNode("Unnamed Domain"))
        .build();
    public static final SimpleAttributeDefinition RELEASE_VERSION = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.RELEASE_VERSION, ModelType.STRING, false)
        .setValidator(NOT_NULL_STRING_LENGTH_ONE_VALIDATOR)
        .build();
    public static final SimpleAttributeDefinition RELEASE_CODENAME = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.RELEASE_CODENAME, ModelType.STRING, false)
        .setValidator(NOT_NULL_STRING_LENGTH_ONE_VALIDATOR)
        .build();
    public static final SimpleAttributeDefinition PRODUCT_NAME = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.PRODUCT_NAME, ModelType.STRING, true)
        .setValidator(NOT_NULL_STRING_LENGTH_ONE_VALIDATOR)
        .build();
    public static final SimpleAttributeDefinition PRODUCT_VERSION = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.PRODUCT_VERSION, ModelType.STRING, true)
        .setValidator(NOT_NULL_STRING_LENGTH_ONE_VALIDATOR)
        .build();
    public static final SimpleAttributeDefinition MANAGEMENT_MAJOR_VERSION = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION, ModelType.INT, false)
        .setValidator(new IntRangeValidator(1))
        .build();
    public static final SimpleAttributeDefinition MANAGEMENT_MINOR_VERSION = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION, ModelType.INT, false)
        .setValidator(new IntRangeValidator(0))
        .build();
    public static final SimpleAttributeDefinition MANAGEMENT_MICRO_VERSION = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION, ModelType.INT, false)
        .setValidator(new IntRangeValidator(0))
        .build();
    public static final SimpleAttributeDefinition PROCESS_TYPE = SimpleAttributeDefinitionBuilder.create(ServerDescriptionConstants.PROCESS_TYPE, ModelType.STRING)
            .setStorageRuntime()
            .setValidator(NOT_NULL_STRING_LENGTH_ONE_VALIDATOR)
            .build();
    public static final SimpleAttributeDefinition LAUNCH_TYPE = SimpleAttributeDefinitionBuilder.create(ServerDescriptionConstants.LAUNCH_TYPE, ModelType.STRING)
            .setValidator(new EnumValidator<LaunchType>(LaunchType.class, false, false))
            .setStorageRuntime()
            .build();
    public static final SimpleAttributeDefinition LOCAL_HOST_NAME = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.LOCAL_HOST_NAME, ModelType.STRING, true)
            .setValidator(NOT_NULL_STRING_LENGTH_ONE_VALIDATOR)
            .setStorageRuntime()
            .build();


    private final LocalHostControllerInfo hostControllerInfo;
    private final HostControllerEnvironment environment;
    private final ExtensibleConfigurationPersister configurationPersister;
    private final ContentRepository contentRepo;
    private final HostFileRepository fileRepository;
    private final boolean isMaster;
    private final ExtensionRegistry extensionRegistry;
    private final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry;


    public DomainRootDefinition(final HostControllerEnvironment environment,
            final ExtensibleConfigurationPersister configurationPersister, final ContentRepository contentRepo,
            final HostFileRepository fileRepository, final boolean isMaster,
            final LocalHostControllerInfo hostControllerInfo,
            final ExtensionRegistry extensionRegistry, final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry,
            final PathManagerService pathManager) {
        super(PathElement.pathElement("this-will-be-ignored-since-we-are-root"), DomainRootDescription.getResourceDescriptionResolver(DOMAIN, false));
        this.isMaster = isMaster;
        this.environment = environment;
        this.hostControllerInfo = hostControllerInfo;
        this.configurationPersister = configurationPersister;
        this.contentRepo = contentRepo;
        this.fileRepository = fileRepository;
        this.extensionRegistry = extensionRegistry;
        this.ignoredDomainResourceRegistry = ignoredDomainResourceRegistry;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadWriteAttribute(NAME, null, new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(NAME));

        resourceRegistration.registerReadOnlyAttribute(PROCESS_TYPE, isMaster ? ProcessTypeHandler.MASTER : ProcessTypeHandler.SLAVE);
        resourceRegistration.registerReadOnlyAttribute(LAUNCH_TYPE, new LaunchTypeHandler(ServerEnvironment.LaunchType.DOMAIN));
        resourceRegistration.registerReadOnlyAttribute(LOCAL_HOST_NAME, new LocalHostNameOperationHandler(hostControllerInfo));

        resourceRegistration.registerReadOnlyAttribute(MANAGEMENT_MAJOR_VERSION, ManagementVersionAttributeHandler.INSTANCE);
        resourceRegistration.registerReadOnlyAttribute(MANAGEMENT_MINOR_VERSION, ManagementVersionAttributeHandler.INSTANCE);
        resourceRegistration.registerReadOnlyAttribute(MANAGEMENT_MICRO_VERSION, ManagementVersionAttributeHandler.INSTANCE);

        resourceRegistration.registerReadOnlyAttribute(RELEASE_VERSION, ReleaseVersionAttributeHandler.INSTANCE);
        resourceRegistration.registerReadOnlyAttribute(RELEASE_CODENAME, ReleaseVersionAttributeHandler.INSTANCE);

        ProductInfoAttributeHandler infoHandler = new ProductInfoAttributeHandler(environment != null ? environment.getProductConfig() : null);
        resourceRegistration.registerReadOnlyAttribute(PRODUCT_NAME, infoHandler);
        resourceRegistration.registerReadOnlyAttribute(PRODUCT_VERSION, infoHandler);

        resourceRegistration.registerReadOnlyAttribute(NAMESPACES, DefaultEmptyListAttributeHandler.INSTANCE);
        resourceRegistration.registerReadOnlyAttribute(SCHEMA_LOCATIONS, DefaultEmptyListAttributeHandler.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        final EnumSet<OperationEntry.Flag> readOnly = EnumSet.of(OperationEntry.Flag.READ_ONLY);
        final EnumSet<OperationEntry.Flag> masterOnly = EnumSet.of(OperationEntry.Flag.MASTER_HOST_CONTROLLER_ONLY);

        // Other root resource operations
        XmlMarshallingHandler xmh = new XmlMarshallingHandler(configurationPersister);
        resourceRegistration.registerOperationHandler(XmlMarshallingHandler.OPERATION_NAME, xmh, xmh, false, OperationEntry.EntryType.PUBLIC, readOnly);

        resourceRegistration.registerOperationHandler(NamespaceAddHandler.OPERATION_NAME, NamespaceAddHandler.INSTANCE, NamespaceAddHandler.INSTANCE, false);
        resourceRegistration.registerOperationHandler(NamespaceRemoveHandler.OPERATION_NAME, NamespaceRemoveHandler.INSTANCE, NamespaceRemoveHandler.INSTANCE, false);
        resourceRegistration.registerOperationHandler(SchemaLocationAddHandler.OPERATION_NAME, SchemaLocationAddHandler.INSTANCE, SchemaLocationAddHandler.INSTANCE, false);
        resourceRegistration.registerOperationHandler(SchemaLocationRemoveHandler.OPERATION_NAME, SchemaLocationRemoveHandler.INSTANCE, SchemaLocationRemoveHandler.INSTANCE, false);

        if (isMaster) {
            DeploymentUploadURLHandler.registerMaster(resourceRegistration, contentRepo);
            DeploymentUploadStreamAttachmentHandler.registerMaster(resourceRegistration, contentRepo);
            DeploymentUploadBytesHandler.registerMaster(resourceRegistration, contentRepo);

            SnapshotDeleteHandler snapshotDelete = new SnapshotDeleteHandler(configurationPersister);
            resourceRegistration.registerOperationHandler(SnapshotDeleteHandler.OPERATION_NAME, snapshotDelete, snapshotDelete, false, EntryType.PUBLIC, masterOnly);
            SnapshotListHandler snapshotList = new SnapshotListHandler(configurationPersister);
            resourceRegistration.registerOperationHandler(SnapshotListHandler.OPERATION_NAME, snapshotList, snapshotList, false, EntryType.PUBLIC, masterOnly);
            SnapshotTakeHandler snapshotTake = new SnapshotTakeHandler(configurationPersister);
            resourceRegistration.registerOperationHandler(SnapshotTakeHandler.OPERATION_NAME, snapshotTake, snapshotTake, false, EntryType.PUBLIC, masterOnly);

            final SubsystemDescriptionDump dumper = new SubsystemDescriptionDump(extensionRegistry);
            resourceRegistration.registerOperationHandler(SubsystemDescriptionDump.DEFINITION, dumper, false);
        } else {
            DeploymentUploadURLHandler.registerSlave(resourceRegistration);
            DeploymentUploadStreamAttachmentHandler.registerSlave(resourceRegistration);
            DeploymentUploadBytesHandler.registerSlave(resourceRegistration);

            final ApplyExtensionsHandler aexh = new ApplyExtensionsHandler(extensionRegistry, hostControllerInfo, ignoredDomainResourceRegistry);
            resourceRegistration.registerOperationHandler(ApplyExtensionsHandler.OPERATION_NAME, aexh, aexh, false, EntryType.PRIVATE);

            ApplyRemoteMasterDomainModelHandler armdmh = new ApplyRemoteMasterDomainModelHandler(fileRepository,
                    contentRepo, hostControllerInfo, ignoredDomainResourceRegistry);
            resourceRegistration.registerOperationHandler(ApplyRemoteMasterDomainModelHandler.OPERATION_NAME, armdmh, armdmh, false, OperationEntry.EntryType.PRIVATE);
        }
        resourceRegistration.registerOperationHandler(DeploymentAttributes.FULL_REPLACE_DEPLOYMENT_DEFINITION, isMaster ? new DeploymentFullReplaceHandler(contentRepo) : new DeploymentFullReplaceHandler(fileRepository));

        resourceRegistration.registerOperationHandler(ValidateAddressOperationHandler.OPERATION_NAME, ValidateAddressOperationHandler.INSTANCE,
                ValidateAddressOperationHandler.INSTANCE, false, EnumSet.of(OperationEntry.Flag.READ_ONLY));

        resourceRegistration.registerOperationHandler(ResolveExpressionOnDomainHandler.OPERATION_NAME, ResolveExpressionOnDomainHandler.INSTANCE,
                ResolveExpressionOnDomainHandler.INSTANCE, EnumSet.of(OperationEntry.Flag.READ_ONLY, OperationEntry.Flag.DOMAIN_PUSH_TO_SERVERS));

        DomainServerLifecycleHandlers.registerDomainHandlers(resourceRegistration);
    }

    public void initialize(ManagementResourceRegistration resourceRegistration) {
        registerAttributes(resourceRegistration);
        registerOperations(resourceRegistration);
        registerChildren(resourceRegistration);
    }
}
