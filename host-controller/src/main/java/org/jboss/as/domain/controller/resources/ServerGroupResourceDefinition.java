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

package org.jboss.as.domain.controller.resources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.domain.controller.LocalHostControllerInfo;
import org.jboss.as.domain.controller.operations.DomainServerLifecycleHandlers;
import org.jboss.as.domain.controller.operations.ServerGroupAddHandler;
import org.jboss.as.domain.controller.operations.ServerGroupProfileWriteAttributeHandler;
import org.jboss.as.domain.controller.operations.ServerGroupRemoveHandler;
import org.jboss.as.domain.controller.operations.ServerGroupSocketBindingGroupWriteAttributeHandler;
import org.jboss.as.domain.controller.operations.deployment.ServerGroupDeploymentReplaceHandler;
import org.jboss.as.host.controller.mgmt.DomainControllerRuntimeIgnoreTransformationRegistry;
import org.jboss.as.host.controller.model.jvm.JvmResourceDefinition;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.repository.HostFileRepository;
import org.jboss.as.server.controller.resources.DeploymentAttributes;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition.Location;
import org.jboss.as.server.deploymentoverlay.DeploymentOverlayDefinition;
import org.jboss.as.server.deploymentoverlay.service.DeploymentOverlayPriority;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for server group resources.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class ServerGroupResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(SERVER_GROUP);

    public static final SimpleAttributeDefinition PROFILE = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.PROFILE, ModelType.STRING)
            .setValidator(new StringLengthValidator(1))
            .build();


    public static final SimpleAttributeDefinition SOCKET_BINDING_GROUP = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.SOCKET_BINDING_GROUP, ModelType.STRING, false)
            .setXmlName(Attribute.REF.getLocalName())
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    public static final SimpleAttributeDefinition SOCKET_BINDING_PORT_OFFSET = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET, ModelType.INT, true)
            .setDefaultValue(new ModelNode(0))
            .setXmlName(Attribute.PORT_OFFSET.getLocalName())
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(-65535, 65535, true, true))
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_CONFIG)
            .build();

    public static final SimpleAttributeDefinition MANAGEMENT_SUBSYSTEM_ENDPOINT = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.MANAGEMENT_SUBSYSTEM_ENDPOINT, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.MANAGEMENT_INTERFACES)
            .build();

    public static final AttributeDefinition[] ADD_ATTRIBUTES = new AttributeDefinition[] {PROFILE, SOCKET_BINDING_GROUP, SOCKET_BINDING_PORT_OFFSET, MANAGEMENT_SUBSYSTEM_ENDPOINT};

    private final boolean master;
    private final ContentRepository contentRepo;
    private final HostFileRepository fileRepository;
    private final DomainControllerRuntimeIgnoreTransformationRegistry runtimeIgnoreTransformationRegistry;

    public ServerGroupResourceDefinition(final boolean master, final LocalHostControllerInfo hostInfo, final ContentRepository contentRepo,
                                         final HostFileRepository fileRepository, final DomainControllerRuntimeIgnoreTransformationRegistry registry) {
        super(PATH, DomainResolver.getResolver(SERVER_GROUP, false), new ServerGroupAddHandler(master), new ServerGroupRemoveHandler(hostInfo));
        this.master = master;
        this.contentRepo = contentRepo;
        this.fileRepository = fileRepository;
        this.runtimeIgnoreTransformationRegistry = registry;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ServerGroupResourceDefinition.ADD_ATTRIBUTES) {
            if (attr.getName().equals(MANAGEMENT_SUBSYSTEM_ENDPOINT.getName())) {
                resourceRegistration.registerReadOnlyAttribute(MANAGEMENT_SUBSYSTEM_ENDPOINT, null);
            } else if (attr.getName().equals(PROFILE.getName())) {
                resourceRegistration.registerReadWriteAttribute(PROFILE, null, new ServerGroupProfileWriteAttributeHandler(master, runtimeIgnoreTransformationRegistry));
            } else if (attr.getName().equals(SOCKET_BINDING_GROUP.getName())) {
                resourceRegistration.registerReadWriteAttribute(SOCKET_BINDING_GROUP, null, new ServerGroupSocketBindingGroupWriteAttributeHandler(master, runtimeIgnoreTransformationRegistry));
            } else {
                resourceRegistration.registerReadWriteAttribute(attr, null, new ModelOnlyWriteAttributeHandler(attr));
            }
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(DeploymentAttributes.SERVER_GROUP_REPLACE_DEPLOYMENT_DEFINITION, new ServerGroupDeploymentReplaceHandler(fileRepository));
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        DomainServerLifecycleHandlers.registerServerGroupHandlers(resourceRegistration);
        resourceRegistration.registerSubModel(JvmResourceDefinition.GLOBAL);
        resourceRegistration.registerSubModel(DomainDeploymentResourceDefinition.createForServerGroup(contentRepo, fileRepository));
        resourceRegistration.registerSubModel(SystemPropertyResourceDefinition.createForDomainOrHost(Location.SERVER_GROUP));
        resourceRegistration.registerSubModel(new DeploymentOverlayDefinition(DeploymentOverlayPriority.SERVER_GROUP, null, null));
    }
}
