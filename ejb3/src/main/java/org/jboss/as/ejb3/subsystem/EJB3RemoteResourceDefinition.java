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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;

import java.util.List;

/**
 * A {@link org.jboss.as.controller.ResourceDefinition} for the EJB remote service
 *
 * @author Jaikiran Pai
 * @author <a href="mailto:rachmatoa@redhat.com">Richard Achmatowicz</a>
 *
 */
public class EJB3RemoteResourceDefinition extends SimpleResourceDefinition {

    // todo: add in connector capability reference when connector resources are converted to use capabilities (WFCORE-5055)
    public static final String CONNECTOR_CAPABILITY_NAME = "org.wildfly.remoting.connector";
    protected static final String INFINISPAN_CACHE_CONTAINER_CAPABILITY_NAME = "org.wildfly.clustering.infinispan.cache-container";
    protected static final String REMOTE_TRANSACTION_SERVICE_CAPABILITY_NAME = "org.wildfly.transactions.remote-transaction-service";
    protected static final String REMOTING_ENDPOINT_CAPABILITY_NAME = "org.wildfly.remoting.endpoint";
    protected static final String THREAD_POOL_CAPABILITY_NAME = "org.wildfly.threads.executor.ejb3";

    public static final String EJB_REMOTE_CAPABILITY_NAME = "org.wildfly.ejb.remote";

    static final RuntimeCapability<Void> EJB_REMOTE_CAPABILITY = RuntimeCapability.Builder.of(EJB_REMOTE_CAPABILITY_NAME)
            .setServiceType(Void.class)
            .addRequirements(REMOTING_ENDPOINT_CAPABILITY_NAME)
            .build();

    static final SimpleAttributeDefinition CLIENT_MAPPINGS_CLUSTER_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.CLIENT_MAPPINGS_CLUSTER_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(BeanManagerFactoryServiceConfiguratorConfiguration.DEFAULT_CONTAINER_NAME))
                    // TODO: replace this with a Requirement reference when the ejb-spi module for clustering is available
                    .setCapabilityReference(INFINISPAN_CACHE_CONTAINER_CAPABILITY_NAME, EJB_REMOTE_CAPABILITY)
                    .build();

    @Deprecated
    static final SimpleAttributeDefinition CONNECTOR_REF =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.CONNECTOR_REF, ModelType.STRING, true)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAlternatives(EJB3SubsystemModel.CONNECTORS)
                    .setDeprecated(ModelVersion.create(8,0,0))
                    .build();

    static final StringListAttributeDefinition CONNECTORS =
            new StringListAttributeDefinition.Builder(EJB3SubsystemModel.CONNECTORS)
                    .setAllowExpression(false)
                    .setRequired(true)
                    .setMinSize(1)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setCapabilityReference(CONNECTOR_CAPABILITY_NAME, EJB_REMOTE_CAPABILITY)
                    .build();

    static final SimpleAttributeDefinition THREAD_POOL_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.THREAD_POOL_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setCapabilityReference(THREAD_POOL_CAPABILITY_NAME, EJB_REMOTE_CAPABILITY)
                    .build();

    static final SimpleAttributeDefinition EXECUTE_IN_WORKER =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.EXECUTE_IN_WORKER, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.TRUE)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { CLIENT_MAPPINGS_CLUSTER_NAME, CONNECTORS, THREAD_POOL_NAME, EXECUTE_IN_WORKER };

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
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }

        // register custom handlers for deprecated attribute connector-ref
        resourceRegistration.registerReadWriteAttribute(CONNECTOR_REF, new RemoteConnectorRefReadAttributeHandler(), new RemoteConnectorRefWriteAttributeHandler());
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        // register channel-creation-options as sub model for EJB remote service
        resourceRegistration.registerSubModel(new RemoteConnectorChannelCreationOptionResource());
    }

    /**
     * read-attribute handler for deprecated attribute connector-ref:
     * - read the first connector from CONNECTORS and return that as the result
     */
    static class RemoteConnectorRefReadAttributeHandler implements OperationStepHandler {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
            List<ModelNode> connectorsList = CONNECTORS.resolveModelAttribute(context, model).asList();
            // return the first connector in the CONNECTORS list
            context.getResult().set(connectorsList.get(0));
        }
    }

    /**
     * write-attribute handler for deprecated attribute connector-ref
     * - use the new value passed to write-attribute to create a new singleton List for CONNECTORS
     */
    static class RemoteConnectorRefWriteAttributeHandler implements OperationStepHandler {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode value = operation.get(VALUE);
            ModelNode targetValue = new ModelNode().add(value);
            AttributeDefinition targetAttribute = CONNECTORS;
            PathAddress address = context.getCurrentAddress();
            // set up write operation for CONNECTORS
            ModelNode targetOperation = Util.getWriteAttributeOperation(address, targetAttribute.getName(), targetValue);
            OperationStepHandler writeAttributeHandler = context.getRootResourceRegistration().getAttributeAccess(address, targetAttribute.getName()).getWriteHandler();
            writeAttributeHandler.execute(context, targetOperation);
        }
    }
}
