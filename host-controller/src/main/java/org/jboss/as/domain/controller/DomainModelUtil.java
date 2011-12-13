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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_CLIENT_CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILED_SERVERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_FAILURE_PERCENTAGE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_TYPE;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.domain.controller.DomainControllerMessages.MESSAGES;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ExtensionContextImpl;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.ExtensionAddHandler;
import org.jboss.as.controller.operations.common.ExtensionRemoveHandler;
import org.jboss.as.controller.operations.common.InterfaceAddHandler;
import org.jboss.as.controller.operations.common.InterfaceCriteriaWriteHandler;
import org.jboss.as.controller.operations.common.InterfaceLegacyCriteriaReadHandler;
import org.jboss.as.controller.operations.common.InterfaceRemoveHandler;
import org.jboss.as.controller.operations.common.JVMHandlers;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
import org.jboss.as.controller.operations.common.PathAddHandler;
import org.jboss.as.controller.operations.common.PathRemoveHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationRemoveHandler;
import org.jboss.as.controller.operations.common.SnapshotDeleteHandler;
import org.jboss.as.controller.operations.common.SnapshotListHandler;
import org.jboss.as.controller.operations.common.SnapshotTakeHandler;
import org.jboss.as.controller.operations.common.SocketBindingGroupRemoveHandler;
import org.jboss.as.controller.operations.common.SystemPropertyAddHandler;
import org.jboss.as.controller.operations.common.SystemPropertyRemoveHandler;
import org.jboss.as.controller.operations.common.SystemPropertyValueWriteAttributeHandler;
import org.jboss.as.controller.operations.common.XmlMarshallingHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.IntRangeValidatingHandler;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.ModelTypeValidatingHandler;
import org.jboss.as.controller.operations.validation.AbstractParameterValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.resource.SocketBindingGroupResourceDefinition;
import org.jboss.as.domain.controller.descriptions.DomainDescriptionProviders;
import org.jboss.as.domain.controller.descriptions.DomainRootDescription;
import org.jboss.as.domain.controller.operations.ApplyRemoteMasterDomainModelHandler;
import org.jboss.as.domain.controller.operations.DomainServerLifecycleHandlers;
import org.jboss.as.domain.controller.operations.ProcessTypeHandler;
import org.jboss.as.domain.controller.operations.ProfileAddHandler;
import org.jboss.as.domain.controller.operations.ProfileDescribeHandler;
import org.jboss.as.domain.controller.operations.ProfileRemoveHandler;
import org.jboss.as.domain.controller.operations.ReadMasterDomainModelHandler;
import org.jboss.as.domain.controller.operations.ResolveExpressionOnDomainHandler;
import org.jboss.as.domain.controller.operations.ServerGroupAddHandler;
import org.jboss.as.domain.controller.operations.ServerGroupRemoveHandler;
import org.jboss.as.domain.controller.operations.SocketBindingGroupAddHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentAddHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentFullReplaceHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentRemoveHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadBytesHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadStreamAttachmentHandler;
import org.jboss.as.domain.controller.operations.deployment.DeploymentUploadURLHandler;
import org.jboss.as.domain.controller.operations.deployment.ServerGroupDeploymentAddHandler;
import org.jboss.as.domain.controller.operations.deployment.ServerGroupDeploymentDeployHandler;
import org.jboss.as.domain.controller.operations.deployment.ServerGroupDeploymentRedeployHandler;
import org.jboss.as.domain.controller.operations.deployment.ServerGroupDeploymentRemoveHandler;
import org.jboss.as.domain.controller.operations.deployment.ServerGroupDeploymentReplaceHandler;
import org.jboss.as.domain.controller.operations.deployment.ServerGroupDeploymentUndeployHandler;
import org.jboss.as.domain.controller.resource.SocketBindingResourceDefinition;
import org.jboss.as.management.client.content.ManagedDMRContentResourceDefinition;
import org.jboss.as.management.client.content.ManagedDMRContentTypeResourceDefinition;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.controller.descriptions.ServerDescriptionConstants;
import org.jboss.as.server.deployment.repository.api.ContentRepository;
import org.jboss.as.server.operations.LaunchTypeHandler;
import org.jboss.as.server.services.net.LocalDestinationOutboundSocketBindingResourceDefinition;
import org.jboss.as.server.services.net.RemoteDestinationOutboundSocketBindingResourceDefinition;
import org.jboss.as.version.Version;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;


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
     */
    public static void updateCoreModel(final Resource rootResource) {
        final ModelNode rootModel = rootResource.getModel();
        rootModel.get(RELEASE_VERSION).set(Version.AS_VERSION);
        rootModel.get(RELEASE_CODENAME).set(Version.AS_RELEASE_CODENAME);
    }

    public static ExtensionContext initializeMasterDomainRegistry(final ManagementResourceRegistration root, final ExtensibleConfigurationPersister configurationPersister,
                                                                  final ContentRepository contentRepository, final FileRepository fileRepository,
                                                                  final DomainController domainController, final UnregisteredHostChannelRegistry registry) {
        return initializeDomainRegistry(root, configurationPersister, contentRepository, fileRepository, true, domainController, registry, domainController.getLocalHostInfo());
    }

    public static ExtensionContext initializeSlaveDomainRegistry(final ManagementResourceRegistration root, final ExtensibleConfigurationPersister configurationPersister,
                                                                 final ContentRepository contentRepository, final FileRepository fileRepository, final LocalHostControllerInfo hostControllerInfo) {
        return initializeDomainRegistry(root, configurationPersister, contentRepository, fileRepository, false, null, null, hostControllerInfo);
    }

    private static ExtensionContext initializeDomainRegistry(final ManagementResourceRegistration root, final ExtensibleConfigurationPersister configurationPersister,
                                                             final ContentRepository contentRepo, final FileRepository fileRepository, final boolean isMaster,
                                                             final DomainController domainController, final UnregisteredHostChannelRegistry registry, final LocalHostControllerInfo hostControllerInfo) {

        final EnumSet<OperationEntry.Flag> readOnly = EnumSet.of(OperationEntry.Flag.READ_ONLY);
        final EnumSet<OperationEntry.Flag> deploymentUpload = EnumSet.of(OperationEntry.Flag.MASTER_HOST_CONTROLLER_ONLY);

        // Other root resource operations
        XmlMarshallingHandler xmh = new XmlMarshallingHandler(configurationPersister);
        root.registerOperationHandler(XmlMarshallingHandler.OPERATION_NAME, xmh, xmh, false, OperationEntry.EntryType.PUBLIC, readOnly);

        root.registerOperationHandler(NamespaceAddHandler.OPERATION_NAME, NamespaceAddHandler.INSTANCE, NamespaceAddHandler.INSTANCE, false);
        root.registerOperationHandler(NamespaceRemoveHandler.OPERATION_NAME, NamespaceRemoveHandler.INSTANCE, NamespaceRemoveHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationAddHandler.OPERATION_NAME, SchemaLocationAddHandler.INSTANCE, SchemaLocationAddHandler.INSTANCE, false);
        root.registerOperationHandler(SchemaLocationRemoveHandler.OPERATION_NAME, SchemaLocationRemoveHandler.INSTANCE, SchemaLocationRemoveHandler.INSTANCE, false);

        DeploymentUploadBytesHandler dubh = isMaster ? new DeploymentUploadBytesHandler(contentRepo) : new DeploymentUploadBytesHandler();
        root.registerOperationHandler(DeploymentUploadBytesHandler.OPERATION_NAME, dubh, dubh, false, OperationEntry.EntryType.PUBLIC, deploymentUpload);
        DeploymentUploadURLHandler duuh = isMaster ? new DeploymentUploadURLHandler(contentRepo) : new DeploymentUploadURLHandler();
        root.registerOperationHandler(DeploymentUploadURLHandler.OPERATION_NAME, duuh, duuh, false, OperationEntry.EntryType.PUBLIC, deploymentUpload);
        DeploymentUploadStreamAttachmentHandler dush = isMaster ? new DeploymentUploadStreamAttachmentHandler(contentRepo) : new DeploymentUploadStreamAttachmentHandler();
        root.registerOperationHandler(DeploymentUploadStreamAttachmentHandler.OPERATION_NAME, dush, dush, false, OperationEntry.EntryType.PUBLIC, deploymentUpload);
        DeploymentFullReplaceHandler dfrh = isMaster ? new DeploymentFullReplaceHandler(contentRepo) : new DeploymentFullReplaceHandler(fileRepository);
        root.registerOperationHandler(DeploymentFullReplaceHandler.OPERATION_NAME, dfrh, dfrh);

        SnapshotDeleteHandler snapshotDelete = new SnapshotDeleteHandler(configurationPersister);
        root.registerOperationHandler(SnapshotDeleteHandler.OPERATION_NAME, snapshotDelete, snapshotDelete, false);
        SnapshotListHandler snapshotList = new SnapshotListHandler(configurationPersister);
        root.registerOperationHandler(SnapshotListHandler.OPERATION_NAME, snapshotList, snapshotList, false);
        SnapshotTakeHandler snapshotTake = new SnapshotTakeHandler(configurationPersister);
        root.registerOperationHandler(SnapshotTakeHandler.OPERATION_NAME, snapshotTake, snapshotTake, false);

        root.registerReadOnlyAttribute(PROCESS_TYPE, isMaster ? ProcessTypeHandler.MASTER : ProcessTypeHandler.SLAVE, Storage.RUNTIME);
        root.registerReadOnlyAttribute(ServerDescriptionConstants.LAUNCH_TYPE, new LaunchTypeHandler(ServerEnvironment.LaunchType.DOMAIN), Storage.RUNTIME);

        root.registerOperationHandler(GlobalOperationHandlers.VALIDATE_ADDRESS_OPERATION_NAME, GlobalOperationHandlers.VALIDATE_ADDRESS, CommonProviders.VALIDATE_ADDRESS_PROVIDER, true);

        root.registerOperationHandler(ResolveExpressionOnDomainHandler.OPERATION_NAME, ResolveExpressionOnDomainHandler.INSTANCE,
                ResolveExpressionOnDomainHandler.INSTANCE, EnumSet.of(OperationEntry.Flag.READ_ONLY, OperationEntry.Flag.DOMAIN_PUSH_TO_SERVERS));

        DomainServerLifecycleHandlers.registerDomainHandlers(root);

        // System Properties
        ManagementResourceRegistration systemProperties = root.registerSubModel(PathElement.pathElement(SYSTEM_PROPERTY), DomainDescriptionProviders.SYSTEM_PROPERTY_PROVIDER);
        systemProperties.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, false);
        systemProperties.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, SystemPropertyRemoveHandler.INSTANCE, SystemPropertyRemoveHandler.INSTANCE, false);
        systemProperties.registerReadWriteAttribute(VALUE, null, SystemPropertyValueWriteAttributeHandler.INSTANCE, Storage.CONFIGURATION);
        systemProperties.registerReadWriteAttribute(BOOT_TIME, null, new ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);

        final ManagementResourceRegistration interfaces = root.registerSubModel(PathElement.pathElement(INTERFACE), CommonProviders.NAMED_INTERFACE_PROVIDER);
        interfaces.registerOperationHandler(ADD, InterfaceAddHandler.NAMED_INSTANCE, InterfaceAddHandler.NAMED_INSTANCE, false);
        interfaces.registerOperationHandler(REMOVE, InterfaceRemoveHandler.INSTANCE, InterfaceRemoveHandler.INSTANCE, false);
        InterfaceCriteriaWriteHandler.register(interfaces);
        interfaces.registerReadOnlyAttribute(ModelDescriptionConstants.CRITERIA, InterfaceLegacyCriteriaReadHandler.INSTANCE, Storage.CONFIGURATION);

        final ManagementResourceRegistration profile = root.registerSubModel(PathElement.pathElement(PROFILE), DomainDescriptionProviders.PROFILE);
        profile.registerOperationHandler(ADD, ProfileAddHandler.INSTANCE, ProfileAddHandler.INSTANCE, false);
        profile.registerOperationHandler(REMOVE, ProfileRemoveHandler.INSTANCE, ProfileRemoveHandler.INSTANCE, false);
        profile.registerOperationHandler(DESCRIBE, ProfileDescribeHandler.INSTANCE, ProfileDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE, readOnly);

        final ManagementResourceRegistration paths = root.registerSubModel(PathElement.pathElement(PATH), DomainDescriptionProviders.PATH_DESCRIPTION);
        paths.registerOperationHandler(ADD, PathAddHandler.NAMED_INSTANCE, PathAddHandler.NAMED_INSTANCE, false);
        paths.registerOperationHandler(REMOVE, PathRemoveHandler.INSTANCE, PathRemoveHandler.INSTANCE, false);

        final ManagementResourceRegistration socketBindingGroup = root.registerSubModel(new SocketBindingGroupResourceDefinition(SocketBindingGroupAddHandler.INSTANCE, SocketBindingGroupRemoveHandler.INSTANCE, true));
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
        DomainServerLifecycleHandlers.registerServerGroupHandlers(serverGroups);


        final ManagementResourceRegistration groupVMs = serverGroups.registerSubModel(PathElement.pathElement(JVM), CommonProviders.JVM_PROVIDER);
        JVMHandlers.register(groupVMs);
        ServerGroupDeploymentReplaceHandler sgdrh = new ServerGroupDeploymentReplaceHandler(fileRepository);
        serverGroups.registerOperationHandler(ServerGroupDeploymentReplaceHandler.OPERATION_NAME, sgdrh, sgdrh);
        final ManagementResourceRegistration serverGroupDeployments = serverGroups.registerSubModel(PathElement.pathElement(DEPLOYMENT), DomainDescriptionProviders.SERVER_GROUP_DEPLOYMENT);
        ServerGroupDeploymentAddHandler sgdah = new ServerGroupDeploymentAddHandler(fileRepository);
        serverGroupDeployments.registerOperationHandler(ServerGroupDeploymentAddHandler.OPERATION_NAME, sgdah, sgdah);
        serverGroupDeployments.registerOperationHandler(ServerGroupDeploymentDeployHandler.OPERATION_NAME, ServerGroupDeploymentDeployHandler.INSTANCE, ServerGroupDeploymentDeployHandler.INSTANCE);
        serverGroupDeployments.registerOperationHandler(ServerGroupDeploymentRedeployHandler.OPERATION_NAME, ServerGroupDeploymentRedeployHandler.INSTANCE, ServerGroupDeploymentRedeployHandler.INSTANCE);
        serverGroupDeployments.registerOperationHandler(ServerGroupDeploymentUndeployHandler.OPERATION_NAME, ServerGroupDeploymentUndeployHandler.INSTANCE, ServerGroupDeploymentUndeployHandler.INSTANCE);
        serverGroupDeployments.registerOperationHandler(DeploymentRemoveHandler.OPERATION_NAME, ServerGroupDeploymentRemoveHandler.INSTANCE, ServerGroupDeploymentRemoveHandler.INSTANCE);

        // Server Group System Properties
        ManagementResourceRegistration serverGroupSystemProperties = serverGroups.registerSubModel(PathElement.pathElement(SYSTEM_PROPERTY), DomainDescriptionProviders.SERVER_GROUP_SYSTEM_PROPERTY_PROVIDER);
        serverGroupSystemProperties.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, SystemPropertyAddHandler.INSTANCE_WITH_BOOTTIME, false);
        serverGroupSystemProperties.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, SystemPropertyRemoveHandler.INSTANCE, SystemPropertyRemoveHandler.INSTANCE, false);
        serverGroupSystemProperties.registerReadWriteAttribute(VALUE, null, SystemPropertyValueWriteAttributeHandler.INSTANCE, Storage.CONFIGURATION);
        serverGroupSystemProperties.registerReadWriteAttribute(BOOT_TIME, null, new ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);

        // Root Deployments
        final ManagementResourceRegistration deployments = root.registerSubModel(PathElement.pathElement(DEPLOYMENT), DomainDescriptionProviders.DEPLOYMENT_PROVIDER);
        DeploymentAddHandler dah = isMaster ? new DeploymentAddHandler(contentRepo) : new DeploymentAddHandler();
        deployments.registerOperationHandler(DeploymentAddHandler.OPERATION_NAME, dah, dah);
        DeploymentRemoveHandler drh = isMaster ? new DeploymentRemoveHandler(contentRepo) : new DeploymentRemoveHandler();
        deployments.registerOperationHandler(DeploymentRemoveHandler.OPERATION_NAME, drh, drh);

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
        final ManagementResourceRegistration extensions = root.registerSubModel(PathElement.pathElement(EXTENSION), CommonProviders.EXTENSION_PROVIDER);
        final ExtensionContext extensionContext = new ExtensionContextImpl(profile, deployments, configurationPersister, ProcessType.HOST_CONTROLLER);
        final ExtensionAddHandler addExtensionHandler = new ExtensionAddHandler(extensionContext, true);
        extensions.registerOperationHandler(ExtensionAddHandler.OPERATION_NAME, addExtensionHandler, addExtensionHandler, false);
        final ExtensionRemoveHandler removeExtensionHandler = new ExtensionRemoveHandler(extensionContext);
        extensions.registerOperationHandler(ExtensionRemoveHandler.OPERATION_NAME, removeExtensionHandler, removeExtensionHandler, false);

        if(!isMaster) {
            ApplyRemoteMasterDomainModelHandler armdmh = new ApplyRemoteMasterDomainModelHandler(extensionContext, fileRepository, contentRepo, hostControllerInfo);
            root.registerOperationHandler(ApplyRemoteMasterDomainModelHandler.OPERATION_NAME, armdmh, armdmh, false, OperationEntry.EntryType.PRIVATE);
        } else {
            ReadMasterDomainModelHandler rmdmh = new ReadMasterDomainModelHandler(domainController, registry);
            root.registerOperationHandler(ReadMasterDomainModelHandler.OPERATION_NAME, rmdmh, rmdmh, false, OperationEntry.EntryType.PRIVATE, EnumSet.of(OperationEntry.Flag.READ_ONLY));
        }

        return extensionContext;
    }

    public static void validateRolloutPlanStructure(ModelNode rolloutPlan) throws OperationFailedException {
        if(rolloutPlan == null) {
            throw new OperationFailedException("rolloutPlan argument is null.");
        }
        if(!rolloutPlan.hasDefined(ROLLOUT_PLAN)) {
            throw new OperationFailedException(MESSAGES.requiredChildIsMissing(ROLLOUT_PLAN, ROLLOUT_PLAN, rolloutPlan.toString()));
        }
        rolloutPlan = rolloutPlan.get(ROLLOUT_PLAN);

        final Set<String> keys;
        try {
            keys = rolloutPlan.keys();
        } catch (IllegalArgumentException e) {
            throw new OperationFailedException(MESSAGES.requiredChildIsMissing(ROLLOUT_PLAN, IN_SERIES, rolloutPlan.toString()));
        }
        if(!keys.contains(IN_SERIES)) {
            throw new OperationFailedException(MESSAGES.requiredChildIsMissing(ROLLOUT_PLAN, IN_SERIES, rolloutPlan.toString()));
        }
        if(keys.size() > 2 || keys.size() == 2 && !keys.contains(ROLLBACK_ACROSS_GROUPS)) {
            throw new OperationFailedException(MESSAGES.unrecognizedChildren(ROLLOUT_PLAN, IN_SERIES + ", " + ROLLBACK_ACROSS_GROUPS, rolloutPlan.toString()));
        }

        final ModelNode inSeries = rolloutPlan.get(IN_SERIES);
        if(!inSeries.isDefined()) {
            throw new OperationFailedException(MESSAGES.requiredChildIsMissing(ROLLOUT_PLAN, IN_SERIES, rolloutPlan.toString()));
        }

        final List<ModelNode> groups = inSeries.asList();
        if(groups.isEmpty()) {
            throw new OperationFailedException(MESSAGES.inSeriesIsMissingGroups(rolloutPlan.toString()));
        }

        for(ModelNode group : groups) {
            if(group.hasDefined(SERVER_GROUP)) {
                final ModelNode serverGroup = group.get(SERVER_GROUP);
                final Set<String> groupKeys;
                try {
                    groupKeys = serverGroup.keys();
                } catch(IllegalArgumentException e) {
                    throw new OperationFailedException(MESSAGES.serverGroupExpectsSingleChild(rolloutPlan.toString()));
                }
                if(groupKeys.size() != 1) {
                    throw new OperationFailedException(MESSAGES.serverGroupExpectsSingleChild(rolloutPlan.toString()));
                }
                validateInSeriesServerGroup(serverGroup.asProperty().getValue());
            } else if(group.hasDefined(CONCURRENT_GROUPS)) {
                final ModelNode concurrent = group.get(CONCURRENT_GROUPS);
                for(ModelNode child: concurrent.asList()) {
                    validateInSeriesServerGroup(child.asProperty().getValue());
                }
            } else {
                throw new OperationFailedException(MESSAGES.unexpectedInSeriesGroup(rolloutPlan.toString()));
            }
        }
    }

    private static final List<String> ALLOWED_SERVER_GROUP_CHILDREN = Arrays.asList(new String[]{ROLLING_TO_SERVERS, MAX_FAILURE_PERCENTAGE, MAX_FAILED_SERVERS});
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
