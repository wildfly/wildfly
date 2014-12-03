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

package org.jboss.as.txn.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringBytesLengthValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the root resource of the transaction subsystem.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class TransactionSubsystemRootResourceDefinition extends SimpleResourceDefinition {

    //recovery environment
    public static final SimpleAttributeDefinition BINDING = new SimpleAttributeDefinitionBuilder(CommonAttributes.BINDING, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.BINDING.getLocalName())
            .setAllowExpression(true)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    public static final SimpleAttributeDefinition STATUS_BINDING = new SimpleAttributeDefinitionBuilder(CommonAttributes.STATUS_BINDING, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.STATUS_BINDING.getLocalName())
            .setAllowExpression(true)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    public static final SimpleAttributeDefinition RECOVERY_LISTENER = new SimpleAttributeDefinitionBuilder(CommonAttributes.RECOVERY_LISTENER, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.RECOVERY_LISTENER.getLocalName())
            .setAllowExpression(true).build();

    //core environment
    public static final SimpleAttributeDefinition NODE_IDENTIFIER = new SimpleAttributeDefinitionBuilder(CommonAttributes.NODE_IDENTIFIER, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("1"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setValidator(new StringBytesLengthValidator(0,23,true,true))
            .build();

    public static final SimpleAttributeDefinition PROCESS_ID_UUID = new SimpleAttributeDefinitionBuilder("process-id-uuid", ModelType.BOOLEAN, false)
            .setDefaultValue(new ModelNode().set(false))
            .setAlternatives("process-id-socket-binding")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition PROCESS_ID_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder("process-id-socket-binding", ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1, true))
            .setAlternatives("process-id-uuid")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.BINDING.getLocalName())
            .setAllowExpression(true)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();

    public static final SimpleAttributeDefinition PROCESS_ID_SOCKET_MAX_PORTS = new SimpleAttributeDefinitionBuilder("process-id-socket-max-ports", ModelType.INT, true)
            .setValidator(new IntRangeValidator(1, true))
            .setDefaultValue(new ModelNode().set(10))
            .setRequires("process-id-socket-binding")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.SOCKET_PROCESS_ID_MAX_PORTS.getLocalName())
            .setAllowExpression(true)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_CONFIG)
            .build();

    public static final SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinitionBuilder(PathResourceDefinition.RELATIVE_TO)
            .setDefaultValue(new ModelNode().set("jboss.server.data.dir"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDeprecated(ModelVersion.create(1,4))
            .setAllowExpression(true).build();

    public static final SimpleAttributeDefinition PATH = new SimpleAttributeDefinitionBuilder(PathResourceDefinition.PATH)
            .setDefaultValue(new ModelNode().set("var"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDeprecated(ModelVersion.create(1,4))
            .setAllowNull(true)
            .setAllowExpression(true).build();

    //coordinator environment
    public static final SimpleAttributeDefinition STATISTICS_ENABLED = new SimpleAttributeDefinitionBuilder(CommonAttributes.STATISTICS_ENABLED, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)  // TODO should be runtime-changeable
            .setAllowExpression(true).build();

    public static final SimpleAttributeDefinition ENABLE_STATISTICS = new SimpleAttributeDefinitionBuilder(CommonAttributes.ENABLE_STATISTICS, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)  // TODO should be runtime-changeable
            .setXmlName(Attribute.ENABLE_STATISTICS.getLocalName())
            .setDeprecated(ModelVersion.create(2))
            .setAllowExpression(true).build();

    public static final SimpleAttributeDefinition ENABLE_TSM_STATUS = new SimpleAttributeDefinitionBuilder(CommonAttributes.ENABLE_TSM_STATUS, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)  // TODO is this runtime-changeable?
            .setXmlName(Attribute.ENABLE_TSM_STATUS.getLocalName())
            .setAllowExpression(true).build();

    public static final SimpleAttributeDefinition DEFAULT_TIMEOUT = new SimpleAttributeDefinitionBuilder(CommonAttributes.DEFAULT_TIMEOUT, ModelType.INT, true)
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setDefaultValue(new ModelNode().set(300))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)  // TODO is this runtime-changeable?
            .setXmlName(Attribute.DEFAULT_TIMEOUT.getLocalName())
            .setAllowExpression(true).build();

    //object store
    public static final SimpleAttributeDefinition OBJECT_STORE_RELATIVE_TO = new SimpleAttributeDefinitionBuilder(CommonAttributes.OBJECT_STORE_RELATIVE_TO, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("jboss.server.data.dir"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.RELATIVE_TO.getLocalName())
            .setAllowExpression(true).build();
    public static final SimpleAttributeDefinition OBJECT_STORE_PATH = new SimpleAttributeDefinitionBuilder(CommonAttributes.OBJECT_STORE_PATH, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("tx-object-store"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.PATH.getLocalName())
            .setAllowExpression(true).build();

    public static final SimpleAttributeDefinition JTS = new SimpleAttributeDefinitionBuilder(CommonAttributes.JTS, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_JVM)  //I think the use of statics in arjunta will require a JVM restart
            .setAllowExpression(false).build();

    public static final SimpleAttributeDefinition USEHORNETQSTORE = new SimpleAttributeDefinitionBuilder(CommonAttributes.USEHORNETQSTORE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_JVM)
            .setAlternatives(CommonAttributes.USE_JDBC_STORE)
            .setAllowExpression(false).build();
    public static final SimpleAttributeDefinition HORNETQ_STORE_ENABLE_ASYNC_IO = new SimpleAttributeDefinitionBuilder(CommonAttributes.HORNETQ_STORE_ENABLE_ASYNC_IO, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_JVM)
            .setXmlName(Attribute.ENABLE_ASYNC_IO.getLocalName())
            .setAllowExpression(true)
            .setRequires(CommonAttributes.USEHORNETQSTORE).build();

    public static final SimpleAttributeDefinition USE_JDBC_STORE = new SimpleAttributeDefinitionBuilder(CommonAttributes.USE_JDBC_STORE, ModelType.BOOLEAN, true)
                .setDefaultValue(new ModelNode(false))
                .setFlags(AttributeAccess.Flag.RESTART_JVM)
                .setAlternatives(CommonAttributes.USEHORNETQSTORE)
                .setAllowExpression(false).build();
    public static final SimpleAttributeDefinition JDBC_STORE_DATASOURCE = new SimpleAttributeDefinitionBuilder(CommonAttributes.JDBC_STORE_DATASOURCE, ModelType.STRING, true)
                .setFlags(AttributeAccess.Flag.RESTART_JVM)
                .setXmlName(Attribute.DATASOURCE_JNDI_NAME.getLocalName())
                .setAllowExpression(true)
                .setRequires(CommonAttributes.USE_JDBC_STORE).build();
    public static final SimpleAttributeDefinition JDBC_ACTION_STORE_TABLE_PREFIX =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.JDBC_ACTION_STORE_TABLE_PREFIX, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_JVM)
            .setXmlName(Attribute.TABLE_PREFIX.getLocalName())
            .setAllowExpression(true)
            .setRequires(CommonAttributes.USE_JDBC_STORE).build();
    public static final SimpleAttributeDefinition JDBC_ACTION_STORE_DROP_TABLE = new SimpleAttributeDefinitionBuilder(CommonAttributes.JDBC_ACTION_STORE_DROP_TABLE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setFlags(AttributeAccess.Flag.RESTART_JVM)
            .setXmlName(Attribute.DROP_TABLE.getLocalName())
            .setAllowExpression(true)
            .setRequires(CommonAttributes.USE_JDBC_STORE).build();
    public static final SimpleAttributeDefinition JDBC_COMMUNICATION_STORE_TABLE_PREFIX = new SimpleAttributeDefinitionBuilder(CommonAttributes.JDBC_COMMUNICATION_STORE_TABLE_PREFIX, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_JVM)
            .setXmlName(Attribute.TABLE_PREFIX.getLocalName())
            .setAllowExpression(true)
            .setRequires(CommonAttributes.USE_JDBC_STORE).build();
    public static final SimpleAttributeDefinition JDBC_COMMUNICATION_STORE_DROP_TABLE = new SimpleAttributeDefinitionBuilder(CommonAttributes.JDBC_COMMUNICATION_STORE_DROP_TABLE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setFlags(AttributeAccess.Flag.RESTART_JVM)
            .setXmlName(Attribute.DROP_TABLE.getLocalName())
            .setAllowExpression(true)
            .setRequires(CommonAttributes.USE_JDBC_STORE).build();
    public static final SimpleAttributeDefinition JDBC_STATE_STORE_TABLE_PREFIX = new SimpleAttributeDefinitionBuilder(CommonAttributes.JDBC_STATE_STORE_TABLE_PREFIX, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_JVM)
            .setXmlName(Attribute.TABLE_PREFIX.getLocalName())
            .setAllowExpression(true)
            .setRequires(CommonAttributes.USE_JDBC_STORE).build();
    public static final SimpleAttributeDefinition JDBC_STATE_STORE_DROP_TABLE = new SimpleAttributeDefinitionBuilder(CommonAttributes.JDBC_STATE_STORE_DROP_TABLE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setFlags(AttributeAccess.Flag.RESTART_JVM)
            .setXmlName(Attribute.DROP_TABLE.getLocalName())
            .setAllowExpression(true)
            .setRequires(CommonAttributes.USE_JDBC_STORE).build();


    private final boolean registerRuntimeOnly;

    TransactionSubsystemRootResourceDefinition(boolean registerRuntimeOnly) {
        super(TransactionExtension.SUBSYSTEM_PATH,
                TransactionExtension.getResourceDescriptionResolver(),
                TransactionSubsystemAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE,
                OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    // all attributes
    static final AttributeDefinition[] attributes = new AttributeDefinition[] {
            BINDING, STATUS_BINDING, RECOVERY_LISTENER, NODE_IDENTIFIER, PROCESS_ID_UUID, PROCESS_ID_SOCKET_BINDING,
            PROCESS_ID_SOCKET_MAX_PORTS, RELATIVE_TO, PATH, STATISTICS_ENABLED, ENABLE_TSM_STATUS, DEFAULT_TIMEOUT,
            OBJECT_STORE_RELATIVE_TO, OBJECT_STORE_PATH, JTS, USEHORNETQSTORE, USE_JDBC_STORE, JDBC_STORE_DATASOURCE,
            JDBC_ACTION_STORE_DROP_TABLE, JDBC_ACTION_STORE_TABLE_PREFIX, JDBC_COMMUNICATION_STORE_DROP_TABLE,
            JDBC_COMMUNICATION_STORE_TABLE_PREFIX, JDBC_STATE_STORE_DROP_TABLE, JDBC_STATE_STORE_TABLE_PREFIX,
            HORNETQ_STORE_ENABLE_ASYNC_IO
    };

    static final AttributeDefinition[] ATTRIBUTES_WITH_EXPRESSIONS_AFTER_1_1_0 = new AttributeDefinition[] {
            DEFAULT_TIMEOUT, STATISTICS_ENABLED, ENABLE_STATISTICS, ENABLE_TSM_STATUS, NODE_IDENTIFIER, OBJECT_STORE_PATH, OBJECT_STORE_RELATIVE_TO,
            PATH, PROCESS_ID_SOCKET_BINDING, PROCESS_ID_SOCKET_MAX_PORTS, RECOVERY_LISTENER, RELATIVE_TO, BINDING, STATUS_BINDING
    };

    static final AttributeDefinition[] ATTRIBUTES_WITH_EXPRESSIONS_AFTER_1_1_1 = new AttributeDefinition[] {
            JTS, USEHORNETQSTORE
    };

    static final AttributeDefinition[] attributes_1_2 = new AttributeDefinition[] {USE_JDBC_STORE, JDBC_STORE_DATASOURCE,
                JDBC_ACTION_STORE_DROP_TABLE, JDBC_ACTION_STORE_TABLE_PREFIX,
                JDBC_COMMUNICATION_STORE_DROP_TABLE, JDBC_COMMUNICATION_STORE_TABLE_PREFIX,
                JDBC_STATE_STORE_DROP_TABLE, JDBC_STATE_STORE_TABLE_PREFIX
    };

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        // Register all attributes except of the mutual ones
        Set<AttributeDefinition> attributesWithoutMutuals = new HashSet<>(Arrays.asList(attributes));
        attributesWithoutMutuals.remove(USEHORNETQSTORE);
        attributesWithoutMutuals.remove(USE_JDBC_STORE);

        attributesWithoutMutuals.remove(PROCESS_ID_UUID);
        attributesWithoutMutuals.remove(PROCESS_ID_SOCKET_BINDING);
        attributesWithoutMutuals.remove(PROCESS_ID_SOCKET_MAX_PORTS);


        OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(attributesWithoutMutuals);
        for(final AttributeDefinition def : attributesWithoutMutuals) {
            resourceRegistration.registerReadWriteAttribute(def, null, writeHandler);
        }

        // Register mutual object store attributes
        OperationStepHandler mutualWriteHandler = new ObjectStoreMutualWriteHandler(USEHORNETQSTORE, USE_JDBC_STORE);
        resourceRegistration.registerReadWriteAttribute(USEHORNETQSTORE, null, mutualWriteHandler);
        resourceRegistration.registerReadWriteAttribute(USE_JDBC_STORE, null, mutualWriteHandler);

        // Register mutual object store attributes
        OperationStepHandler mutualProcessIdWriteHandler = new ProcessIdWriteHandler(PROCESS_ID_UUID, PROCESS_ID_SOCKET_BINDING, PROCESS_ID_SOCKET_MAX_PORTS);
        resourceRegistration.registerReadWriteAttribute(PROCESS_ID_UUID, null, mutualProcessIdWriteHandler);
        resourceRegistration.registerReadWriteAttribute(PROCESS_ID_SOCKET_BINDING, null, mutualProcessIdWriteHandler);
        resourceRegistration.registerReadWriteAttribute(PROCESS_ID_SOCKET_MAX_PORTS, null, mutualProcessIdWriteHandler);

        EnableStatisticsHandler esh = new EnableStatisticsHandler();
        resourceRegistration.registerReadWriteAttribute(ENABLE_STATISTICS, esh, esh);

        if (registerRuntimeOnly) {
            TxStatsHandler.INSTANCE.registerMetrics(resourceRegistration);
        }
    }

    private static class EnableStatisticsHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode aliased = getAliasedOperation(operation);
            context.addStep(aliased, getHandlerForOperation(context, operation), OperationContext.Stage.MODEL, true);
            context.stepCompleted();
        }

        private static ModelNode getAliasedOperation(ModelNode operation) {
            ModelNode aliased = operation.clone();
            aliased.get(ModelDescriptionConstants.NAME).set(STATISTICS_ENABLED.getName());
            return aliased;
        }

        private static OperationStepHandler getHandlerForOperation(OperationContext context, ModelNode operation) {
            ImmutableManagementResourceRegistration imrr = context.getResourceRegistration();
            return imrr.getOperationHandler(PathAddress.EMPTY_ADDRESS, operation.get(OP).asString());
        }
    }

    private static class ObjectStoreMutualWriteHandler extends ReloadRequiredWriteAttributeHandler {
        public ObjectStoreMutualWriteHandler(final AttributeDefinition... definitions) {
            super(definitions);
        }

        @Override
        protected void finishModelStage(final OperationContext context, final ModelNode operation, String attributeName,
                                        ModelNode newValue, ModelNode oldValue, final Resource model) throws OperationFailedException {
            super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);

            assert !USEHORNETQSTORE.isAllowExpression() && !USE_JDBC_STORE.isAllowExpression() : "rework this before enabling expression";

            if (attributeName.equals(USEHORNETQSTORE.getName()) || attributeName.equals(USE_JDBC_STORE.getName())) {
                if (newValue.asBoolean() == true) {
                    // check the value of the mutual attribute and disable it if it is set to true
                    final String mutualAttributeName = attributeName.equals(USE_JDBC_STORE.getName())
                            ? USEHORNETQSTORE.getName()
                            : USE_JDBC_STORE.getName();

                    ModelNode resourceModel = model.getModel();
                    if (resourceModel.hasDefined(mutualAttributeName) && resourceModel.get(mutualAttributeName).asBoolean()) {
                        resourceModel.get(mutualAttributeName).set(new ModelNode(false));
                    }
                }
            }
        }
    }

    private static class ProcessIdWriteHandler extends ReloadRequiredWriteAttributeHandler {
        public ProcessIdWriteHandler(final AttributeDefinition... definitions) {
            super(definitions);
        }

        @Override
        protected void validateUpdatedModel(final OperationContext context, final Resource model) throws OperationFailedException {
            context.addStep(model.getModel(), new OperationStepHandler() {
                @Override
                public void execute(OperationContext operationContext, ModelNode node) throws OperationFailedException {
                    if (node.hasDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName()) && node.get(TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName()).asBoolean()) {
                        if (node.hasDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName())) {
                            throw TransactionLogger.ROOT_LOGGER.mustBeUndefinedIfTrue(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName(), TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName());
                        } else if (node.hasDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.getName())) {
                            throw TransactionLogger.ROOT_LOGGER.mustBeUndefinedIfTrue(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.getName(), TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName());
                        }
                    } else if (node.hasDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName())) {
                        //it's fine do nothing
                    } else if (node.hasDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.getName())) {
                        throw TransactionLogger.ROOT_LOGGER.mustBedefinedIfDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName(), TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_MAX_PORTS.getName());
                    } else {
                        // not uuid and also not sockets!
                        throw TransactionLogger.ROOT_LOGGER.eitherTrueOrDefined(TransactionSubsystemRootResourceDefinition.PROCESS_ID_UUID.getName(), TransactionSubsystemRootResourceDefinition.PROCESS_ID_SOCKET_BINDING.getName());
                    }

                    context.stepCompleted();
                }
            }, OperationContext.Stage.MODEL);


        }

        @Override
        protected void finishModelStage(final OperationContext context, final ModelNode operation, String attributeName,
                                        ModelNode newValue, ModelNode oldValue, final Resource model) throws OperationFailedException {

            if (attributeName.equals(PROCESS_ID_SOCKET_BINDING.getName())) {
                if (newValue.isDefined()) {

                    ModelNode resourceModel = model.getModel();
                    if (resourceModel.hasDefined(PROCESS_ID_UUID.getName()) && resourceModel.get(PROCESS_ID_UUID.getName()).asBoolean()) {
                        resourceModel.get(PROCESS_ID_UUID.getName()).set(new ModelNode(false));
                    }
                }
            }

            if (attributeName.equals(PROCESS_ID_UUID.getName())) {
                if (newValue.asBoolean(false)) {

                    ModelNode resourceModel = model.getModel();
                    resourceModel.get(PROCESS_ID_SOCKET_BINDING.getName()).clear();
                    resourceModel.get(PROCESS_ID_SOCKET_MAX_PORTS.getName()).clear();

                }
            }

            validateUpdatedModel(context, model);
        }


    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new CMResourceResourceDefinition());
    }

}
