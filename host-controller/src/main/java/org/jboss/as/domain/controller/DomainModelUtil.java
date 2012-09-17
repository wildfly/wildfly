/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL_HOST_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_CLIENT_CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_SUBSYSTEM_ENDPOINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILED_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILURE_PERCENTAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_CODENAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELEASE_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ACROSS_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLING_TO_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLANS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReadResourceNameOperationStepHandler;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.ExtensionResourceDefinition;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.controller.operations.common.InterfaceCriteriaWriteHandler;
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
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.IntRangeValidatingHandler;
import org.jboss.as.controller.operations.validation.AbstractParameterValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.EntryType;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.resource.SocketBindingGroupResourceDefinition;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.controller.transform.SubsystemDescriptionDump;
import org.jboss.as.domain.controller.descriptions.DomainAttributes;
import org.jboss.as.domain.controller.descriptions.DomainDescriptionProviders;
import org.jboss.as.domain.controller.descriptions.DomainRootDescription;
import org.jboss.as.domain.controller.operations.ApplyExtensionsHandler;
import org.jboss.as.domain.controller.operations.ApplyRemoteMasterDomainModelHandler;
import org.jboss.as.domain.controller.operations.DomainServerLifecycleHandlers;
import org.jboss.as.domain.controller.operations.DomainSocketBindingGroupRemoveHandler;
import org.jboss.as.domain.controller.operations.LocalHostNameOperationHandler;
import org.jboss.as.domain.controller.operations.ProcessTypeHandler;
import org.jboss.as.domain.controller.operations.ProfileAddHandler;
import org.jboss.as.domain.controller.operations.ProfileDescribeHandler;
import org.jboss.as.domain.controller.operations.ProfileRemoveHandler;
import org.jboss.as.domain.controller.operations.ResolveExpressionOnDomainHandler;
import org.jboss.as.domain.controller.operations.ServerGroupAddHandler;
import org.jboss.as.domain.controller.operations.ServerGroupProfileWriteAttributeHandler;
import org.jboss.as.domain.controller.operations.ServerGroupRemoveHandler;
import org.jboss.as.domain.controller.operations.SocketBindingGroupAddHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentFullReplaceHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadBytesHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadStreamAttachmentHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadURLHandler;
import org.jboss.as.domain.controller.operations.deployment.ServerGroupDeploymentReplaceHandler;
import org.jboss.as.domain.controller.resource.DomainDeploymentResourceDescription;
import org.jboss.as.domain.controller.resource.SocketBindingResourceDefinition;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.ignored.IgnoredDomainResourceRegistry;
import org.jboss.as.host.controller.model.jvm.JvmResourceDefinition;
import org.jboss.as.management.client.content.ManagedDMRContentResourceDefinition;
import org.jboss.as.management.client.content.ManagedDMRContentTypeResourceDefinition;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.controller.descriptions.ServerDescriptionConstants;
import org.jboss.as.server.controller.resources.DeploymentAttributes;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition.Location;
import org.jboss.as.server.deploymentoverlay.ContentDefinition;
import org.jboss.as.server.deploymentoverlay.DeploymentOverlayDefinition;
import org.jboss.as.server.deploymentoverlay.DeploymentOverlayDeploymentDefinition;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayPriority;
import org.jboss.as.server.operations.LaunchTypeHandler;
import org.jboss.as.server.services.net.LocalDestinationOutboundSocketBindingResourceDefinition;
import org.jboss.as.server.services.net.RemoteDestinationOutboundSocketBindingResourceDefinition;
import org.jboss.as.version.Version;
import org.jboss.dmr.ModelNode;


/**
 * Utilities related to the domain model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DomainModelUtil {

    /**
     * Update the model to hold domain level configuration.
     *
     * @param rootResource the root resource to be updated.
     * @param environment the host controller's configuration environment
     */
    public static void updateCoreModel(final Resource rootResource, HostControllerEnvironment environment) {
        final ModelNode rootModel = rootResource.getModel();
        rootModel.get(RELEASE_VERSION).set(Version.AS_VERSION);
        rootModel.get(RELEASE_CODENAME).set(Version.AS_RELEASE_CODENAME);
        rootModel.get(MANAGEMENT_MAJOR_VERSION).set(Version.MANAGEMENT_MAJOR_VERSION);
        rootModel.get(MANAGEMENT_MINOR_VERSION).set(Version.MANAGEMENT_MINOR_VERSION);
        rootModel.get(MANAGEMENT_MICRO_VERSION).set(Version.MANAGEMENT_MICRO_VERSION);

         // Community uses UNDEF values
        ModelNode nameNode = rootModel.get(PRODUCT_NAME);
        ModelNode versionNode = rootModel.get(PRODUCT_VERSION);

        if (environment != null) {
            String productName = environment.getProductConfig().getProductName();
            String productVersion = environment.getProductConfig().getProductVersion();

            if (productName != null) {
                nameNode.set(productName);
            }
            if (productVersion != null) {
                versionNode.set(productVersion);
            }
        }
    }

    public static void initializeMasterDomainRegistry(final ManagementResourceRegistration root, final ExtensibleConfigurationPersister configurationPersister,
                                                                  final ContentRepository contentRepository, final HostFileRepository fileRepository,
                                                                  final DomainController domainController, final ExtensionRegistry extensionRegistry,
                                                                  final PathManagerService pathManager) {
        initializeDomainRegistry(root, configurationPersister, contentRepository, fileRepository, true,
                domainController.getLocalHostInfo(), extensionRegistry, null, pathManager);
    }

    public static void initializeSlaveDomainRegistry(final ManagementResourceRegistration root, final ExtensibleConfigurationPersister configurationPersister,
                                                                 final ContentRepository contentRepository, final HostFileRepository fileRepository,
                                                                 final LocalHostControllerInfo hostControllerInfo, final ExtensionRegistry extensionRegistry,
                                                             final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry,
                                                             final PathManagerService pathManager) {
        initializeDomainRegistry(root, configurationPersister, contentRepository, fileRepository, false,
                hostControllerInfo, extensionRegistry, ignoredDomainResourceRegistry, pathManager);
    }

    private static void initializeDomainRegistry(final ManagementResourceRegistration root, final ExtensibleConfigurationPersister configurationPersister,
                                                 final ContentRepository contentRepo, final HostFileRepository fileRepository, final boolean isMaster,
                                                 final LocalHostControllerInfo hostControllerInfo,
                                                 final ExtensionRegistry extensionRegistry, final IgnoredDomainResourceRegistry ignoredDomainResourceRegistry,
                                                 final PathManagerService pathManager) {

        final EnumSet<OperationEntry.Flag> readOnly = EnumSet.of(OperationEntry.Flag.READ_ONLY);
        final EnumSet<OperationEntry.Flag> masterOnly = EnumSet.of(OperationEntry.Flag.MASTER_HOST_CONTROLLER_ONLY);

        // Other root resource operations
        XmlMarshallingHandler xmh = new XmlMarshallingHandler(configurationPersister);
        root.registerOperationHandler(XmlMarshallingHandler.OPERATION_NAME, xmh, xmh, false, OperationEntry.EntryType.PUBLIC, readOnly);

        root.registerOperationHandler(NamespaceAddHandler.OPERATION_NAME, NamespaceAddHandler.INSTANCE, NamespaceAddHandler.INSTANCE, false);
        root.registerOperationHandler(NamespaceRemoveHandler.OPERATION_NAME, NamespaceRemoveHandler.INSTANCE, NamespaceRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationAddHandler.OPERATION_NAME, SchemaLocationAddHandler.INSTANCE, SchemaLocationAddHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationRemoveHandler.OPERATION_NAME, SchemaLocationRemoveHandler.INSTANCE, SchemaLocationRemoveHandler.INSTANCE, false);

        if (isMaster) {
            DeploymentUploadURLHandler.registerMaster(root, contentRepo);
            DeploymentUploadStreamAttachmentHandler.registerMaster(root, contentRepo);
            DeploymentUploadBytesHandler.registerMaster(root, contentRepo);
        } else {
            DeploymentUploadURLHandler.registerSlave(root);
            DeploymentUploadStreamAttachmentHandler.registerSlave(root);
            DeploymentUploadBytesHandler.registerSlave(root);

        }
        root.registerOperationHandler(DeploymentAttributes.FULL_REPLACE_DEPLOYMENT_DEFINITION, isMaster ? new DeploymentFullReplaceHandler(contentRepo) : new DeploymentFullReplaceHandler(fileRepository));

        if (isMaster) {
            SnapshotDeleteHandler snapshotDelete = new SnapshotDeleteHandler(configurationPersister);
            root.registerOperationHandler(SnapshotDeleteHandler.OPERATION_NAME, snapshotDelete, snapshotDelete, false, EntryType.PUBLIC, masterOnly);
            SnapshotListHandler snapshotList = new SnapshotListHandler(configurationPersister);
            root.registerOperationHandler(SnapshotListHandler.OPERATION_NAME, snapshotList, snapshotList, false, EntryType.PUBLIC, masterOnly);
            SnapshotTakeHandler snapshotTake = new SnapshotTakeHandler(configurationPersister);
            root.registerOperationHandler(SnapshotTakeHandler.OPERATION_NAME, snapshotTake, snapshotTake, false, EntryType.PUBLIC, masterOnly);
        }

        root.registerReadOnlyAttribute(PROCESS_TYPE, isMaster ? ProcessTypeHandler.MASTER : ProcessTypeHandler.SLAVE, Storage.RUNTIME);
        root.registerReadOnlyAttribute(ServerDescriptionConstants.LAUNCH_TYPE, new LaunchTypeHandler(ServerEnvironment.LaunchType.DOMAIN), Storage.RUNTIME);
        root.registerReadOnlyAttribute(LOCAL_HOST_NAME, new LocalHostNameOperationHandler(hostControllerInfo), Storage.RUNTIME);
        root.registerReadWriteAttribute(DomainAttributes.NAME, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true, true));

        root.registerOperationHandler(ValidateAddressOperationHandler.OPERATION_NAME, ValidateAddressOperationHandler.INSTANCE,
                ValidateAddressOperationHandler.INSTANCE, false, EnumSet.of(OperationEntry.Flag.READ_ONLY));

        root.registerOperationHandler(ResolveExpressionOnDomainHandler.OPERATION_NAME, ResolveExpressionOnDomainHandler.INSTANCE,
                ResolveExpressionOnDomainHandler.INSTANCE, EnumSet.of(OperationEntry.Flag.READ_ONLY, OperationEntry.Flag.DOMAIN_PUSH_TO_SERVERS));

        DomainServerLifecycleHandlers.registerDomainHandlers(root);

        // System Properties
        root.registerSubModel(SystemPropertyResourceDefinition.createForDomainOrHost(Location.DOMAIN));

        final ManagementResourceRegistration interfaces = root.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.NAMED_INTERFACE_PROVIDER);
        interfaces.registerOperationHandler(ADD, InterfaceAddHandler.NAMED_INSTANCE, InterfaceAddHandler.NAMED_INSTANCE, false);
        interfaces.registerOperationHandler(REMOVE, InterfaceRemoveHandler.INSTANCE, InterfaceRemoveHandler.INSTANCE, false);
        InterfaceCriteriaWriteHandler.CONFIG_ONLY.register(interfaces);

        final ManagementResourceRegistration profile = root.registerSubModel(PathElement.pathElement(PROFILE), DomainDescriptionProviders.PROFILE);
        profile.registerOperationHandler(ADD, ProfileAddHandler.INSTANCE, ProfileAddHandler.INSTANCE, false);
        profile.registerOperationHandler(REMOVE, ProfileRemoveHandler.INSTANCE, ProfileRemoveHandler.INSTANCE, false);
        profile.registerOperationHandler(DESCRIBE, ProfileDescribeHandler.INSTANCE, ProfileDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE, readOnly);
        profile.registerReadOnlyAttribute(NAME, ReadResourceNameOperationStepHandler.INSTANCE, Storage.CONFIGURATION);

        root.registerSubModel(PathResourceDefinition.createNamed(pathManager));

        final ManagementResourceRegistration socketBindingGroup = root.registerSubModel(new SocketBindingGroupResourceDefinition(SocketBindingGroupAddHandler.INSTANCE, DomainSocketBindingGroupRemoveHandler.INSTANCE, true));
        socketBindingGroup.registerSubModel(SocketBindingResourceDefinition.INSTANCE);
        // outbound-socket-binding (for remote destination)
        socketBindingGroup.registerSubModel(RemoteDestinationOutboundSocketBindingResourceDefinition.INSTANCE);
        // outbound-socket-binding (for local destination)
        socketBindingGroup.registerSubModel(LocalDestinationOutboundSocketBindingResourceDefinition.INSTANCE);


        final ManagementResourceRegistration serverGroups = root.registerSubModel(PathElement.pathElement(SERVER_GROUP), DomainDescriptionProviders.SERVER_GROUP);
        serverGroups.registerOperationHandler(ADD, ServerGroupAddHandler.INSTANCE, ServerGroupAddHandler.INSTANCE, false);
        serverGroups.registerOperationHandler(REMOVE, ServerGroupRemoveHandler.INSTANCE, ServerGroupRemoveHandler.INSTANCE, false);
        serverGroups.registerReadWriteAttribute(SOCKET_BINDING_GROUP, null, WriteAttributeHandlers.WriteAttributeOperationHandler.INSTANCE, Storage.CONFIGURATION);
        serverGroups.registerReadWriteAttribute(SOCKET_BINDING_PORT_OFFSET, null, new IntRangeValidatingHandler(0, true), Storage.CONFIGURATION);
        serverGroups.registerReadWriteAttribute(PROFILE, null, ServerGroupProfileWriteAttributeHandler.INSTANCE, Storage.CONFIGURATION);
        serverGroups.registerReadOnlyAttribute(MANAGEMENT_SUBSYSTEM_ENDPOINT, null, Storage.CONFIGURATION);
        DomainServerLifecycleHandlers.registerServerGroupHandlers(serverGroups);

        serverGroups.registerSubModel(JvmResourceDefinition.GLOBAL);

        serverGroups.registerOperationHandler(DeploymentAttributes.SERVER_GROUP_REPLACE_DEPLOYMENT_DEFINITION, new ServerGroupDeploymentReplaceHandler(fileRepository));
        serverGroups.registerSubModel(DomainDeploymentResourceDescription.createForServerGroup(contentRepo, fileRepository));


        // Server Group System Properties
        serverGroups.registerSubModel(SystemPropertyResourceDefinition.createForDomainOrHost(Location.SERVER_GROUP));

        // Root Deployments
        root.registerSubModel(DomainDeploymentResourceDescription.createForDomainRoot(isMaster, contentRepo, fileRepository));


        //deployment overlays
        final ManagementResourceRegistration deploymentOverlays = root.registerSubModel(DeploymentOverlayDefinition.INSTANCE);
        deploymentOverlays.registerSubModel(new ContentDefinition(contentRepo, fileRepository));

        //server group deployment overlay links
        final ManagementResourceRegistration serverGroupDeploymentOverlay = serverGroups.registerSubModel(DeploymentOverlayDefinition.INSTANCE);
        serverGroupDeploymentOverlay.registerSubModel(new DeploymentOverlayDeploymentDefinition(DeploymentOverlayPriority.SERVER_GROUP));

        // Management client content
        ManagedDMRContentTypeResourceDefinition plansDef = new ManagedDMRContentTypeResourceDefinition(contentRepo, ROLLOUT_PLAN,
                PathElement.pathElement(MANAGEMENT_CLIENT_CONTENT, ROLLOUT_PLANS), DomainRootDescription.getResourceDescriptionResolver(ROLLOUT_PLANS));
        ManagementResourceRegistration mgmtContent = root.registerSubModel(plansDef);
        ParameterValidator contentValidator = new AbstractParameterValidator(){
            @Override
            public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
                validateRolloutPlanStructure(value);
            }};
        ManagedDMRContentResourceDefinition planDef = ManagedDMRContentResourceDefinition.create(ROLLOUT_PLAN, contentValidator, DomainRootDescription.getResourceDescriptionResolver(ROLLOUT_PLAN));
        mgmtContent.registerSubModel(planDef);

        // Extensions
        root.registerSubModel(new ExtensionResourceDefinition(extensionRegistry, true, !isMaster));
        extensionRegistry.setSubsystemParentResourceRegistrations(profile, null);

        if(!isMaster) {
            final ApplyExtensionsHandler aexh = new ApplyExtensionsHandler(extensionRegistry, hostControllerInfo, ignoredDomainResourceRegistry);
            root.registerOperationHandler(ApplyExtensionsHandler.OPERATION_NAME, aexh, aexh, false, EntryType.PRIVATE);

            ApplyRemoteMasterDomainModelHandler armdmh = new ApplyRemoteMasterDomainModelHandler(fileRepository,
                    contentRepo, hostControllerInfo, ignoredDomainResourceRegistry);
            root.registerOperationHandler(ApplyRemoteMasterDomainModelHandler.OPERATION_NAME, armdmh, armdmh, false, OperationEntry.EntryType.PRIVATE);
        } else {
            final SubsystemDescriptionDump dumper = new SubsystemDescriptionDump(extensionRegistry);
            root.registerOperationHandler(SubsystemDescriptionDump.DEFINITION, dumper, false);
        }

        // Initialize the domain transformers
        DomainTransformers.initializeDomainRegistry(extensionRegistry.getTransformerRegistry());

    }

    public static void validateRolloutPlanStructure(ModelNode plan) throws OperationFailedException {
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

    private static final List<String> ALLOWED_SERVER_GROUP_CHILDREN = Arrays.asList(ROLLING_TO_SERVERS, MAX_FAILURE_PERCENTAGE, MAX_FAILED_SERVERS);

    private static void validateInSeriesServerGroup(ModelNode serverGroup) throws OperationFailedException {
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
