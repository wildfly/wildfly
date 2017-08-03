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

package org.jboss.as.jacorb;

import static org.jboss.as.controller.OperationContext.Stage.MODEL;
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.operations.common.Util.createRemoveOperation;
import static org.jboss.dmr.ModelType.EXPRESSION;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.MultistepUtil;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.jacorb.logging.JacORBLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.wildfly.iiop.openjdk.ConfigValidator;

/**
 * Operation to migrate from the legacy JacORB subsystem to new IIOP-OpenJDK subsystem.
 *
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 */

public class MigrateOperation implements OperationStepHandler {

    public static final String MIGRATE = "migrate";
    public static final String DESCRIBE_MIGRATION = "describe-migration";
    public static final String MIGRATION_OPERATIONS = "migration-operations";
    public static final String MIGRATION_WARNINGS = "migration-warnings";
    public static final String MIGRATION_ERROR = "migration-error";
    private static final PathAddress JACORB_EXTENSION = pathAddress(PathElement.pathElement(EXTENSION, "org.jboss.as.jacorb"));

    private static final List<String> TRANSFORMED_PROPERTIES = Arrays.asList(JacORBSubsystemConstants.ORB_GIOP_MINOR_VERSION, JacORBSubsystemConstants.ORB_INIT_TRANSACTIONS,
            JacORBSubsystemConstants.ORB_INIT_SECURITY, JacORBSubsystemConstants.SECURITY_SUPPORT_SSL, JacORBSubsystemConstants.SECURITY_ADD_COMP_VIA_INTERCEPTOR,
            JacORBSubsystemConstants.NAMING_EXPORT_CORBALOC);


    public static final StringListAttributeDefinition MIGRATION_WARNINGS_ATTR = new StringListAttributeDefinition.Builder(MIGRATION_WARNINGS)
            .setRequired(false)
            .build();

    public static final SimpleMapAttributeDefinition MIGRATION_ERROR_ATTR = new SimpleMapAttributeDefinition.Builder(MIGRATION_ERROR, ModelType.OBJECT, true)
            .setValueType(ModelType.OBJECT)
            .setRequired(false)
            .build();

    static void registerOperation(final ManagementResourceRegistration registry, final ResourceDescriptionResolver resourceDescriptionResolver) {
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder(MIGRATE, resourceDescriptionResolver)
                        .setReplyParameters(MIGRATION_WARNINGS_ATTR, MIGRATION_ERROR_ATTR)
                        .build(),
                new MigrateOperation(false));
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder(DESCRIBE_MIGRATION, resourceDescriptionResolver)
                        .setReplyParameters(MIGRATION_WARNINGS_ATTR, MIGRATION_ERROR_ATTR)
                        .setReadOnly()
                        .build(),
                new MigrateOperation(true));

    }

    private static final PathElement OPENJDK_EXTENSION_ELEMENT = PathElement.pathElement(EXTENSION, "org.wildfly.iiop-openjdk");
    private static final PathElement OPENJDK_SUBSYSTEM_ELEMENT = PathElement.pathElement(SUBSYSTEM, "iiop-openjdk");
    private static final PathElement JACORB_SUBSYSTEM_ELEMENT = PathElement.pathElement(SUBSYSTEM, "jacorb");

    private final boolean describe;

    private MigrateOperation(final boolean describe) {
        this.describe = describe;
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        if (context.getRunningMode() != RunningMode.ADMIN_ONLY) {
            throw new OperationFailedException("the iiop migration can be performed when the server is in admin-only mode");
        }

        final PathAddress subsystemsAddress = context.getCurrentAddress().getParent();

        if (context.readResourceFromRoot(subsystemsAddress, false).hasChild(OPENJDK_SUBSYSTEM_ELEMENT)) {
            throw new OperationFailedException("can not migrate: the new iiop-openjdk subsystem is already defined");
        }

        final Map<PathAddress, ModelNode> migrateOperations = new LinkedHashMap<>();

        if (!context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false).hasChild(OPENJDK_EXTENSION_ELEMENT)) {
            addOpenjdkExtension(context, migrateOperations);
        }

        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext operationContext, ModelNode modelNode) throws OperationFailedException {

                final Resource jacorbResource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
                final ModelNode jacorbModel = Resource.Tools.readModel(jacorbResource).clone();

                final List<String> warnings = new LinkedList<>();

                List<String> unsupportedProperties = TransformUtils.validateDeprecatedProperites(jacorbModel);
                if (!unsupportedProperties.isEmpty()) {
                    warnings.add(JacORBLogger.ROOT_LOGGER.cannotEmulatePropertiesWarning(unsupportedProperties));
                    for(String unsupportedProperty : unsupportedProperties){
                        jacorbModel.get(unsupportedProperty).clear();
                    }
                }

                checkPropertiesWithExpression(jacorbModel, warnings);

                final ModelNode openjdkModel = TransformUtils.transformModel(jacorbModel);
                warnings.addAll(ConfigValidator.validateConfig(context, openjdkModel));

                final PathAddress openjdkAddress = subsystemsAddress.append(OPENJDK_SUBSYSTEM_ELEMENT);
                addOpenjdkSubsystem(openjdkAddress, openjdkModel, migrateOperations);

                final PathAddress jacorbAddress = subsystemsAddress.append(JACORB_SUBSYSTEM_ELEMENT);
                removeJacorbSubsystem(jacorbAddress, migrateOperations, context.getProcessType() == ProcessType.STANDALONE_SERVER);

                if (describe) {
                    // :describe-migration operation

                    // for describe-migration operation, do nothing and return the list of operations that would
                    // be executed in the composite operation
                    final Collection<ModelNode> values = migrateOperations.values();
                    ModelNode result = new ModelNode();

                    result.get(MIGRATION_OPERATIONS).set(values);

                    ModelNode rw = new ModelNode().setEmptyList();
                    for (String warning : warnings) {
                        rw.add(warning);
                    }
                    result.get(MIGRATION_WARNINGS).set(rw);

                    context.getResult().set(result);
                } else {
                    // :migrate operation
                    // invoke an OSH on a composite operation with all the migration operations
                    final Map<PathAddress, ModelNode> migrateOpResponses = migrateSubsystems(context, migrateOperations);

                    context.completeStep(new OperationContext.ResultHandler() {
                        @Override
                        public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                            final ModelNode result = new ModelNode();

                            ModelNode rw = new ModelNode().setEmptyList();
                            for (String warning : warnings) {
                                rw.add(warning);
                            }
                            result.get(MIGRATION_WARNINGS).set(rw);

                            if (resultAction == OperationContext.ResultAction.ROLLBACK) {
                                for (Map.Entry<PathAddress, ModelNode> entry : migrateOpResponses.entrySet()) {
                                    if (entry.getValue().hasDefined(FAILURE_DESCRIPTION)) {
                                        //we check for failure description, as every node has 'failed', but one
                                        //the real error has a failure description
                                        //we break when we find the first one, as there will only ever be one failure
                                        //as the op stops after the first failure
                                        ModelNode desc = new ModelNode();
                                        desc.get(OP).set(migrateOperations.get(entry.getKey()));
                                        desc.get(RESULT).set(entry.getValue());
                                        result.get(MIGRATION_ERROR).set(desc);
                                        break;
                                    }
                                }
                                context.getFailureDescription().set(new ModelNode(JacORBLogger.ROOT_LOGGER.migrationFailed()));
                            }

                            context.getResult().set(result);
                        }
                    });
                }
            }
        }, MODEL);


    }

    private void checkPropertiesWithExpression(final ModelNode legacyModel, final List<String> warnings) {
        final List<String> transformedExpressionProperties = new LinkedList<>();
        for (Property property : legacyModel.asPropertyList()) {
            if (property.getValue().getType() == EXPRESSION && TRANSFORMED_PROPERTIES.contains(property.getName())) {
                transformedExpressionProperties.add(property.getName());
            }
        }
        if (!transformedExpressionProperties.isEmpty()) {
            warnings.add(JacORBLogger.ROOT_LOGGER.expressionMigrationWarning(transformedExpressionProperties.toString()));
        }
    }

    private void addOpenjdkExtension(final OperationContext context, final Map<PathAddress, ModelNode> migrateOperations) {
        final PathAddress extensionAddress = PathAddress.EMPTY_ADDRESS.append(OPENJDK_EXTENSION_ELEMENT);
        OperationEntry addEntry = context.getRootResourceRegistration().getOperationEntry(extensionAddress, ADD);
        final ModelNode addOperation = Util.createAddOperation(extensionAddress);
        if (describe) {
            migrateOperations.put(extensionAddress, addOperation);
        } else {
            context.addStep(context.getResult().get(extensionAddress.toString()), addOperation, addEntry.getOperationHandler(), MODEL);
        }
    }

    private void addOpenjdkSubsystem(final PathAddress address, final ModelNode model,
                                     final Map<PathAddress, ModelNode> migrateOperations) {
        final ModelNode operation = Util.createAddOperation(address);
        for (final Property property : model.asPropertyList()) {
            if (property.getValue().isDefined()) {
                operation.get(property.getName()).set(property.getValue());
            }
        }
        migrateOperations.put(address, operation);
    }

    private void removeJacorbSubsystem(final PathAddress address, final Map<PathAddress, ModelNode> migrateOperations, boolean standalone) {
        ModelNode removeLegacySubsystemOperation = Util.createRemoveOperation(address);
        migrateOperations.put(address, removeLegacySubsystemOperation);

        if(standalone) {
            removeLegacySubsystemOperation = createRemoveOperation(JACORB_EXTENSION);
            migrateOperations.put(JACORB_EXTENSION, removeLegacySubsystemOperation);
        }
    }

    private Map<PathAddress, ModelNode> migrateSubsystems(OperationContext context, final Map<PathAddress, ModelNode> migrationOperations) throws OperationFailedException {
        final Map<PathAddress, ModelNode> result = new LinkedHashMap<>();
        MultistepUtil.recordOperationSteps(context, migrationOperations, result);
        return result;
    }
}

