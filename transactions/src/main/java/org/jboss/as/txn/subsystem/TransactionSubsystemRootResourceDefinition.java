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

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
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
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.transaction.client.ContextTransactionManager;

import com.arjuna.ats.arjuna.common.CoordinatorEnvironmentBean;
import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.arjuna.coordinator.TxControl;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the root resource of the transaction subsystem.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class TransactionSubsystemRootResourceDefinition extends SimpleResourceDefinition {

    public static final RuntimeCapability<Void> TRANSACTION_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.transactions")
            .build();

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

    public static final SimpleAttributeDefinition PROCESS_ID_UUID = new SimpleAttributeDefinitionBuilder("process-id-uuid", ModelType.BOOLEAN)
            .setRequired(true)
            .setDefaultValue(new ModelNode().set(false))
            .setAlternatives("process-id-socket-binding")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition PROCESS_ID_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder("process-id-socket-binding", ModelType.STRING)
            .setRequired(true)
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

    //coordinator environment
    public static final SimpleAttributeDefinition STATISTICS_ENABLED = new SimpleAttributeDefinitionBuilder(CommonAttributes.STATISTICS_ENABLED, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .setAllowExpression(true).build();

    public static final SimpleAttributeDefinition ENABLE_STATISTICS = new SimpleAttributeDefinitionBuilder(CommonAttributes.ENABLE_STATISTICS, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .setXmlName(Attribute.ENABLE_STATISTICS.getLocalName())
            .setDeprecated(ModelVersion.create(2))
            .setAllowExpression(true).build();

    public static final SimpleAttributeDefinition ENABLE_TSM_STATUS = new SimpleAttributeDefinitionBuilder(CommonAttributes.ENABLE_TSM_STATUS, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)  // TODO is this runtime-changeable?
            .setXmlName(Attribute.ENABLE_TSM_STATUS.getLocalName())
            .setAllowExpression(true).build();

    public static final SimpleAttributeDefinition DEFAULT_TIMEOUT = new SimpleAttributeDefinitionBuilder(CommonAttributes.DEFAULT_TIMEOUT, ModelType.INT, true)
            .setValidator(new IntRangeValidator(0))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setDefaultValue(new ModelNode().set(300))
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .setXmlName(Attribute.DEFAULT_TIMEOUT.getLocalName())
            .setAllowExpression(true).build();

    public static final SimpleAttributeDefinition MAXIMUM_TIMEOUT = new SimpleAttributeDefinitionBuilder(CommonAttributes.MAXIMUM_TIMEOUT, ModelType.INT, true)
            .setValidator(new IntRangeValidator(300))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setDefaultValue(new ModelNode().set(31536000))
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .setAllowExpression(true).build();
    //object store
    public static final SimpleAttributeDefinition OBJECT_STORE_RELATIVE_TO = new SimpleAttributeDefinitionBuilder(CommonAttributes.OBJECT_STORE_RELATIVE_TO, ModelType.STRING, true)
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

    public static final SimpleAttributeDefinition USE_HORNETQ_STORE = new SimpleAttributeDefinitionBuilder(CommonAttributes.USE_HORNETQ_STORE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .addAlternatives(CommonAttributes.USE_JDBC_STORE)
            .addAlternatives(CommonAttributes.USE_JOURNAL_STORE)
            .setFlags(AttributeAccess.Flag.RESTART_JVM)
            .setAllowExpression(false)
            .setDeprecated(ModelVersion.create(3)).build();
    public static final SimpleAttributeDefinition HORNETQ_STORE_ENABLE_ASYNC_IO = new SimpleAttributeDefinitionBuilder(CommonAttributes.HORNETQ_STORE_ENABLE_ASYNC_IO, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_JVM)
            .setXmlName(Attribute.ENABLE_ASYNC_IO.getLocalName())
            .setAllowExpression(true)
            .setRequires(CommonAttributes.USE_HORNETQ_STORE)
            .setDeprecated(ModelVersion.create(3)).build();

    public static final SimpleAttributeDefinition USE_JOURNAL_STORE = new SimpleAttributeDefinitionBuilder(CommonAttributes.USE_JOURNAL_STORE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .addAlternatives(CommonAttributes.USE_JDBC_STORE)
            .addAlternatives(CommonAttributes.USE_HORNETQ_STORE)
            .setFlags(AttributeAccess.Flag.RESTART_JVM)
            .setAllowExpression(false).build();
    public static final SimpleAttributeDefinition JOURNAL_STORE_ENABLE_ASYNC_IO = new SimpleAttributeDefinitionBuilder(CommonAttributes.JOURNAL_STORE_ENABLE_ASYNC_IO, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_JVM)
            .setXmlName(Attribute.ENABLE_ASYNC_IO.getLocalName())
            .setAllowExpression(true)
            .setRequires(CommonAttributes.USE_JOURNAL_STORE).build();

    public static final SimpleAttributeDefinition USE_JDBC_STORE = new SimpleAttributeDefinitionBuilder(CommonAttributes.USE_JDBC_STORE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .addAlternatives(CommonAttributes.USE_JOURNAL_STORE)
            .addAlternatives(CommonAttributes.USE_HORNETQ_STORE)
            .setRequires(CommonAttributes.JDBC_STORE_DATASOURCE)
            .setFlags(AttributeAccess.Flag.RESTART_JVM)
            .setAllowExpression(false).build();
    public static final SimpleAttributeDefinition JDBC_STORE_DATASOURCE = new SimpleAttributeDefinitionBuilder(CommonAttributes.JDBC_STORE_DATASOURCE, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_JVM)
            .setXmlName(Attribute.DATASOURCE_JNDI_NAME.getLocalName())
            .setAllowExpression(true).build();
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
                TransactionSubsystemAdd.INSTANCE, TransactionSubsystemRemove.INSTANCE,
                OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    // all attributes
    static final AttributeDefinition[] attributes = new AttributeDefinition[] {
            BINDING, STATUS_BINDING, RECOVERY_LISTENER, NODE_IDENTIFIER, PROCESS_ID_UUID, PROCESS_ID_SOCKET_BINDING,
            PROCESS_ID_SOCKET_MAX_PORTS, STATISTICS_ENABLED, ENABLE_TSM_STATUS, DEFAULT_TIMEOUT, MAXIMUM_TIMEOUT,
            OBJECT_STORE_RELATIVE_TO, OBJECT_STORE_PATH, JTS, USE_JOURNAL_STORE, USE_JDBC_STORE, JDBC_STORE_DATASOURCE,
            JDBC_ACTION_STORE_DROP_TABLE, JDBC_ACTION_STORE_TABLE_PREFIX, JDBC_COMMUNICATION_STORE_DROP_TABLE,
            JDBC_COMMUNICATION_STORE_TABLE_PREFIX, JDBC_STATE_STORE_DROP_TABLE, JDBC_STATE_STORE_TABLE_PREFIX,
            JOURNAL_STORE_ENABLE_ASYNC_IO
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
        attributesWithoutMutuals.remove(USE_JOURNAL_STORE);
        attributesWithoutMutuals.remove(USE_JDBC_STORE);

        attributesWithoutMutuals.remove(STATISTICS_ENABLED);
        attributesWithoutMutuals.remove(DEFAULT_TIMEOUT);
        attributesWithoutMutuals.remove(MAXIMUM_TIMEOUT);
        attributesWithoutMutuals.remove(JDBC_STORE_DATASOURCE); // Remove these as it also needs special write handler

        attributesWithoutMutuals.remove(PROCESS_ID_UUID);
        attributesWithoutMutuals.remove(PROCESS_ID_SOCKET_BINDING);
        attributesWithoutMutuals.remove(PROCESS_ID_SOCKET_MAX_PORTS);


        OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(attributesWithoutMutuals);
        for(final AttributeDefinition def : attributesWithoutMutuals) {
            resourceRegistration.registerReadWriteAttribute(def, null, writeHandler);
        }

        // Register mutual object store attributes
        OperationStepHandler mutualWriteHandler = new ObjectStoreMutualWriteHandler(USE_JOURNAL_STORE, USE_JDBC_STORE);
        resourceRegistration.registerReadWriteAttribute(USE_JOURNAL_STORE, null, mutualWriteHandler);
        resourceRegistration.registerReadWriteAttribute(USE_JDBC_STORE, null, mutualWriteHandler);

        //Register default-timeout attribute
        resourceRegistration.registerReadWriteAttribute(DEFAULT_TIMEOUT, null, new DefaultTimeoutHandler(DEFAULT_TIMEOUT));
        resourceRegistration.registerReadWriteAttribute(MAXIMUM_TIMEOUT, null, new MaximumTimeoutHandler(MAXIMUM_TIMEOUT));

        // Register jdbc-store-datasource attribute
        resourceRegistration.registerReadWriteAttribute(JDBC_STORE_DATASOURCE, null, new JdbcStoreDatasourceWriteHandler(JDBC_STORE_DATASOURCE));

        // Register mutual object store attributes
        OperationStepHandler mutualProcessIdWriteHandler = new ProcessIdWriteHandler(PROCESS_ID_UUID, PROCESS_ID_SOCKET_BINDING, PROCESS_ID_SOCKET_MAX_PORTS);
        resourceRegistration.registerReadWriteAttribute(PROCESS_ID_UUID, null, mutualProcessIdWriteHandler);
        resourceRegistration.registerReadWriteAttribute(PROCESS_ID_SOCKET_BINDING, null, mutualProcessIdWriteHandler);
        resourceRegistration.registerReadWriteAttribute(PROCESS_ID_SOCKET_MAX_PORTS, null, mutualProcessIdWriteHandler);

        //Register statistics-enabled attribute
        resourceRegistration.registerReadWriteAttribute(STATISTICS_ENABLED, null, new StatisticsEnabledHandler(STATISTICS_ENABLED));
        AliasedHandler esh = new AliasedHandler(STATISTICS_ENABLED.getName());
        resourceRegistration.registerReadWriteAttribute(ENABLE_STATISTICS, esh, esh);

        AliasedHandler hsh = new AliasedHandler(USE_JOURNAL_STORE.getName());
        resourceRegistration.registerReadWriteAttribute(USE_HORNETQ_STORE, hsh, hsh);

        AliasedHandler hseh = new AliasedHandler(JOURNAL_STORE_ENABLE_ASYNC_IO.getName());
        resourceRegistration.registerReadWriteAttribute(HORNETQ_STORE_ENABLE_ASYNC_IO, hseh, hseh);

        if (registerRuntimeOnly) {
            TxStatsHandler.INSTANCE.registerMetrics(resourceRegistration);
        }
    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerCapability(TRANSACTION_CAPABILITY);
    }

    private static class AliasedHandler implements OperationStepHandler {
        private String aliasedName;

        public AliasedHandler(String aliasedName) {
            this.aliasedName = aliasedName;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode aliased = getAliasedOperation(operation);
            context.addStep(aliased, getHandlerForOperation(context, operation), OperationContext.Stage.MODEL, true);
        }

        private ModelNode getAliasedOperation(ModelNode operation) {
            ModelNode aliased = operation.clone();
            aliased.get(ModelDescriptionConstants.NAME).set(aliasedName);
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

            assert !USE_JOURNAL_STORE.isAllowExpression() && !USE_JDBC_STORE.isAllowExpression() : "rework this before enabling expression";

            if (attributeName.equals(USE_JOURNAL_STORE.getName()) || attributeName.equals(USE_JDBC_STORE.getName())) {
                if (newValue.isDefined() && newValue.asBoolean()) {
                    // check the value of the mutual attribute and disable it if it is set to true
                    final String mutualAttributeName = attributeName.equals(USE_JDBC_STORE.getName())
                            ? USE_JOURNAL_STORE.getName()
                            : USE_JDBC_STORE.getName();

                    ModelNode resourceModel = model.getModel();
                    if (resourceModel.hasDefined(mutualAttributeName) && resourceModel.get(mutualAttributeName).asBoolean()) {
                        resourceModel.get(mutualAttributeName).set(new ModelNode(false));
                    }
                }
            }

            context.addStep(JdbcStoreValidationStep.INSTANCE, OperationContext.Stage.MODEL);
        }
    }

    private static class JdbcStoreDatasourceWriteHandler extends ReloadRequiredWriteAttributeHandler {

        public JdbcStoreDatasourceWriteHandler(AttributeDefinition... definitions) {
            super(definitions);
        }

        @Override
        protected void validateUpdatedModel(OperationContext context, Resource model) throws OperationFailedException {
            super.validateUpdatedModel(context, model);
            context.addStep(JdbcStoreValidationStep.INSTANCE, OperationContext.Stage.MODEL);
        }
    }

    /**
     * Validates that if use-jdbc-store is set, jdbc-store-datasource must be also set.
     *
     * Must be added to both use-jdbc-store and jdbc-store-datasource fields.
     */
    private static class JdbcStoreValidationStep implements OperationStepHandler {

        private static JdbcStoreValidationStep INSTANCE = new JdbcStoreValidationStep();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode modelNode = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
            if (modelNode.hasDefined(USE_JDBC_STORE.getName()) && modelNode.get(USE_JDBC_STORE.getName()).asBoolean()
                    && !modelNode.hasDefined(JDBC_STORE_DATASOURCE.getName())) {
                throw TransactionLogger.ROOT_LOGGER.mustBeDefinedIfTrue(JDBC_STORE_DATASOURCE.getName(), USE_JDBC_STORE.getName());
            }
        }
    }

    private static class ProcessIdWriteHandler extends ReloadRequiredWriteAttributeHandler {
        public ProcessIdWriteHandler(final AttributeDefinition... definitions) {
            super(definitions);
        }

        @Override
        protected void validateUpdatedModel(final OperationContext context, final Resource model) throws OperationFailedException {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext operationContext, ModelNode operation) throws OperationFailedException {
                    ModelNode node = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
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

    private static class DefaultTimeoutHandler extends AbstractWriteAttributeHandler<Void> {
        public DefaultTimeoutHandler(final AttributeDefinition... definitions) {
            super(definitions);
        }

        @Override
        protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                               final String attributeName, final ModelNode resolvedValue,
                                               final ModelNode currentValue, final HandbackHolder<Void> handbackHolder)
            throws OperationFailedException {
            int timeout = resolvedValue.asInt();

            TxControl.setDefaultTimeout(timeout);
            if (timeout == 0) {
                ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
                timeout = MAXIMUM_TIMEOUT.resolveModelAttribute(context, model).asInt();
                TransactionLogger.ROOT_LOGGER.timeoutValueIsSetToMaximum(timeout);
            }
            ContextTransactionManager.setGlobalDefaultTransactionTimeout(timeout);
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                             final String attributeName, final ModelNode valueToRestore,
                                             final ModelNode valueToRevert, final Void handback)
            throws OperationFailedException {
            TxControl.setDefaultTimeout(valueToRestore.asInt());
            ContextTransactionManager.setGlobalDefaultTransactionTimeout(valueToRestore.asInt());
        }
    }

    private static class MaximumTimeoutHandler extends AbstractWriteAttributeHandler<Void> {
       public MaximumTimeoutHandler(final AttributeDefinition... definitions) {
           super(definitions);
       }

       @Override
       protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                              final String attributeName, final ModelNode resolvedValue,
                                              final ModelNode currentValue, final HandbackHolder<Void> handbackHolder)
               throws OperationFailedException {
           int maximum_timeout = resolvedValue.asInt();
           if (TxControl.getDefaultTimeout() == 0) {
               TransactionLogger.ROOT_LOGGER.timeoutValueIsSetToMaximum(maximum_timeout);
               ContextTransactionManager.setGlobalDefaultTransactionTimeout(maximum_timeout);
           }
           return false;
       }

        @Override
        protected void revertUpdateToRuntime(OperationContext operationContext, ModelNode modelNode, String s, ModelNode modelNode1, ModelNode modelNode2, Void aVoid) throws OperationFailedException {

        }
    }

    private static class StatisticsEnabledHandler extends AbstractWriteAttributeHandler<Void> {

        private volatile CoordinatorEnvironmentBean coordinatorEnvironmentBean;

        public StatisticsEnabledHandler(final AttributeDefinition... definitions) {
            super(definitions);
        }


        @Override
        protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                               final String attributeName, final ModelNode resolvedValue,
                                               final ModelNode currentValue, final HandbackHolder<Void> handbackHolder)
            throws OperationFailedException {
            if (this.coordinatorEnvironmentBean == null) {
                this.coordinatorEnvironmentBean = arjPropertyManager.getCoordinatorEnvironmentBean();
            }
            coordinatorEnvironmentBean.setEnableStatistics(resolvedValue.asBoolean());
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                             final String attributeName, final ModelNode valueToRestore,
                                             final ModelNode valueToRevert, final Void handback)
            throws OperationFailedException {
            if (this.coordinatorEnvironmentBean == null) {
                this.coordinatorEnvironmentBean = arjPropertyManager.getCoordinatorEnvironmentBean();
            }
            coordinatorEnvironmentBean.setEnableStatistics(valueToRestore.asBoolean());
        }
    }

}
