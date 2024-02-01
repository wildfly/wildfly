/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.opentracing;

import static org.jboss.as.controller.OperationContext.Stage.MODEL;
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.microprofile.opentracing.SubsystemExtension.EXTENSION_NAME;
import static org.wildfly.extension.microprofile.opentracing.SubsystemExtension.SUBSYSTEM_NAME;
import static org.wildfly.extension.microprofile.opentracing.TracingExtensionLogger.ROOT_LOGGER;

import java.util.Collection;
import java.util.LinkedHashMap;
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
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.MultistepUtil;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Operation to migrate from the legacy MicroProfile subsystem to new MicroProfile Telemetry subsystem.
 *
 * @author <a href=mailto:jasondlee@redhat.com>Jason Lee</a>
 */

public class MigrateOperation implements OperationStepHandler {
    private static final String OPENTELEMETRY_EXTENSION = "org.wildfly.extension.opentelemetry";
    private static final PathAddress OPENTRACING_EXTENSION_ELEMENT = pathAddress(pathElement(EXTENSION, EXTENSION_NAME));
    private static final PathAddress OPENTRACING_SUBSYSTEM_ELEMENT = pathAddress(pathElement(SUBSYSTEM, SUBSYSTEM_NAME));
    private static final PathElement OPENTELEMETRY_EXTENSION_ELEMENT = PathElement.pathElement(EXTENSION, OPENTELEMETRY_EXTENSION);
    private static final PathElement OPENTELEMETRY_SUBSYSTEM_ELEMENT = PathElement.pathElement(SUBSYSTEM, "opentelemetry");

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

        if (!context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false).hasChild(OPENTELEMETRY_EXTENSION_ELEMENT)) {
            addOpenTelemetryExtension(context, migrateOperations);
        }

        context.addStep((operationContext, modelNode) -> {
            addOpenTelemetrySubsystem(context, migrateOperations);

            final PathAddress opentracingAddress = subsystemsAddress.append(OPENTRACING_SUBSYSTEM_ELEMENT);
            removeOpentracingSubsystem(opentracingAddress, migrateOperations, context.getProcessType() == ProcessType.STANDALONE_SERVER);

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

    private void addOpenTelemetryExtension(final OperationContext context,
                                           final Map<PathAddress, ModelNode> migrateOperations) {
        final PathAddress extensionAddress = PathAddress.EMPTY_ADDRESS.append(OPENTELEMETRY_EXTENSION_ELEMENT);
        OperationEntry addEntry = context.getRootResourceRegistration().getOperationEntry(extensionAddress, ADD);
        final ModelNode addOperation = Util.createAddOperation(extensionAddress);
        if (describe) {
            migrateOperations.put(extensionAddress, addOperation);
        } else {
            context.addStep(context.getResult().get(extensionAddress.toString()), addOperation,
                    addEntry.getOperationHandler(), MODEL);
        }
    }

    private void addOpenTelemetrySubsystem(final OperationContext context,
                                           final Map<PathAddress, ModelNode> migrateOperations) {
        PathAddress parentAddress = context.getCurrentAddress().getParent();
        Resource root = context.readResourceFromRoot(parentAddress, false);
        if (root.hasChild(OPENTELEMETRY_SUBSYSTEM_ELEMENT)) {
            return; // subsystem is already added, nothing to do

        }

        PathAddress subsystemAddress = parentAddress.append(OPENTELEMETRY_SUBSYSTEM_ELEMENT);
        final ModelNode operation = Util.createAddOperation(subsystemAddress);
        migrateOperations.put(subsystemAddress, operation);
    }

    private void removeOpentracingSubsystem(final PathAddress address,
                                            final Map<PathAddress, ModelNode> migrateOperations,
                                            boolean standalone) {
        ModelNode removeLegacySubsystemOperation = Util.createRemoveOperation(address);
        migrateOperations.put(address, removeLegacySubsystemOperation);

        if (standalone) {
            removeLegacySubsystemOperation = Util.createRemoveOperation(OPENTRACING_EXTENSION_ELEMENT);
            migrateOperations.put(OPENTRACING_EXTENSION_ELEMENT, removeLegacySubsystemOperation);
        }
    }

    private Map<PathAddress, ModelNode> migrateSubsystems(OperationContext context,
                                                          final Map<PathAddress,
                                                                  ModelNode> migrationOperations) throws OperationFailedException {
        final Map<PathAddress, ModelNode> result = new LinkedHashMap<>();
        MultistepUtil.recordOperationSteps(context, migrationOperations, result);
        return result;
    }

}
