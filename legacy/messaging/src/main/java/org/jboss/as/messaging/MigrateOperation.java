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

import static java.util.Arrays.asList;
import static org.jboss.as.controller.OperationContext.Stage.MODEL;
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.operations.common.Util.createAddOperation;
import static org.jboss.as.controller.operations.common.Util.createOperation;
import static org.jboss.as.controller.operations.common.Util.createRemoveOperation;
import static org.jboss.as.messaging.CommonAttributes.ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.ALLOW_FAILBACK;
import static org.jboss.as.messaging.CommonAttributes.BACKUP;
import static org.jboss.as.messaging.CommonAttributes.BACKUP_GROUP_NAME;
import static org.jboss.as.messaging.CommonAttributes.BRIDGE;
import static org.jboss.as.messaging.CommonAttributes.BROADCAST_GROUP;
import static org.jboss.as.messaging.CommonAttributes.CHECK_FOR_LIVE_SERVER;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_CONNECTION;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR_REF_STRING;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR_SERVICE;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP_NAME;
import static org.jboss.as.messaging.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.CommonAttributes.FACTORY_CLASS;
import static org.jboss.as.messaging.CommonAttributes.FAILBACK_DELAY;
import static org.jboss.as.messaging.CommonAttributes.FAILOVER_ON_SHUTDOWN;
import static org.jboss.as.messaging.CommonAttributes.GROUP_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.GROUP_PORT;
import static org.jboss.as.messaging.CommonAttributes.HORNETQ_SERVER;
import static org.jboss.as.messaging.CommonAttributes.HTTP_ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.HTTP_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_CHANNEL;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_STACK;
import static org.jboss.as.messaging.CommonAttributes.JMS_QUEUE;
import static org.jboss.as.messaging.CommonAttributes.JMS_TOPIC;
import static org.jboss.as.messaging.CommonAttributes.LOCAL_BIND_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.LOCAL_BIND_PORT;
import static org.jboss.as.messaging.CommonAttributes.MAX_SAVED_REPLICATED_JOURNAL_SIZE;
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.REMOTE_CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.SHARED_STORE;
import static org.jboss.as.messaging.logging.MessagingLogger.ROOT_LOGGER;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.EXPRESSION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.MultistepUtil;
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
    private static final PathAddress MESSAGING_EXTENSION = pathAddress(PathElement.pathElement(EXTENSION, "org.jboss.as.messaging"));

    private static final String NEW_ENTRY_SUFFIX = "-new";
    private static final String HORNETQ_NETTY_CONNECTOR_FACTORY = "org.hornetq.core.remoting.impl.netty.NettyConnectorFactory";
    private static final String HORNETQ_NETTY_ACCEPTOR_FACTORY = "org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory";
    private static final String ARTEMIS_NETTY_CONNECTOR_FACTORY = "org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory";
    private static final String ARTEMIS_NETTY_ACCEPTOR_FACTORY = "org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory";

    public static final String MIGRATE = "migrate";
    public static final String MIGRATION_WARNINGS = "migration-warnings";
    public static final String MIGRATION_ERROR = "migration-error";
    public static final String MIGRATION_OPERATIONS = "migration-operations";
    public static final String DESCRIBE_MIGRATION = "describe-migration";


    public static final StringListAttributeDefinition MIGRATION_WARNINGS_ATTR = new StringListAttributeDefinition.Builder(MIGRATION_WARNINGS)
            .setRequired(false)
            .build();

    public static final SimpleMapAttributeDefinition MIGRATION_ERROR_ATTR = new SimpleMapAttributeDefinition.Builder(MIGRATION_ERROR, ModelType.OBJECT, true)
            .setValueType(ModelType.OBJECT)
            .setRequired(false)
            .build();

    private static final OperationStepHandler DESCRIBE_MIGRATION_INSTANCE = new MigrateOperation(true);
    private static final OperationStepHandler MIGRATE_INSTANCE = new MigrateOperation(false);

    private static final AttributeDefinition ADD_LEGACY_ENTRIES = SimpleAttributeDefinitionBuilder.create("add-legacy-entries", BOOLEAN)
            .setDefaultValue(new ModelNode(false))
            .build();
    public static final String HA_POLICY = "ha-policy";

    private final boolean describe;

    private MigrateOperation(boolean describe) {

        this.describe = describe;
    }

    static void registerOperations(ManagementResourceRegistration registry, ResourceDescriptionResolver resourceDescriptionResolver) {
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder(MIGRATE, resourceDescriptionResolver)
                        .setParameters(ADD_LEGACY_ENTRIES)
                        .setReplyParameters(MIGRATION_WARNINGS_ATTR, MIGRATION_ERROR_ATTR)
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.READ_WHOLE_CONFIG)
                        .build(),
                MigrateOperation.MIGRATE_INSTANCE);
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder(DESCRIBE_MIGRATION, resourceDescriptionResolver)
                        .addParameter(ADD_LEGACY_ENTRIES)
                        .setReplyParameters(MIGRATION_WARNINGS_ATTR)
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.READ_WHOLE_CONFIG)
                        .setReadOnly()
                        .build(),
                MigrateOperation.DESCRIBE_MIGRATION_INSTANCE);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (!describe && context.getRunningMode() != RunningMode.ADMIN_ONLY) {
            throw ROOT_LOGGER.migrateOperationAllowedOnlyInAdminOnly();
        }

        boolean addLegacyEntries = ADD_LEGACY_ENTRIES.resolveModelAttribute(context, operation).asBoolean();

        final List<String> warnings = new ArrayList<>();

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
                transformResources(context, legacyModelAddOps, migrationOperations, addLegacyEntries, warnings);

                // put the /subsystem=messaging:remove operation
                removeMessagingSubsystem(migrationOperations, context.getProcessType() == ProcessType.STANDALONE_SERVER);

                PathAddress parentAddress = context.getCurrentAddress().getParent();
                fixAddressesForDomainMode(parentAddress, migrationOperations);

                if (describe) {
                    // :describe-migration operation

                    // for describe-migration operation, do nothing and return the list of operations that would
                    // be executed in the composite operation
                    final Collection<ModelNode> values = migrationOperations.values();
                    ModelNode result = new ModelNode();
                    fillWarnings(result, warnings);
                    result.get(MIGRATION_OPERATIONS).set(values);

                    context.getResult().set(result);
                } else {
                    // :migrate operation
                    // invoke an OSH on a composite operation with all the migration operations
                    final Map<PathAddress, ModelNode> migrateOpResponses = migrateSubsystems(context, migrationOperations);

                    context.completeStep(new OperationContext.ResultHandler() {
                        @Override
                        public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                            final ModelNode result = new ModelNode();
                            fillWarnings(result, warnings);
                            if (resultAction == OperationContext.ResultAction.ROLLBACK) {
                                for (Map.Entry<PathAddress, ModelNode> entry : migrateOpResponses.entrySet()) {
                                    if (entry.getValue().hasDefined(FAILURE_DESCRIPTION)) {
                                        //we check for failure description, as every node has 'failed', but one
                                        //the real error has a failure description
                                        //we break when we find the first one, as there will only ever be one failure
                                        //as the op stops after the first failure
                                        ModelNode desc = new ModelNode();
                                        desc.get(OP).set(migrationOperations.get(entry.getKey()));
                                        desc.get(RESULT).set(entry.getValue());
                                        result.get(MIGRATION_ERROR).set(desc);
                                        break;
                                    }
                                }
                                context.getFailureDescription().set(ROOT_LOGGER.migrationFailed());
                            }

                            context.getResult().set(result);
                        }
                    });

                }
            }
        }, MODEL);
    }

    protected void fillWarnings(ModelNode result, List<String> warnings) {
        ModelNode rw = new ModelNode().setEmptyList();
        for (String warning : warnings) {
            rw.add(warning);
        }
        result.get(MIGRATION_WARNINGS).set(rw);
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

    private void removeMessagingSubsystem(Map<PathAddress, ModelNode> migrationOperations, boolean standalone) {
        PathAddress subsystemAddress =  pathAddress(MessagingExtension.SUBSYSTEM_PATH);
        ModelNode removeOperation = createRemoveOperation(subsystemAddress);
        migrationOperations.put(subsystemAddress, removeOperation);
        if(standalone) {
            removeOperation = createRemoveOperation(MESSAGING_EXTENSION);
            migrationOperations.put(MESSAGING_EXTENSION, removeOperation);
        }
    }

    private Map<PathAddress, ModelNode> migrateSubsystems(OperationContext context, final Map<PathAddress, ModelNode> migrationOperations) throws OperationFailedException {
        final Map<PathAddress, ModelNode> result = new LinkedHashMap<>();
        MultistepUtil.recordOperationSteps(context, migrationOperations, result);
        return result;
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

    private void transformResources(OperationContext context, final ModelNode legacyModelDescription, final Map<PathAddress, ModelNode> newAddOperations, boolean addLegacyEntries, List<String> warnings) throws OperationFailedException {
        for (ModelNode legacyAddOp : legacyModelDescription.get(RESULT).asList()) {
            final ModelNode newAddOp = legacyAddOp.clone();
            ModelNode legacyAddress = legacyAddOp.get(OP_ADDR);

            ModelNode newAddress = transformAddress(legacyAddress.clone());
            newAddOp.get(OP_ADDR).set(newAddress);

            PathAddress address = PathAddress.pathAddress(newAddress);

            // migrate server resource
            if (address.size() == 2 && "server".equals(address.getLastElement().getKey())) {
                migrateServer(PathAddress.pathAddress(legacyAddress), newAddOp, newAddOperations, warnings);
                continue;
            }

            if (newAddress.asList().size() > 2) {
                // element 0 is subsystem=messaging-activemq
                String parentType = address.getElement(1).getKey();
                String resourceType = address.getElement(2).getKey();
                if ("server".equals(parentType)) {
                    switch (resourceType) {
                        case BROADCAST_GROUP:
                            migrateBroadcastGroup(newAddOp, warnings);
                            break;
                        case DISCOVERY_GROUP:
                            migrateDiscoveryGroup(newAddOp, warnings);
                            break;
                        case CONNECTION_FACTORY:
                            if (addLegacyEntries) {
                                if(connectionFactoryIsUsingInVMConnectors(context, legacyAddOp)) {
                                    warnings.add(ROOT_LOGGER.couldNotCreateLegacyConnectionFactoryUsingInVMConnector(address));
                                } else {
                                    PathAddress legacyConnectionFactoryAddress = address.getParent().append("legacy-connection-factory", address.getLastElement().getValue());
                                    final ModelNode addLegacyConnectionFactoryOp = legacyAddOp.clone();
                                    addLegacyConnectionFactoryOp.get(OP_ADDR).set(legacyConnectionFactoryAddress.toModelNode());
                                    migrateConnectionFactory(addLegacyConnectionFactoryOp, "");
                                    newAddOperations.put(legacyConnectionFactoryAddress, addLegacyConnectionFactoryOp);
                                }
                            }
                            migrateConnectionFactory(newAddOp, addLegacyEntries ? NEW_ENTRY_SUFFIX : "");
                            break;
                        case POOLED_CONNECTION_FACTORY:
                            migratePooledConnectionFactory(newAddOp);
                            break;
                        case CLUSTER_CONNECTION:
                            migrateClusterConnection(newAddOp, warnings);
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
                        case CONNECTOR:
                            migrateGenericTransport(newAddOp);
                            // no break on purpose, the acceptor and connector must also migrate their params.
                        case HTTP_ACCEPTOR:
                        case REMOTE_ACCEPTOR:
                        case HTTP_CONNECTOR:
                        case REMOTE_CONNECTOR:
                        case CONNECTOR_SERVICE:
                            if (address.size() == 4) {
                                // if there are any param resource underneath connectors, acceptors, and connector-services
                                // add them directly to their parent add operation in their params attribute
                                String name = address.getLastElement().getValue();
                                ModelNode value = newAddOp.get(VALUE);
                                ModelNode parentAddOp = newAddOperations.get(address.getParent());
                                if (name.equals("http-upgrade-endpoint") && address.getParent().getLastElement().getKey().equals("http-connector")) {
                                    parentAddOp.get("endpoint").set(value);
                                } else {
                                    if (parameterIsAllowed(name, resourceType)) {
                                        parentAddOp.get("params").add(new Property(name, value));
                                    } else {
                                        warnings.add(ROOT_LOGGER.couldNotMigrateUnsupportedAttribute(name, address.getParent()));
                                    }
                                }
                                continue;
                            }
                            break;
                    }
                }
            }

            newAddOperations.put(address, newAddOp);
        }
    }

    /**
     * Check if the name of the parameter is allowed for the given resourceType.
     */
    private boolean parameterIsAllowed(String name, String resourceType) {
        switch (resourceType) {
            case REMOTE_ACCEPTOR:
            case HTTP_ACCEPTOR:
            case REMOTE_CONNECTOR:
            case HTTP_CONNECTOR:
                // WFLY-5667 - for now remove only use-nio. Revisit this code when Artemis offers an API
                // to know which parameters are ignored.
                if ("use-nio".equals(name)) {
                    return false;
                } else {
                    return true;
                }
            default:
                // accept any parameter for other resources.
                return true;
        }
    }

    private boolean connectionFactoryIsUsingInVMConnectors(OperationContext context, ModelNode connectionFactoryAddOp) {
        ModelNode connector = connectionFactoryAddOp.get(CONNECTOR);
        if (connector.isDefined()) {

            PathAddress connectionFactoryAddress = pathAddress(connectionFactoryAddOp.get(OP_ADDR));
            PathElement relativeLegacyServerAddress = connectionFactoryAddress.getParent().getLastElement();
            // read the server resource related to the context current address (which is the messaging subsystem address).
            Resource serverResource = context.readResource(pathAddress(relativeLegacyServerAddress), false);
            Set<String> definedInVMConnectors = serverResource.getChildrenNames("in-vm-connector");

            // legacy connector is a property list where the name is the connector and the value is undefined
            List<Property> connectorProps = connector.asPropertyList();
            for (Property connectorProp : connectorProps) {
                String connectorName = connectorProp.getName();
                if (definedInVMConnectors.contains(connectorName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void migrateDiscoveryGroup(ModelNode newAddOp, List<String> warnings) {
        // These attributes are not present in the messaging-activemq subsystem.
        // Instead a socket-binding must be used to configure the broadcast-group.
        for (String property : asList(LOCAL_BIND_ADDRESS.getName(), GROUP_ADDRESS.getName(), GROUP_PORT.getName())) {
            if (newAddOp.has(property)) {
                newAddOp.remove(property);
                warnings.add(ROOT_LOGGER.couldNotMigrateDiscoveryGroupAttribute(property, pathAddress(newAddOp.get(OP_ADDR))));
            }
        }
        // These attributes no longer accept expressions in the messaging-activemq subsystem.
        removePropertiesWithExpression(newAddOp, warnings, JGROUPS_CHANNEL.getName(), JGROUPS_STACK.getName());
    }

    private void migrateBroadcastGroup(ModelNode newAddOp, List<String> warnings) {
        // These attributes are not present in the messaging-activemq subsystem.
        // Instead a socket-binding must be used to configure the broadcast-group.
        final Collection<String> unmigratedProperties = asList(LOCAL_BIND_ADDRESS.getName(),
                LOCAL_BIND_PORT.getName(),
                GROUP_ADDRESS.getName(),
                GROUP_PORT.getName());
        for (Property property : newAddOp.asPropertyList()) {
            if (unmigratedProperties.contains(property.getName())) {
                warnings.add(ROOT_LOGGER.couldNotMigrateBroadcastGroupAttribute(property.getName(), pathAddress(newAddOp.get(OP_ADDR))));
            }
        }
        // These attributes no longer accept expressions in the messaging-activemq subsystem.
        removePropertiesWithExpression(newAddOp, warnings, JGROUPS_CHANNEL.getName(), JGROUPS_STACK.getName());
    }


    private void removePropertiesWithExpression(ModelNode newAddOp, List<String> warnings, String... properties) {
        for (String property : properties) {
            if (newAddOp.hasDefined(property) && newAddOp.get(property).getType() == EXPRESSION) {
                newAddOp.remove(property);
                warnings.add(ROOT_LOGGER.couldNotMigrateResourceAttributeWithExpression(property, pathAddress(newAddOp.get(OP_ADDR))));
            }
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
        // WFLY-8928 - allow local transacted JMS session
        addOperation.get("allow-local-transactions").set(new ModelNode(true));
    }

    private void migrateClusterConnection(ModelNode addOperation, List<String> warnings) {
        // connector-ref attribute has been renamed to connector-name
        addOperation.get("connector-name").set(addOperation.get(CONNECTOR_REF_STRING));
        addOperation.remove(CONNECTOR_REF_STRING);

        ModelNode forwardWhenNoConsumers = addOperation.get(ClusterConnectionDefinition.FORWARD_WHEN_NO_CONSUMERS.getName());
        if (forwardWhenNoConsumers.getType() == EXPRESSION) {
            warnings.add(ROOT_LOGGER.couldNotMigrateResourceAttributeWithExpression(ClusterConnectionDefinition.FORWARD_WHEN_NO_CONSUMERS.getName(), pathAddress(addOperation.get(OP_ADDR))));
        } else {
            boolean value = forwardWhenNoConsumers.asBoolean(ClusterConnectionDefinition.FORWARD_WHEN_NO_CONSUMERS.getDefaultValue().asBoolean());
            String messageLoadBalancingType = value ? "STRICT" : "ON_DEMAND";
            addOperation.get("message-load-balancing-type").set(messageLoadBalancingType);
        }
        addOperation.remove(ClusterConnectionDefinition.FORWARD_WHEN_NO_CONSUMERS.getName());

        ModelNode clusterConnectionAddress = addOperation.get(ClusterConnectionDefinition.ADDRESS.getName());
        // HornetQ was routing addresses corresponding to JMS destination by using the "jms" address prefix.
        // Artemis 2 no longer uses this "jms" prefix. Instead, it uses the empty string to denotes any addresses.
        if (clusterConnectionAddress.isDefined() && clusterConnectionAddress.asString().equals("jms")) {
            warnings.add(ROOT_LOGGER.changingClusterConnectionAddress(pathAddress(addOperation.get(OP_ADDR))));
            addOperation.get("cluster-connection-address").set("");
        }


        migrateDiscoveryGroupNameAttribute(addOperation);
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

    /**
     * For generic acceptor and connectors, migrate their factory-class attribute
     * if they are using the default Netty ones.
     */
    private void migrateGenericTransport(ModelNode addOperation) {
        String factoryClass = addOperation.get(FACTORY_CLASS.getName()).asString();
        final String newFactoryClass;
        switch (factoryClass) {
            case HORNETQ_NETTY_ACCEPTOR_FACTORY:
                newFactoryClass = ARTEMIS_NETTY_ACCEPTOR_FACTORY;
                break;
            case HORNETQ_NETTY_CONNECTOR_FACTORY:
                newFactoryClass = ARTEMIS_NETTY_CONNECTOR_FACTORY;
                break;
            default:
                newFactoryClass = factoryClass;
        }
        addOperation.get(FACTORY_CLASS.getName()).set(newFactoryClass);
    }


    private void migrateServer(PathAddress legacyAddress, ModelNode addOperation, Map<PathAddress, ModelNode> newAddOperations, List<String> warnings) {
        discardInterceptors(addOperation, CommonAttributes.REMOTING_INTERCEPTORS.getName(), warnings);
        discardInterceptors(addOperation, CommonAttributes.REMOTING_INCOMING_INTERCEPTORS.getName(), warnings);
        discardInterceptors(addOperation, CommonAttributes.REMOTING_OUTGOING_INTERCEPTORS.getName(), warnings);

        // add the server :add operation before eventually adding a ha-policy child :add operation in migrateHAPolicy.
        newAddOperations.put(pathAddress(addOperation.get(OP_ADDR)), addOperation);

        migrateHAPolicy(legacyAddress, addOperation, newAddOperations, warnings);
    }

    private void migrateHAPolicy(PathAddress legacyAddress, ModelNode serverAddOperation, Map<PathAddress, ModelNode> newAddOperations, List<String> warnings) {
        PathAddress serverAddress = PathAddress.pathAddress(serverAddOperation.get(OP_ADDR));

        ModelNode sharedStoreAttr = serverAddOperation.get(SHARED_STORE.getName());
        ModelNode backupAttr = serverAddOperation.get(BACKUP.getName());

        if (sharedStoreAttr.getType() == EXPRESSION || backupAttr.getType() == EXPRESSION) {
            warnings.add(ROOT_LOGGER.couldNotMigrateHA(legacyAddress));
            return;
        }

        boolean sharedStore = sharedStoreAttr.isDefined() ? sharedStoreAttr.asBoolean() : SHARED_STORE.getDefaultValue().asBoolean();
        boolean backup = backupAttr.isDefined() ? backupAttr.asBoolean() : BACKUP.getDefaultValue().asBoolean();

        ModelNode haPolicyAddOperation = createAddOperation();
        final PathAddress haPolicyAddress;

        if (sharedStore) {
            if (backup) {
                haPolicyAddress = serverAddress.append(HA_POLICY, "shared-store-slave");
                setAndDiscard(haPolicyAddOperation, serverAddOperation, ALLOW_FAILBACK, "allow-failback");
                setAndDiscard(haPolicyAddOperation, serverAddOperation, FAILOVER_ON_SHUTDOWN, "failover-on-server-shutdown");
                discardFailbackDelay(serverAddOperation, warnings);
            } else {
                haPolicyAddress = serverAddress.append(HA_POLICY, "shared-store-master");
                setAndDiscard(haPolicyAddOperation, serverAddOperation, FAILOVER_ON_SHUTDOWN, "failover-on-server-shutdown");
                discardFailbackDelay(serverAddOperation, warnings);
            }
        } else {
            if (backup) {
                haPolicyAddress = serverAddress.append(HA_POLICY, "replication-slave");
                setAndDiscard(haPolicyAddOperation, serverAddOperation, ALLOW_FAILBACK, "allow-failback");
                setAndDiscard(haPolicyAddOperation, serverAddOperation, MAX_SAVED_REPLICATED_JOURNAL_SIZE, "max-saved-replicated-journal-size");
                setAndDiscard(haPolicyAddOperation, serverAddOperation, BACKUP_GROUP_NAME, "group-name");
                discardFailbackDelay(serverAddOperation, warnings);
            } else {
                haPolicyAddress = serverAddress.append(HA_POLICY, "replication-master");
                setAndDiscard(haPolicyAddOperation, serverAddOperation, CHECK_FOR_LIVE_SERVER, "check-for-live-server");
                setAndDiscard(haPolicyAddOperation, serverAddOperation, BACKUP_GROUP_NAME, "group-name");
            }
        }
        haPolicyAddOperation.get(OP_ADDR).set(haPolicyAddress.toModelNode());

        newAddOperations.put(haPolicyAddress, haPolicyAddOperation);
    }

    private void discardInterceptors(ModelNode addOperation, String legacyInterceptorsAttributeName, List<String> warnings) {
        if (!addOperation.get(legacyInterceptorsAttributeName).isDefined()) {
            return;
        }
        warnings.add(ROOT_LOGGER.couldNotMigrateInterceptors(legacyInterceptorsAttributeName));
        addOperation.remove(legacyInterceptorsAttributeName);
    }

    /**
     * Discard from a node and set it to a new node if the attribute is defined. Use the {@code newAttributeName} as the messaging-activemq may
     * named differently its corresponding attribute.
     */
    private void setAndDiscard(ModelNode setNode, ModelNode discardNode, AttributeDefinition legacyAttributeDefinition, String newAttributeName) {
        ModelNode attribute = discardNode.get(legacyAttributeDefinition.getName());
        if (attribute.isDefined()) {
            setNode.get(newAttributeName).set(attribute);
            discardNode.remove(legacyAttributeDefinition.getName());
        }
    }

    private void discardUnsupportedAttribute(ModelNode newAddOp, AttributeDefinition legacyAttributeDefinition, List<String> warnings) {
        if (newAddOp.hasDefined(legacyAttributeDefinition.getName())) {
            newAddOp.remove(legacyAttributeDefinition.getName());
            warnings.add(MessagingLogger.ROOT_LOGGER.couldNotMigrateUnsupportedAttribute(legacyAttributeDefinition.getName(), pathAddress(newAddOp.get(OP_ADDR))));
        }
    }

    private void discardFailbackDelay(ModelNode newAddOp, List<String> warnings) {
        if (newAddOp.hasDefined(FAILBACK_DELAY.getName())) {
            newAddOp.remove(FAILBACK_DELAY.getName());
            warnings.add(MessagingLogger.ROOT_LOGGER.couldNotMigrateFailbackDelayAttribute(pathAddress(newAddOp.get(OP_ADDR))));
        }
    }
}
