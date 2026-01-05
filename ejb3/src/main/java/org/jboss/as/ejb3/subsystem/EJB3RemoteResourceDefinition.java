/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import org.jboss.as.controller.AttributeDefinition;
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
import org.jboss.as.ejb3.remote.EJBRemoteConnectorService;
import org.jboss.as.ejb3.remote.http.EJBRemoteHTTPService;
import org.jboss.as.network.ClientMapping;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.registry.Registry;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;

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
    protected static final String REMOTE_TRANSACTION_SERVICE_CAPABILITY_NAME = "org.wildfly.transactions.remote-transaction-service";
    protected static final String REMOTING_ENDPOINT_CAPABILITY_NAME = "org.wildfly.remoting.endpoint";

    public static final String EJB_REMOTE_CONNECTOR_CAPABILITY_NAME = "org.wildfly.ejb.remote.connector";
    public static final String EJB_HTTP_CONNECTOR_CAPABILITY_NAME = "org.wildfly.ejb.remote.http.connector";

    @SuppressWarnings("unchecked")
    static final UnaryServiceDescriptor<Registry<GroupMember, String, List<ClientMapping>>> CLIENT_MAPPINGS_REGISTRY = UnaryServiceDescriptor.of("org.wildfly.ejb.remote.client-mappings-registry", (Class<Registry<GroupMember, String, List<ClientMapping>>>) (Class<?>) Registry.class);

    static final RuntimeCapability<Void> EJB_REMOTE_CONNECTOR_CAPABILITY = RuntimeCapability.Builder.of(EJB_REMOTE_CONNECTOR_CAPABILITY_NAME)
            .setServiceType(EJBRemoteConnectorService.class)
            .addRequirements(REMOTING_ENDPOINT_CAPABILITY_NAME)
            .build();

    static final RuntimeCapability<Void> EJB_HTTP_CONNECTOR_CAPABILITY = RuntimeCapability.Builder.of(EJB_HTTP_CONNECTOR_CAPABILITY_NAME)
            .setServiceType(EJBRemoteHTTPService.class)
            .build();

    @Deprecated
    static final SimpleAttributeDefinition CLIENT_MAPPINGS_CLUSTER_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.CLIENT_MAPPINGS_CLUSTER_NAME, ModelType.STRING, true)
                    // Capability references should not allow expressions
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(org.wildfly.clustering.ejb.bean.LegacyBeanManagementConfiguration.DEFAULT_CONTAINER_NAME))
                    .setDeprecated(EJB3Model.VERSION_10_0_0.getVersion())
                    .build();

    @Deprecated
    static final SimpleAttributeDefinition CONNECTOR_REF =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.CONNECTOR_REF, ModelType.STRING, true)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAlternatives(EJB3SubsystemModel.CONNECTORS)
                    .setDeprecated(EJB3Model.VERSION_8_0_0.getVersion())
                    .build();

    static final StringListAttributeDefinition CONNECTORS =
            new StringListAttributeDefinition.Builder(EJB3SubsystemModel.CONNECTORS)
                    .setAllowExpression(false)
                    .setRequired(true)
                    .setMinSize(1)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setCapabilityReference(CONNECTOR_CAPABILITY_NAME, EJB_REMOTE_CONNECTOR_CAPABILITY)
                    .build();

    static final SimpleAttributeDefinition THREAD_POOL_NAME =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.THREAD_POOL_NAME, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setCapabilityReference(CapabilityReference.builder(EJB_REMOTE_CONNECTOR_CAPABILITY, EJB3SubsystemRootResourceDefinition.EXECUTOR_SERVICE_DESCRIPTOR).build())
                    .build();

    static final SimpleAttributeDefinition EXECUTE_IN_WORKER =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.EXECUTE_IN_WORKER, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(ModelNode.TRUE)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { CLIENT_MAPPINGS_CLUSTER_NAME, CONNECTORS, THREAD_POOL_NAME, EXECUTE_IN_WORKER };

    static final EJB3RemoteServiceAdd ADD_HANDLER = new EJB3RemoteServiceAdd();

    EJB3RemoteResourceDefinition() {
        super(new Parameters(EJB3SubsystemModel.REMOTE_SERVICE_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.REMOTE))
                .setAddHandler(ADD_HANDLER)
                .setAddRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .addCapabilities(EJB_REMOTE_CONNECTOR_CAPABILITY, EJB_HTTP_CONNECTOR_CAPABILITY));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, ReloadRequiredWriteAttributeHandler.INSTANCE);
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
