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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;

/**
 * A {@link org.jboss.as.controller.ResourceDefinition} for the EJB remote service
 * <p/>
 * User: Jaikiran Pai
 */
public class EJB3RemoteResourceDefinition extends SimpleResourceDefinition {
    private static final String JBOSS_AS_REMOTING = "org.jboss.as.remoting";

    public static final String EJB_REMOTE_CAPABILITY_NAME = "org.wildfly.ejb.remote";

    static final RuntimeCapability<Void> EJB_REMOTE_CAPABILITY = RuntimeCapability.Builder.of(EJB_REMOTE_CAPABILITY_NAME).setServiceType(Void.class).build();

    static final SimpleAttributeDefinition CLIENT_MAPPINGS_CLUSTER_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.CLIENT_MAPPINGS_CLUSTER_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(BeanManagerFactoryServiceConfiguratorConfiguration.DEFAULT_CONTAINER_NAME))
                    .build();

    static final SimpleAttributeDefinition CONNECTOR_REF =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.CONNECTOR_REF, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    static final SimpleAttributeDefinition THREAD_POOL_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.THREAD_POOL_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    static final SimpleAttributeDefinition EXECUTE_IN_WORKER =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.EXECUTE_IN_WORKER, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.TRUE)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { CLIENT_MAPPINGS_CLUSTER_NAME, CONNECTOR_REF, THREAD_POOL_NAME, EXECUTE_IN_WORKER };
    static final EJB3RemoteServiceAdd ADD_HANDLER = new EJB3RemoteServiceAdd(ATTRIBUTES);
    public static final EJB3RemoteResourceDefinition INSTANCE = new EJB3RemoteResourceDefinition();

    private EJB3RemoteResourceDefinition() {
        super(new Parameters(EJB3SubsystemModel.REMOTE_SERVICE_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.REMOTE))
                .setAddHandler(ADD_HANDLER)
                .setAddRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .addCapabilities(EJB_REMOTE_CAPABILITY));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            // TODO: Make this read-write attribute
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        // register channel-creation-options as sub model for EJB remote service
        resourceRegistration.registerSubModel(new RemoteConnectorChannelCreationOptionResource());
    }

    @Override
    public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerAdditionalRuntimePackages(RuntimePackageDependency.required(JBOSS_AS_REMOTING));
    }
}
