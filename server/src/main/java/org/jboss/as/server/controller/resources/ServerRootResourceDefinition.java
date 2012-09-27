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
package org.jboss.as.server.controller.resources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;

import java.util.EnumSet;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
import org.jboss.as.controller.operations.common.ProcessReloadHandler;
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
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.StringLengthValidatingHandler;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.domain.management.security.WhoAmIOperation;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironment.LaunchType;
import org.jboss.as.server.ServerEnvironmentResourceDescription;
import org.jboss.as.server.Services;
import org.jboss.as.server.controller.descriptions.ServerDescriptionConstants;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.as.server.deployment.DeploymentFullReplaceHandler;
import org.jboss.as.server.deployment.DeploymentReplaceHandler;
import org.jboss.as.server.deployment.DeploymentUploadBytesHandler;
import org.jboss.as.server.deployment.DeploymentUploadStreamAttachmentHandler;
import org.jboss.as.server.deployment.DeploymentUploadURLHandler;
import org.jboss.as.server.operations.LaunchTypeHandler;
import org.jboss.as.server.operations.ProcessTypeHandler;
import org.jboss.as.server.operations.RootResourceHack;
import org.jboss.as.server.operations.RunningModeReadHandler;
import org.jboss.as.server.operations.ServerProcessReloadHandler;
import org.jboss.as.server.operations.ServerRestartRequiredHandler;
import org.jboss.as.server.operations.ServerShutdownHandler;
import org.jboss.as.server.operations.ServerVersionOperations.DefaultEmptyListAttributeHandler;
import org.jboss.as.server.operations.ServerVersionOperations.ManagementVersionAttributeHandler;
import org.jboss.as.server.operations.ServerVersionOperations.ProductInfoAttributeHandler;
import org.jboss.as.server.operations.ServerVersionOperations.ReleaseVersionAttributeHandler;
import org.jboss.as.server.services.net.SpecifiedInterfaceResolveHandler;
import org.jboss.as.server.services.security.AbstractVaultReader;
import org.jboss.dmr.ModelType;
/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ServerRootResourceDefinition extends SimpleResourceDefinition {

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
    public static final SimpleAttributeDefinition PROFILE_NAME = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.PROFILE_NAME, ModelType.STRING, false)
            .setValidator(NOT_NULL_STRING_LENGTH_ONE_VALIDATOR)
            .build();
    public static final SimpleAttributeDefinition NULL_PROFILE_NAME = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.PROFILE_NAME, ModelType.STRING, true)
            .build();
    public static final SimpleAttributeDefinition SERVER_STATE = SimpleAttributeDefinitionBuilder.create(ServerDescriptionConstants.PROCESS_STATE, ModelType.STRING)
            .setStorageRuntime()
            .setValidator(NOT_NULL_STRING_LENGTH_ONE_VALIDATOR)
            .build();
    public static final SimpleAttributeDefinition PROCESS_TYPE = SimpleAttributeDefinitionBuilder.create(ServerDescriptionConstants.PROCESS_TYPE, ModelType.STRING)
            .setStorageRuntime()
            .setValidator(NOT_NULL_STRING_LENGTH_ONE_VALIDATOR)
            .build();
    public static final SimpleAttributeDefinition LAUNCH_TYPE = SimpleAttributeDefinitionBuilder.create(ServerDescriptionConstants.LAUNCH_TYPE, ModelType.STRING)
            .setValidator(new EnumValidator<LaunchType>(LaunchType.class, false, false))
            .setStorageRuntime()
            .build();

    private static final EnumSet<Flag> runtimeOnlyFlag = EnumSet.of(Flag.RUNTIME_ONLY);
    public static final AttributeDefinition RUNNING_MODE = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.RUNNING_MODE, ModelType.STRING)
            .setValidator(new EnumValidator(RunningMode.class, false, false))
            .setStorageRuntime()
            .build();

    private final boolean isDomain;
    private final ContentRepository contentRepository;
    private final ExtensibleConfigurationPersister extensibleConfigurationPersister;
    private final ServerEnvironment serverEnvironment;
    private final ControlledProcessState processState;
    private final RunningModeControl runningModeControl;
    private final AbstractVaultReader vaultReader;
    private final ExtensionRegistry extensionRegistry;
    private final boolean parallelBoot;
    private final PathManagerService pathManager;

    public ServerRootResourceDefinition(
            final ContentRepository contentRepository,
            final ExtensibleConfigurationPersister extensibleConfigurationPersister,
            final ServerEnvironment serverEnvironment,
            final ControlledProcessState processState,
            final RunningModeControl runningModeControl,
            final AbstractVaultReader vaultReader,
            final ExtensionRegistry extensionRegistry,
            final boolean parallelBoot,
            final PathManagerService pathManager) {
        super(PathElement.pathElement("this-will-be-ignored-since-we-are-root"), ServerDescriptions.getResourceDescriptionResolver(SERVER, false));
        this.contentRepository = contentRepository;
        this.extensibleConfigurationPersister = extensibleConfigurationPersister;
        this.serverEnvironment = serverEnvironment;
        this.processState = processState;
        this.runningModeControl = runningModeControl;
        this.vaultReader = vaultReader;
        this.extensionRegistry = extensionRegistry;
        this.parallelBoot = parallelBoot;
        this.pathManager = pathManager;

        isDomain = serverEnvironment == null || serverEnvironment.getLaunchType() == LaunchType.DOMAIN;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        GlobalOperationHandlers.registerGlobalOperations(resourceRegistration, ProcessType.STANDALONE_SERVER);

        if (serverEnvironment != null) {
            resourceRegistration.registerOperationHandler(ValidateOperationHandler.OPERATION_NAME, ValidateOperationHandler.INSTANCE, ValidateOperationHandler.INSTANCE);
        } else {
            resourceRegistration.registerOperationHandler(ValidateOperationHandler.OPERATION_NAME, ValidateOperationHandler.INSTANCE, ValidateOperationHandler.INSTANCE, false, runtimeOnlyFlag);
        }

        // Other root resource operations
        resourceRegistration.registerOperationHandler(CompositeOperationHandler.NAME, CompositeOperationHandler.INSTANCE, CompositeOperationHandler.INSTANCE, false, EntryType.PRIVATE);
        XmlMarshallingHandler xmh = new XmlMarshallingHandler(extensibleConfigurationPersister);
        resourceRegistration.registerOperationHandler(XmlMarshallingHandler.DEFINITION, xmh);
        resourceRegistration.registerOperationHandler(NamespaceAddHandler.DEFINITION, NamespaceAddHandler.INSTANCE);
        resourceRegistration.registerOperationHandler(NamespaceRemoveHandler.DEFINITION, NamespaceRemoveHandler.INSTANCE);
        resourceRegistration.registerOperationHandler(SchemaLocationAddHandler.DEFINITION, SchemaLocationAddHandler.INSTANCE);
        resourceRegistration.registerOperationHandler(SchemaLocationRemoveHandler.DEFINITION, SchemaLocationRemoveHandler.INSTANCE);
        resourceRegistration.registerOperationHandler(ValidateAddressOperationHandler.OPERATION_NAME, ValidateAddressOperationHandler.INSTANCE,
                ValidateAddressOperationHandler.INSTANCE, false, EnumSet.of(OperationEntry.Flag.READ_ONLY));

        DeploymentUploadBytesHandler.register(resourceRegistration, contentRepository);
        DeploymentUploadURLHandler.register(resourceRegistration, contentRepository);
        DeploymentUploadStreamAttachmentHandler.register(resourceRegistration, contentRepository);
        resourceRegistration.registerOperationHandler(DeploymentAttributes.REPLACE_DEPLOYMENT_DEFINITION, DeploymentReplaceHandler.create(contentRepository, vaultReader));
        resourceRegistration.registerOperationHandler(DeploymentAttributes.FULL_REPLACE_DEPLOYMENT_DEFINITION, DeploymentFullReplaceHandler.create(contentRepository, vaultReader));

        if (!isDomain) {
            SnapshotDeleteHandler snapshotDelete = new SnapshotDeleteHandler(extensibleConfigurationPersister);
            resourceRegistration.registerOperationHandler(SnapshotDeleteHandler.DEFINITION, snapshotDelete);
            SnapshotListHandler snapshotList = new SnapshotListHandler(extensibleConfigurationPersister);
            resourceRegistration.registerOperationHandler(SnapshotListHandler.DEFINITION, snapshotList);
            SnapshotTakeHandler snapshotTake = new SnapshotTakeHandler(extensibleConfigurationPersister);
            resourceRegistration.registerOperationHandler(SnapshotTakeHandler.DEFINITION, snapshotTake);
        }

        resourceRegistration.registerOperationHandler(ServerRestartRequiredHandler.DEFINITION, ServerRestartRequiredHandler.INSTANCE);

        resourceRegistration.registerOperationHandler(ResolveExpressionHandler.OPERATION_NAME, ResolveExpressionHandler.INSTANCE,
                ResolveExpressionHandler.INSTANCE, EnumSet.of(OperationEntry.Flag.READ_ONLY, OperationEntry.Flag.RUNTIME_ONLY));

        resourceRegistration.registerOperationHandler(SpecifiedInterfaceResolveHandler.DEFINITION, SpecifiedInterfaceResolveHandler.INSTANCE);
        resourceRegistration.registerOperationHandler(WhoAmIOperation.OPERATION_NAME, WhoAmIOperation.INSTANCE, WhoAmIOperation.INSTANCE, true);

        //Hack to be able to access the registry for the jmx facade
        resourceRegistration.registerOperationHandler(RootResourceHack.NAME, RootResourceHack.INSTANCE, RootResourceHack.INSTANCE, false, OperationEntry.EntryType.PRIVATE, runtimeOnlyFlag);

        // Runtime operations
        if (serverEnvironment != null) {
            // Reload op -- does not work on a domain mode server
            if (serverEnvironment.getLaunchType() != ServerEnvironment.LaunchType.DOMAIN) {
                ServerProcessReloadHandler reloadHandler = new ServerProcessReloadHandler(Services.JBOSS_AS, runningModeControl,
                        processState, ServerDescriptions.getResourceDescriptionResolver("server"));
                resourceRegistration.registerOperationHandler(ProcessReloadHandler.OPERATION_NAME, reloadHandler, reloadHandler);
            }

            // The System.exit() based shutdown command is only valid for a server process directly launched from the command line
            if (serverEnvironment.getLaunchType() == ServerEnvironment.LaunchType.STANDALONE) {
                ServerShutdownHandler serverShutdownHandler = new ServerShutdownHandler(processState);
                resourceRegistration.registerOperationHandler(ServerShutdownHandler.DEFINITION, serverShutdownHandler);
            }
            resourceRegistration.registerSubModel(ServerEnvironmentResourceDescription.of(serverEnvironment));
        }

    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        if (serverEnvironment != null) { // TODO eliminate test cases that result in serverEnviroment == null
            if (isDomain) {
                resourceRegistration.registerReadOnlyAttribute(NAME, serverEnvironment.getProcessNameReadHandler());
                resourceRegistration.registerReadWriteAttribute(PROFILE_NAME, null, new StringLengthValidatingHandler(1));
            } else {
                resourceRegistration.registerReadWriteAttribute(NAME, serverEnvironment.getProcessNameReadHandler(), serverEnvironment.getProcessNameWriteHandler());
                // The legacy "undefined" profile-name
                resourceRegistration.registerReadOnlyAttribute(NULL_PROFILE_NAME, null);
            }
            resourceRegistration.registerReadOnlyAttribute(LAUNCH_TYPE, new LaunchTypeHandler(serverEnvironment.getLaunchType()));
        }

        resourceRegistration.registerReadOnlyAttribute(SERVER_STATE, new ProcessStateAttributeHandler(processState));
        resourceRegistration.registerReadOnlyAttribute(PROCESS_TYPE, ProcessTypeHandler.INSTANCE);
        resourceRegistration.registerReadOnlyAttribute(RUNNING_MODE, new RunningModeReadHandler(runningModeControl));


        resourceRegistration.registerReadOnlyAttribute(MANAGEMENT_MAJOR_VERSION, ManagementVersionAttributeHandler.INSTANCE);
        resourceRegistration.registerReadOnlyAttribute(MANAGEMENT_MINOR_VERSION, ManagementVersionAttributeHandler.INSTANCE);
        resourceRegistration.registerReadOnlyAttribute(MANAGEMENT_MICRO_VERSION, ManagementVersionAttributeHandler.INSTANCE);

        resourceRegistration.registerReadOnlyAttribute(RELEASE_VERSION, ReleaseVersionAttributeHandler.INSTANCE);
        resourceRegistration.registerReadOnlyAttribute(RELEASE_CODENAME, ReleaseVersionAttributeHandler.INSTANCE);

        ProductInfoAttributeHandler infoHandler = new ProductInfoAttributeHandler(serverEnvironment != null ? serverEnvironment.getProductConfig() : null);
        resourceRegistration.registerReadOnlyAttribute(PRODUCT_NAME, infoHandler);
        resourceRegistration.registerReadOnlyAttribute(PRODUCT_VERSION, infoHandler);

        resourceRegistration.registerReadOnlyAttribute(NAMESPACES, DefaultEmptyListAttributeHandler.INSTANCE);
        resourceRegistration.registerReadOnlyAttribute(SCHEMA_LOCATIONS, DefaultEmptyListAttributeHandler.INSTANCE);
    }


}
