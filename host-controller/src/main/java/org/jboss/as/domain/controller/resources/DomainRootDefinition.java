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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_CLIENT_CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILED_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILURE_PERCENTAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ACROSS_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLING_TO_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLANS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionResourceDefinition;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.controller.operations.common.InterfaceRemoveHandler;
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
import org.jboss.as.controller.operations.validation.AbstractParameterValidator;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.resource.InterfaceDefinition;
import org.jboss.as.controller.resource.SocketBindingGroupResourceDefinition;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.controller.transform.SubsystemDescriptionDump;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.descriptions.DomainResolver;
import org.jboss.as.domain.controller.operations.ApplyExtensionsHandler;
import org.jboss.as.domain.controller.operations.ApplyRemoteMasterDomainModelHandler;
import org.jboss.as.domain.controller.operations.DomainServerLifecycleHandlers;
import org.jboss.as.domain.controller.operations.DomainSocketBindingGroupRemoveHandler;
import org.jboss.as.domain.controller.operations.LocalHostNameOperationHandler;
import org.jboss.as.domain.controller.operations.ProcessTypeHandler;
import org.jboss.as.domain.controller.operations.ResolveExpressionOnDomainHandler;
import org.jboss.as.domain.controller.operations.SocketBindingGroupAddHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentFullReplaceHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadBytesHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadStreamAttachmentHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadURLHandler;
import org.jboss.as.domain.controller.transformers.DomainTransformers;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.management.client.content.ManagedDMRContentTypeResourceDefinition;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironment.LaunchType;
import org.jboss.as.server.controller.descriptions.ServerDescriptionConstants;
import org.jboss.as.server.controller.resources.DeploymentAttributes;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition.Location;
import org.jboss.as.server.deploymentoverlay.DeploymentOverlayDefinition;
import org.jboss.as.server.operations.LaunchTypeHandler;
import org.jboss.as.server.operations.ServerVersionOperations.DefaultEmptyListAttributeHandler;
import org.jboss.as.server.services.net.LocalDestinationOutboundSocketBindingResourceDefinition;
import org.jboss.as.server.services.net.RemoteDestinationOutboundSocketBindingResourceDefinition;
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
    private final PathManagerService pathManager;


    public DomainRootDefinition(final HostControllerEnvironment environment,
            final ExtensibleConfigurationPersister configurationPersister, final ContentRepository contentRepo,
            final HostFileRepository fileRepository, final boolean isMaster,
            final LocalHostControllerInfo hostControllerInfo,
            final ExtensionRegistry extensionRegistry, final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry,
            final PathManagerService pathManager) {
        super(PathElement.pathElement("this-will-be-ignored-since-we-are-root"), DomainResolver.getResolver(DOMAIN, false));
        this.isMaster = isMaster;
        this.environment = environment;
        this.hostControllerInfo = hostControllerInfo;
        this.configurationPersister = configurationPersister;
        this.contentRepo = contentRepo;
        this.fileRepository = fileRepository;
        this.extensionRegistry = extensionRegistry;
        this.ignoredDomainResourceRegistry = ignoredDomainResourceRegistry;
        this.pathManager = pathManager;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadWriteAttribute(NAME, null, new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(NAME));

        resourceRegistration.registerReadOnlyAttribute(PROCESS_TYPE, isMaster ? ProcessTypeHandler.MASTER : ProcessTypeHandler.SLAVE);
        resourceRegistration.registerReadOnlyAttribute(LAUNCH_TYPE, new LaunchTypeHandler(ServerEnvironment.LaunchType.DOMAIN));
        resourceRegistration.registerReadOnlyAttribute(LOCAL_HOST_NAME, new LocalHostNameOperationHandler(hostControllerInfo));

        resourceRegistration.registerReadOnlyAttribute(MANAGEMENT_MAJOR_VERSION, null);
        resourceRegistration.registerReadOnlyAttribute(MANAGEMENT_MINOR_VERSION, null);
        resourceRegistration.registerReadOnlyAttribute(MANAGEMENT_MICRO_VERSION, null);

        resourceRegistration.registerReadOnlyAttribute(RELEASE_VERSION, null);
        resourceRegistration.registerReadOnlyAttribute(RELEASE_CODENAME, null);

        resourceRegistration.registerReadOnlyAttribute(PRODUCT_NAME, null);
        resourceRegistration.registerReadOnlyAttribute(PRODUCT_VERSION, null);

        resourceRegistration.registerReadOnlyAttribute(NAMESPACES, DefaultEmptyListAttributeHandler.INSTANCE);
        resourceRegistration.registerReadOnlyAttribute(SCHEMA_LOCATIONS, DefaultEmptyListAttributeHandler.INSTANCE);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        final EnumSet<OperationEntry.Flag> readOnly = EnumSet.of(OperationEntry.Flag.READ_ONLY);

        // Other root resource operations
        XmlMarshallingHandler xmh = new XmlMarshallingHandler(configurationPersister);
        resourceRegistration.registerOperationHandler(XmlMarshallingHandler.DEFINITION, xmh);

        resourceRegistration.registerOperationHandler(NamespaceAddHandler.DEFINITION, NamespaceAddHandler.INSTANCE);
        resourceRegistration.registerOperationHandler(NamespaceRemoveHandler.DEFINITION, NamespaceRemoveHandler.INSTANCE);
        resourceRegistration.registerOperationHandler(SchemaLocationAddHandler.DEFINITION, SchemaLocationAddHandler.INSTANCE);
        resourceRegistration.registerOperationHandler(SchemaLocationRemoveHandler.DEFINITION, SchemaLocationRemoveHandler.INSTANCE);

        if (isMaster) {
            DeploymentUploadURLHandler.registerMaster(resourceRegistration, contentRepo);
            DeploymentUploadStreamAttachmentHandler.registerMaster(resourceRegistration, contentRepo);
            DeploymentUploadBytesHandler.registerMaster(resourceRegistration, contentRepo);

            SnapshotDeleteHandler snapshotDelete = new SnapshotDeleteHandler(configurationPersister);
            resourceRegistration.registerOperationHandler(SnapshotDeleteHandler.DEFINITION, snapshotDelete);
            SnapshotListHandler snapshotList = new SnapshotListHandler(configurationPersister);
            resourceRegistration.registerOperationHandler(SnapshotListHandler.DEFINITION, snapshotList);
            SnapshotTakeHandler snapshotTake = new SnapshotTakeHandler(configurationPersister);
            resourceRegistration.registerOperationHandler(SnapshotTakeHandler.DEFINITION, snapshotTake);

            final SubsystemDescriptionDump dumper = new SubsystemDescriptionDump(extensionRegistry);
            resourceRegistration.registerOperationHandler(SubsystemDescriptionDump.DEFINITION, dumper);
        } else {
            DeploymentUploadURLHandler.registerSlave(resourceRegistration);
            DeploymentUploadStreamAttachmentHandler.registerSlave(resourceRegistration);
            DeploymentUploadBytesHandler.registerSlave(resourceRegistration);

            final ApplyExtensionsHandler aexh = new ApplyExtensionsHandler(extensionRegistry, hostControllerInfo, ignoredDomainResourceRegistry);
            resourceRegistration.registerOperationHandler(ApplyExtensionsHandler.DEFINITION, aexh);

            ApplyRemoteMasterDomainModelHandler armdmh = new ApplyRemoteMasterDomainModelHandler(fileRepository,
                    contentRepo, hostControllerInfo, ignoredDomainResourceRegistry);
            resourceRegistration.registerOperationHandler(ApplyRemoteMasterDomainModelHandler.DEFINITION, armdmh);
        }
        resourceRegistration.registerOperationHandler(DeploymentAttributes.FULL_REPLACE_DEPLOYMENT_DEFINITION, isMaster ? new DeploymentFullReplaceHandler(contentRepo) : new DeploymentFullReplaceHandler(fileRepository));

        resourceRegistration.registerOperationHandler(ValidateAddressOperationHandler.DEFINITION, ValidateAddressOperationHandler.INSTANCE);

        resourceRegistration.registerOperationHandler(ResolveExpressionOnDomainHandler.DEFINITION, ResolveExpressionOnDomainHandler.INSTANCE);

        DomainServerLifecycleHandlers.registerDomainHandlers(resourceRegistration);
    }



    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(SystemPropertyResourceDefinition.createForDomainOrHost(Location.DOMAIN));
        resourceRegistration.registerSubModel(new InterfaceDefinition(
                InterfaceAddHandler.NAMED_INSTANCE,
                InterfaceRemoveHandler.INSTANCE,
                false
        ));
        resourceRegistration.registerSubModel(new ProfileResourceDefinition(extensionRegistry));
        resourceRegistration.registerSubModel(PathResourceDefinition.createNamed(pathManager));
        resourceRegistration.registerSubModel(DomainDeploymentResourceDescription.createForDomainRoot(isMaster, contentRepo, fileRepository));
        resourceRegistration.registerSubModel(new DeploymentOverlayDefinition(null, contentRepo, fileRepository));
        resourceRegistration.registerSubModel(new ServerGroupResourceDefinition(contentRepo, fileRepository));

        //TODO socket-binding-group currently lives in controller and the child RDs live in domain so they currently need passing in from here
        resourceRegistration.registerSubModel(new SocketBindingGroupResourceDefinition(
                                                    SocketBindingGroupAddHandler.INSTANCE,
                                                    DomainSocketBindingGroupRemoveHandler.INSTANCE,
                                                    true,
                                                    SocketBindingResourceDefinition.INSTANCE,
                                                    RemoteDestinationOutboundSocketBindingResourceDefinition.INSTANCE,
                                                    LocalDestinationOutboundSocketBindingResourceDefinition.INSTANCE));

        //TODO perhaps all these desriptions and the validator log messages should be moved into management-client-content?
        resourceRegistration.registerSubModel(
                new ManagedDMRContentTypeResourceDefinition(contentRepo, ROLLOUT_PLAN,
                PathElement.pathElement(MANAGEMENT_CLIENT_CONTENT, ROLLOUT_PLANS), new RolloutPlanValidator(), DomainResolver.getResolver(ROLLOUT_PLANS), DomainResolver.getResolver(ROLLOUT_PLAN)));

        // Extensions
        resourceRegistration.registerSubModel(new ExtensionResourceDefinition(extensionRegistry, true, !isMaster));


        // Initialize the domain transformers
        DomainTransformers.initializeDomainRegistry(extensionRegistry.getTransformerRegistry());

    }

    public void initialize(ManagementResourceRegistration resourceRegistration) {
        registerAttributes(resourceRegistration);
        registerOperations(resourceRegistration);
        registerChildren(resourceRegistration);
    }

    public static class RolloutPlanValidator extends AbstractParameterValidator {
        private static final List<String> ALLOWED_SERVER_GROUP_CHILDREN = Arrays.asList(ROLLING_TO_SERVERS, MAX_FAILURE_PERCENTAGE, MAX_FAILED_SERVERS);
        @Override
        public void validateParameter(String parameterName, ModelNode plan) throws OperationFailedException {
            if(plan == null) {
                throw new OperationFailedException(MESSAGES.nullVar("plan").getLocalizedMessage());
            }
            if(!plan.hasDefined(ROLLOUT_PLAN)) {
                throw new OperationFailedException(MESSAGES.requiredChildIsMissing(ROLLOUT_PLAN, ROLLOUT_PLAN, plan.toString()));
            }
            ModelNode rolloutPlan1 = plan.get(ROLLOUT_PLAN);

            final Set<String> keys;
            try {
                keys = rolloutPlan1.keys();
            } catch (IllegalArgumentException e) {
                throw new OperationFailedException(MESSAGES.requiredChildIsMissing(ROLLOUT_PLAN, IN_SERIES, plan.toString()));
            }
            if(!keys.contains(IN_SERIES)) {
                throw new OperationFailedException(MESSAGES.requiredChildIsMissing(ROLLOUT_PLAN, IN_SERIES, plan.toString()));
            }
            if(keys.size() > 2 || keys.size() == 2 && !keys.contains(ROLLBACK_ACROSS_GROUPS)) {
                throw new OperationFailedException(MESSAGES.unrecognizedChildren(ROLLOUT_PLAN, IN_SERIES + ", " + ROLLBACK_ACROSS_GROUPS, plan.toString()));
            }

            final ModelNode inSeries = rolloutPlan1.get(IN_SERIES);
            if(!inSeries.isDefined()) {
                throw new OperationFailedException(MESSAGES.requiredChildIsMissing(ROLLOUT_PLAN, IN_SERIES, plan.toString()));
            }

            final List<ModelNode> groups = inSeries.asList();
            if(groups.isEmpty()) {
                throw new OperationFailedException(MESSAGES.inSeriesIsMissingGroups(plan.toString()));
            }

            for(ModelNode group : groups) {
                if(group.hasDefined(SERVER_GROUP)) {
                    final ModelNode serverGroup = group.get(SERVER_GROUP);
                    final Set<String> groupKeys;
                    try {
                        groupKeys = serverGroup.keys();
                    } catch(IllegalArgumentException e) {
                        throw new OperationFailedException(MESSAGES.serverGroupExpectsSingleChild(plan.toString()));
                    }
                    if(groupKeys.size() != 1) {
                        throw new OperationFailedException(MESSAGES.serverGroupExpectsSingleChild(plan.toString()));
                    }
                    validateInSeriesServerGroup(serverGroup.asProperty().getValue());
                } else if(group.hasDefined(CONCURRENT_GROUPS)) {
                    final ModelNode concurrent = group.get(CONCURRENT_GROUPS);
                    for(ModelNode child: concurrent.asList()) {
                        validateInSeriesServerGroup(child.asProperty().getValue());
                    }
                } else {
                    throw new OperationFailedException(MESSAGES.unexpectedInSeriesGroup(plan.toString()));
                }
            }
        }

        public static void validateInSeriesServerGroup(ModelNode serverGroup) throws OperationFailedException {
            if(serverGroup.isDefined()) {
                try {
                    final Set<String> specKeys = serverGroup.keys();
                    if(!ALLOWED_SERVER_GROUP_CHILDREN.containsAll(specKeys)) {
                        throw new OperationFailedException(MESSAGES.unrecognizedChildren(SERVER_GROUP, ALLOWED_SERVER_GROUP_CHILDREN.toString(), specKeys.toString()));
                    }
                } catch(IllegalArgumentException e) {// ignore?
                }
            }
        }
    }
}
