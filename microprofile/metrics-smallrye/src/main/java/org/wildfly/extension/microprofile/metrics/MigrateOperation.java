/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.metrics;

import static org.jboss.as.controller.OperationContext.Stage.MODEL;
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsExtension.EXTENSION_NAME;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsExtension.SUBSYSTEM_NAME;
import static org.wildfly.extension.microprofile.metrics._private.MicroProfileMetricsLogger.ROOT_LOGGER;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.MultistepUtil;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class MigrateOperation implements OperationStepHandler {
    private static final PathAddress MP_METRICS_EXTENSION_ELEMENT = pathAddress(pathElement(EXTENSION, EXTENSION_NAME));
    private static final PathAddress MP_METRICS_SUBSYSTEM_ELEMENT = pathAddress(pathElement(SUBSYSTEM, SUBSYSTEM_NAME));
    private static final String MIGRATE = "migrate";
    private static final String MIGRATION_WARNINGS = "migration-warnings";
    private static final String MIGRATION_ERROR = "migration-error";
    private static final String MIGRATION_OPERATIONS = "migration-operations";
    private static final String DESCRIBE_MIGRATION = "describe-migration";

    static final StringListAttributeDefinition MIGRATION_WARNINGS_ATTR =
            new StringListAttributeDefinition.Builder(MIGRATION_WARNINGS)
                    .setRequired(false)
                    .build();

    static final SimpleMapAttributeDefinition MIGRATION_ERROR_ATTR =
            new SimpleMapAttributeDefinition.Builder(MIGRATION_ERROR, ModelType.OBJECT, true)
                    .setValueType(ModelType.OBJECT)
                    .setRequired(false)
                    .build();
    private final boolean describe;

    private MigrateOperation(boolean describe) {
        this.describe = describe;
    }

    static void registerOperations(ManagementResourceRegistration registry, ResourceDescriptionResolver resourceDescriptionResolver) {
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder(MIGRATE, resourceDescriptionResolver)
                        .setReplyParameters(MIGRATION_WARNINGS_ATTR, MIGRATION_ERROR_ATTR)
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.READ_WHOLE_CONFIG)
                        .build(),
                new MigrateOperation(false));
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder(DESCRIBE_MIGRATION, resourceDescriptionResolver)
                        .setReplyParameters(MIGRATION_WARNINGS_ATTR, MIGRATION_ERROR_ATTR)
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.READ_WHOLE_CONFIG)
                        .setReadOnly()
                        .build(),
                new MigrateOperation(true));
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (!describe && context.getRunningMode() != RunningMode.ADMIN_ONLY) {
            throw ROOT_LOGGER.migrateOperationAllowedOnlyInAdminOnly();
        }

        final PathAddress subsystemsAddress = context.getCurrentAddress().getParent();
        final Map<PathAddress, ModelNode> migrateOperations = new LinkedHashMap<>();

        context.addStep((operationContext, modelNode) -> {
            removeMicroProfileMetrics(subsystemsAddress.append(MP_METRICS_SUBSYSTEM_ELEMENT), migrateOperations);

            if (describe) {
                // :describe-migration operation

                // for describe-migration operation, do nothing and return the list of operations that would
                // be executed in the composite operation
                final Collection<ModelNode> values = migrateOperations.values();
                ModelNode result = new ModelNode();

                result.get(MIGRATION_OPERATIONS).set(values);
                result.get(MIGRATION_WARNINGS).set(new ModelNode().setEmptyList());

                context.getResult().set(result);
            } else {
                // :migrate operation
                // invoke an OSH on a composite operation with all the migration operations
                final Map<PathAddress, ModelNode> migrateOpResponses = migrateSubsystems(context, migrateOperations);

                context.completeStep((resultAction, context1, operation1) -> {
                    final ModelNode result = new ModelNode();

                    result.get(MIGRATION_WARNINGS).set(new ModelNode().setEmptyList());

                    if (resultAction == OperationContext.ResultAction.ROLLBACK) {
                        for (Map.Entry<PathAddress, ModelNode> entry : migrateOpResponses.entrySet()) {
                            if (entry.getValue().hasDefined(FAILURE_DESCRIPTION)) {
                                ModelNode desc = new ModelNode();
                                desc.get(OP).set(migrateOperations.get(entry.getKey()));
                                desc.get(RESULT).set(entry.getValue());
                                result.get(MIGRATION_ERROR).set(desc);
                                break;
                            }
                        }
                        context1.getFailureDescription().set(new ModelNode(ROOT_LOGGER.migrationFailed()));
                    }

                    context1.getResult().set(result);
                });
            }
        }, MODEL);
    }

    private void removeMicroProfileMetrics(final PathAddress address,
                                           final Map<PathAddress, ModelNode> migrateOperations) {
        migrateOperations.put(address, Util.createRemoveOperation(address));
        migrateOperations.put(MP_METRICS_EXTENSION_ELEMENT, Util.createRemoveOperation(MP_METRICS_EXTENSION_ELEMENT));
    }

    private Map<PathAddress, ModelNode> migrateSubsystems(OperationContext context,
                                                          final Map<PathAddress,
                                                                  ModelNode> migrationOperations) throws OperationFailedException {
        final Map<PathAddress, ModelNode> result = new LinkedHashMap<>();
        MultistepUtil.recordOperationSteps(context, migrationOperations, result);
        return result;
    }
}
