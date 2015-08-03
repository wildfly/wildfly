/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import static org.jboss.as.controller.OperationContext.Stage.MODEL;
import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.operations.common.Util.createAddOperation;
import static org.jboss.as.controller.operations.common.Util.createOperation;
import static org.jboss.as.controller.operations.common.Util.createRemoveOperation;
import static org.jboss.as.messaging.CommonAttributes.ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.BRIDGE;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_CONNECTION;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR_REF_STRING;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR_SERVICE;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP_NAME;
import static org.jboss.as.messaging.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.HTTP_ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.HTTP_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.JMS_QUEUE;
import static org.jboss.as.messaging.CommonAttributes.JMS_TOPIC;
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_CONNECTOR;
import static org.jboss.dmr.ModelType.BOOLEAN;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Operation to migrate from the legacy messaging subsystem to the new messaging-activemq subsystem.
 *
 * This operation must be performed when the server is in admin-only mode.
 * Internally, the operation:
 *
 * <ul>
 *     <li>query the description of all the messaging subsystem by invoking the :describe operation.
 *     This returns a list of :add operations for each messaging resources.</li>
 *     <li>:add the new org.widlfy.extension.messaging-activemq extension</li>
 *     <li>for each messaging resources, transform the :add operations to add the
 *     corresponding resource to the new messaging-activemq subsystem.
 *     In this step, changes to the resources model are taken into account
 *     (e.g. cluster-connection's connector-ref is named connector-name in the new messaging-activemq subsystem.)</li>
 *     <li>:remove the messaging subsystem</li>
 * </ul>
 *
 * The companion <code>:describe-migration</code> operation will return a list of all the actual operations that would be
 * performed during the invocation of the <code>:migrate</code> operation.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */

public class MigrateOperation implements OperationStepHandler {

    private static final String MESSAGING_ACTIVEMQ_EXTENSION = "org.wildfly.extension.messaging-activemq";
    private static final String MESSAGING_ACTIVEMQ_MODULE = "org.wildfly.extension.messaging-activemq";

    private static final String NEW_ENTRY_SUFFIX = "-new";

    private static final OperationStepHandler DESCRIBE_MIGRATION_INSTANCE = new MigrateOperation(true);
    private static final OperationStepHandler MIGRATE_INSTANCE = new MigrateOperation(false);

    private static final AttributeDefinition ADD_LEGACY_ENTRIES = SimpleAttributeDefinitionBuilder.create("add-legacy-entries", BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .build();

    private final boolean describe;

    private MigrateOperation(boolean describe) {

        this.describe = describe;
    }

    static void registerOperations(ManagementResourceRegistration registry, ResourceDescriptionResolver resourceDescriptionResolver) {
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder("migrate", resourceDescriptionResolver)
                        .setParameters(ADD_LEGACY_ENTRIES)
                        .setRuntimeOnly()
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.READ_WHOLE_CONFIG)
                        .build(),
                MigrateOperation.MIGRATE_INSTANCE);
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder("describe-migration", resourceDescriptionResolver)
                        .addParameter(ADD_LEGACY_ENTRIES)
                        .setReplyType(ModelType.LIST).setReplyValueType(ModelType.OBJECT)
                        .setRuntimeOnly()
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.READ_WHOLE_CONFIG)
                        .build(),
                MigrateOperation.DESCRIBE_MIGRATION_INSTANCE);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (!describe && context.getRunningMode() != RunningMode.ADMIN_ONLY) {
            throw MessagingLogger.ROOT_LOGGER.migrateOperationAllowedOnlyInAdminOnly();
        }

        boolean addLegacyEntries = ADD_LEGACY_ENTRIES.resolveModelAttribute(context, operation).asBoolean();

        // node containing the description (list of add operations) of the legacy subsystem
        final ModelNode legacyModelAddOps = new ModelNode();
        // preserve the order of insertion of the add operations for the new subsystem.
        final Map<PathAddress, ModelNode> migrationOperations = new LinkedHashMap<PathAddress, ModelNode>();

        // invoke an OSH to describe the legacy messaging subsystem
        describeLegacyMessagingResources(context, legacyModelAddOps);
        // invoke an OSH to add the messaging-activemq extension
        // FIXME: this does not work it the extension :add is added to the migrationOperations directly (https://issues.jboss.org/browse/WFCORE-323)
        addMessagingActiveMQExtension(context, migrationOperations, describe);
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                // transform the legacy add operations and put them in migrationOperations
                transformResources(legacyModelAddOps, migrationOperations, addLegacyEntries);

                // put the /subsystem=messaging:remove operation
                removeMessagingSubsystem(migrationOperations);

                PathAddress parentAddress = context.getCurrentAddress().getParent();
                fixAddressesForDomainMode(parentAddress, migrationOperations);

                if (describe) {
                    // :describe-migration operation

                    // for describe-migration operation, do nothing and return the list of operations that would
                    // be executed in the composite operation
                    context.getResult().set(migrationOperations.values());
                } else {
                    // :migrate operation
                    // invoke an OSH on a composite operation with all the migration operations
                    migrateSubsystems(context, migrationOperations);
                }
            }
        }, MODEL);
    }

    /**
     * In domain mode, the subsystem are under /profile=XXX.
     * This method fixes the address by prepending the addresses (that start with /subsystem) with the current
     * operation parent so that is works both in standalone (parent = EMPTY_ADDRESS) and domain mode
     * (parent = /profile=XXX)
     */
    private void fixAddressesForDomainMode(PathAddress parentAddress, Map<PathAddress, ModelNode> migrationOperations) {
        // in standalone mode, do nothing
        if (parentAddress.size() == 0) {
            return;
        }

        // use a linked hash map to preserve operations order
        Map<PathAddress, ModelNode> fixedMigrationOperations = new LinkedHashMap<>(migrationOperations);
        migrationOperations.clear();
        for (Map.Entry<PathAddress, ModelNode> entry : fixedMigrationOperations.entrySet()) {
            PathAddress fixedAddress = parentAddress.append(entry.getKey());
            entry.getValue().get(ADDRESS).set(fixedAddress.toModelNode());
            migrationOperations.put(fixedAddress, entry.getValue());
        }
    }


    /**
     * It's possible that the extension is already present. In that case, this method does nothing.
     */
    private void addMessagingActiveMQExtension(OperationContext context, Map<PathAddress, ModelNode> migrationOperations, boolean describe) {
        Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false);
        if (root.getChildrenNames(EXTENSION).contains(MESSAGING_ACTIVEMQ_EXTENSION)) {
            // extension is already added, do nothing
            return;
        }
        PathAddress extensionAddress = pathAddress(EXTENSION, MESSAGING_ACTIVEMQ_EXTENSION);
        OperationEntry addEntry = context.getRootResourceRegistration().getOperationEntry(extensionAddress, ADD);
        ModelNode addOperation = createAddOperation(extensionAddress);
        addOperation.get(MODULE).set(MESSAGING_ACTIVEMQ_MODULE);
        if (describe) {
            migrationOperations.put(extensionAddress, addOperation);
        } else {
            context.addStep(context.getResult().get(extensionAddress.toString()), addOperation, addEntry.getOperationHandler(), MODEL);
        }
    }

    private void removeMessagingSubsystem(Map<PathAddress, ModelNode> migrationOperations) {
        PathAddress subsystemAddress =  pathAddress(MessagingExtension.SUBSYSTEM_PATH);
        ModelNode removeOperation = createRemoveOperation(subsystemAddress);
        migrationOperations.put(subsystemAddress, removeOperation);
    }

    private void migrateSubsystems(OperationContext context, final Map<PathAddress, ModelNode> migrationOperations) {
        ModelNode compositeOp = createOperation(COMPOSITE, EMPTY_ADDRESS);
        compositeOp.get(STEPS).set(migrationOperations.values());
        context.addStep(compositeOp, CompositeOperationHandler.INSTANCE, MODEL);
    }

    private ModelNode transformAddress(ModelNode legacyAddress) {
        ModelNode newAddress = new ModelNode();
        for (Property segment : legacyAddress.asPropertyList()) {
            final Property newSegment;
            switch (segment.getName()) {
                case CommonAttributes.SUBSYSTEM:
                    newSegment = new Property(SUBSYSTEM, new ModelNode("messaging-activemq"));
                    break;
                case HORNETQ_SERVER:
                    newSegment = new Property("server", segment.getValue());
                    break;
                default:
                    newSegment = segment;
            }
            newAddress.add(newSegment);
        }
        return newAddress;
    }

    private void transformResources(final ModelNode legacyModelDescription, final Map<PathAddress, ModelNode> newAddOperations, boolean addLegacyEntries) throws OperationFailedException {
        for (ModelNode legacyAddOp : legacyModelDescription.get(RESULT).asList()) {
            final ModelNode newAddOp = legacyAddOp.clone();

            ModelNode newAddress = transformAddress(legacyAddOp.get(OP_ADDR).clone());
            newAddOp.get(OP_ADDR).set(newAddress);

            if (newAddress.asList().size() > 2) {
                Property subsystemSubresource = newAddress.asPropertyList().get(1);
                if (subsystemSubresource.getName().equals("server")) {
                    Property serverSubresource = newAddress.asPropertyList().get(2);
                    switch (serverSubresource.getName()) {
                        case CONNECTION_FACTORY:
                            if (addLegacyEntries) {
                                PathAddress address = PathAddress.pathAddress(newAddress);
                                PathAddress legacyConnectionFactoryAddress = address.getParent().append("legacy-connection-factory", address.getLastElement().getValue());
                                final ModelNode addLegacyConnectionFactoryOp = legacyAddOp.clone();
                                addLegacyConnectionFactoryOp.get(OP_ADDR).set(legacyConnectionFactoryAddress.toModelNode());
                                migrateConnectionFactory(addLegacyConnectionFactoryOp, "");
                                newAddOperations.put(pathAddress(addLegacyConnectionFactoryOp.get(OP_ADDR)), addLegacyConnectionFactoryOp);
                            }
                            migrateConnectionFactory(newAddOp, addLegacyEntries ? NEW_ENTRY_SUFFIX : "");
                            break;
                        case POOLED_CONNECTION_FACTORY:
                            migratePooledConnectionFactory(newAddOp);
                            break;
                        case CLUSTER_CONNECTION:
                            migrateClusterConnection(newAddOp);
                            break;
                        case BRIDGE:
                            migrateBridge(newAddOp);
                            break;
                        case JMS_QUEUE:
                        case JMS_TOPIC:
                            if (addLegacyEntries) {
                                addLegacyEntries(newAddOp);
                            }
                            break;
                        case ACCEPTOR:
                        case HTTP_ACCEPTOR:
                        case REMOTE_ACCEPTOR:
                        case CONNECTOR:
                        case HTTP_CONNECTOR:
                        case REMOTE_CONNECTOR:
                        case CONNECTOR_SERVICE:
                            if (newAddress.asPropertyList().size() > 3) {
                                // if there are any param resource underneath connectors, acceptors, and connector-services
                                // add them directly to their parent add operation in their params attribute
                                String name = newAddress.asPropertyList().get(3).getValue().asString();
                                ModelNode value = newAddOp.get(VALUE);
                                PathAddress currentAddress = pathAddress(newAddress);
                                ModelNode parentAddOp = newAddOperations.get(currentAddress.getParent());
                                parentAddOp.get("params").add(new Property(name, value));
                                continue;
                            }
                            break;
                    }
                }
            }

            newAddOperations.put(pathAddress(newAddOp.get(OP_ADDR)), newAddOp);
        }
    }

    private void addLegacyEntries(ModelNode newAddOp) {
        newAddOp.get("legacy-entries").set(newAddOp.get(ENTRIES));
        newAddOp.remove(ENTRIES);
        for (ModelNode legacyEntry : newAddOp.get("legacy-entries").asList()) {
            String newEntry = legacyEntry.asString() + NEW_ENTRY_SUFFIX;
            newAddOp.get(ENTRIES).add(newEntry);
        }
    }

    private void describeLegacyMessagingResources(OperationContext context, ModelNode legacyModelDescription) {
        ModelNode describeLegacySubsystem = createOperation(GenericSubsystemDescribeHandler.DEFINITION, context.getCurrentAddress());
        context.addStep(legacyModelDescription, describeLegacySubsystem, GenericSubsystemDescribeHandler.INSTANCE, MODEL, true);
    }

    private void migrateConnectionFactory(ModelNode addOperation, String entrySuffix) {
        migrateConnectorAttribute(addOperation);
        migrateDiscoveryGroupNameAttribute(addOperation);

        if (!entrySuffix.isEmpty()) {
            List<ModelNode> entries = addOperation.get(ENTRIES).asList();
            addOperation.remove(ENTRIES);
            for (ModelNode entry : entries) {
                String newEntry = entry.asString() + entrySuffix;
                addOperation.get(ENTRIES).add(newEntry);
            }
        }
    }

    private void migratePooledConnectionFactory(ModelNode addOperation) {
        migrateConnectorAttribute(addOperation);
        migrateDiscoveryGroupNameAttribute(addOperation);
    }

    private void migrateClusterConnection(ModelNode addOperation) {
        // connector-ref attribute has been renamed to connector-name
        addOperation.get("connector-name").set(addOperation.get(CONNECTOR_REF_STRING));
        addOperation.remove(CONNECTOR_REF_STRING);
    }

    private void migrateConnectorAttribute(ModelNode addOperation) {
        ModelNode connector = addOperation.get(CONNECTOR);
        if (connector.isDefined()) {
            // legacy connector is a property list where the name is the connector and the value is undefined
            List<Property> connectorProps = connector.asPropertyList();
            for (Property connectorProp : connectorProps) {
                addOperation.get("connectors").add(connectorProp.getName());
            }
            addOperation.remove(CONNECTOR);
        }
    }
    private void migrateDiscoveryGroupNameAttribute(ModelNode addOperation) {
        ModelNode discoveryGroup = addOperation.get(DISCOVERY_GROUP_NAME);
        if (discoveryGroup.isDefined()) {
            // discovery-group-name attribute has been renamed to discovery-group
            addOperation.get("discovery-group").set(discoveryGroup);
            addOperation.remove(DISCOVERY_GROUP_NAME);
        }
    }

    private void migrateBridge(ModelNode addOperation) {
        migrateDiscoveryGroupNameAttribute(addOperation);
    }
}
