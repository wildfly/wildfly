/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.picketlink.federation;

import static org.jboss.as.controller.OperationContext.Stage.MODEL;
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.operations.common.Util.createAddOperation;
import static org.jboss.as.controller.operations.common.Util.createOperation;
import static org.jboss.as.controller.operations.common.Util.createRemoveOperation;
import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
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
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Operation to migrate from the legacy picketlink-federation subsystem to the keycloak-saml subsystem.
 *
 * <p/>
 * This operation must be performed when the server is in admin-only mode.
 * <p/>
 *
 * <p>
 * This operation assumes that the user has already obtained the Keycloak client SAML adapter and installed it.
 * <p/>
 *
 * <p>
 * Internally, the operation:
 * <p/>
 *
 * <ul>
 *     <li>queries the description of the entire picketlink-federation subsystem by invoking the :describe operation.
 *     This returns a list of :add operations for each picketlink-federation resource.</li>
 *     <li>:add the new org.keycloak.keycloak-saml-adapter-subsystem extension if necessary (this should already
 *     be added when installing the Keycloak client SAML adapter)</li>
 *     <li>for each picketlink-federation resource, transform the :add operations to add the corresponding resource to the
 *     new keycloak-saml. In this step, changes to the resources model are taken into account</li>
 *     <li>:remove the picketlink-federation subsystem</li>
 * </ul>
 *
 * <p/>
 *
 * The companion <code>:describe-migration</code> operation will return a list of all the actual operations that would
 * be performed during the invocation of the <code>:migrate</code> operation.
 * <p/>
 *
 * Note that all new operation addresses are generated for standalone mode. If this is a domain mode server then the
 * addresses are fixed after they have been generated
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class MigrateOperation implements OperationStepHandler {

    private static final String KEYCLOAK_SAML_EXTENSION = "org.keycloak.keycloak-saml-adapter-subsystem";
    private static final String KEYCLOAK_SAML = "keycloak-saml";
    private static final String PICKETLINK_EXTENSION = "org.wildfly.extension.picketlink";
    private static final PathAddress PICKETLINK_EXTENSION_ADDRESS = pathAddress(pathElement(EXTENSION, PICKETLINK_EXTENSION));

    private static final OperationStepHandler DESCRIBE_MIGRATION_INSTANCE = new MigrateOperation(true);
    private static final OperationStepHandler MIGRATE_INSTANCE = new MigrateOperation(false);

    private static final String MIGRATE = "migrate";
    private static final String MIGRATION_WARNINGS = "migration-warnings";
    private static final String MIGRATION_ERROR = "migration-error";
    private static final String MIGRATION_OPERATIONS = "migration-operations";
    private static final String DESCRIBE_MIGRATION = "describe-migration";

    static final StringListAttributeDefinition MIGRATION_WARNINGS_ATTR = new StringListAttributeDefinition.Builder(MIGRATION_WARNINGS)
            .setRequired(false)
            .build();

    static final SimpleMapAttributeDefinition MIGRATION_ERROR_ATTR = new SimpleMapAttributeDefinition.Builder(MIGRATION_ERROR, ModelType.OBJECT, true)
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
                MigrateOperation.MIGRATE_INSTANCE);
        registry.registerOperationHandler(new SimpleOperationDefinitionBuilder(DESCRIBE_MIGRATION, resourceDescriptionResolver)
                        .setReplyParameters(MIGRATION_WARNINGS_ATTR)
                        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.READ_WHOLE_CONFIG)
                        .setReadOnly()
                        .build(),
                MigrateOperation.DESCRIBE_MIGRATION_INSTANCE);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (! describe && context.getRunningMode() != RunningMode.ADMIN_ONLY) {
            throw ROOT_LOGGER.migrateOperationAllowedOnlyInAdminOnly();
        }

        final List<String> warnings = new ArrayList<>();

        // node containing the description (list of add operations) of the legacy subsystem
        final ModelNode legacyModelAddOps = new ModelNode();

        // preserve the order of insertion of the add operations for the new subsystem.
        final Map<PathAddress, ModelNode> migrationOperations = new LinkedHashMap<>();

        // invoke an OSH to describe the legacy picketlink-federation subsystem
        describeLegacyPicketLinkFederationResources(context, legacyModelAddOps);

        // invoke an OSH to add the keycloak-saml extension
        addKeycloakSamlExtension(context, migrationOperations, describe);

        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                // transform the legacy add operations and put them in migrationOperations
                transformResources(context, legacyModelAddOps, migrationOperations, warnings);

                // put the /subsystem=picketlink-federation:remove operation
                removePicketLinkFederation(migrationOperations, context.getProcessType() == ProcessType.STANDALONE_SERVER);

                PathAddress parentAddress = context.getCurrentAddress().getParent();
                fixAddressesForDomainMode(parentAddress, migrationOperations);

                if (describe) {
                    // :describe-migration operation returns the list of operations that would be executed in the composite operation
                    final Collection<ModelNode> values = migrationOperations.values();
                    ModelNode result = new ModelNode();
                    fillWarnings(result, warnings);
                    result.get(MIGRATION_OPERATIONS).set(values);

                    context.getResult().set(result);
                } else {
                    // :migrate operation invokes an OSH on a composite operation with all the migration operations
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

    private void describeLegacyPicketLinkFederationResources(OperationContext context, ModelNode legacyModelDescription) {
        ModelNode describeLegacySubsystem = createOperation(GenericSubsystemDescribeHandler.DEFINITION, context.getCurrentAddress());
        context.addStep(legacyModelDescription, describeLegacySubsystem, GenericSubsystemDescribeHandler.INSTANCE, MODEL, true);
    }

    /**
     * Attempt to add the keycloak-saml extension. If it's already present, nothing is done.
     */
    private void addKeycloakSamlExtension(OperationContext context, Map<PathAddress, ModelNode> migrationOperations, boolean describe) {
        Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false);
        if (root.getChildrenNames(EXTENSION).contains(KEYCLOAK_SAML_EXTENSION)) {
            return; // extension is already added, nothing to do
        }
        PathAddress extensionAddress = pathAddress(EXTENSION, KEYCLOAK_SAML_EXTENSION);
        OperationEntry addEntry = context.getRootResourceRegistration().getOperationEntry(extensionAddress, ADD);
        ModelNode addOperation = createAddOperation(extensionAddress);
        addOperation.get(MODULE).set(KEYCLOAK_SAML_EXTENSION);
        if (describe) {
            migrationOperations.put(extensionAddress, addOperation);
        } else {
            context.addStep(context.getResult().get(extensionAddress.toString()), addOperation, addEntry.getOperationHandler(), MODEL);
        }
    }

    private void transformResources(OperationContext context, final ModelNode legacyModelDescription, final Map<PathAddress, ModelNode> newAddOperations,
                                    List<String> warnings) throws OperationFailedException {
        for (ModelNode legacyAddOp : legacyModelDescription.get(RESULT).asList()) {
            final ModelNode newAddOp = legacyAddOp.clone();
            ModelNode legacyAddress = legacyAddOp.get(OP_ADDR);

            ModelNode newAddress = transformAddress(legacyAddress.clone(), context);
            if (! newAddress.isDefined()) {
                continue;
            }
            newAddOp.get(OP_ADDR).set(newAddress);

            PathAddress address = PathAddress.pathAddress(newAddress);

            if (address.size() > 1) {
                // non-empty subsystem configuration, fail for now
                throw ROOT_LOGGER.cannotMigrateNonEmptyConfiguration();
            }


            newAddOperations.put(address, newAddOp);
        }
    }

    private ModelNode transformAddress(ModelNode legacyAddress, OperationContext context) {
        ModelNode newAddress = new ModelNode();
        if (legacyAddress.asPropertyList().size() == 1) {
            Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false);
            if (root.getChildrenNames(SUBSYSTEM).contains(KEYCLOAK_SAML)) {
                return new ModelNode(); // keycloak-saml subsystem is already present, no need to add it
            }
        }
        for (Property segment : legacyAddress.asPropertyList()) {
            final Property newSegment;
            switch (segment.getName()) {
                case SUBSYSTEM:
                    newSegment = new Property(SUBSYSTEM, new ModelNode(KEYCLOAK_SAML));
                    break;
                default:
                    newSegment = segment;
            }
            newAddress.add(newSegment);
        }
        return newAddress;
    }

    private void removePicketLinkFederation(Map<PathAddress, ModelNode> migrationOperations, boolean standalone) {
        PathAddress subsystemAddress =  pathAddress(FederationExtension.SUBSYSTEM_PATH);
        ModelNode removeOperation = createRemoveOperation(subsystemAddress);
        migrationOperations.put(subsystemAddress, removeOperation);
        if (standalone) {
            removeOperation = createRemoveOperation(PICKETLINK_EXTENSION_ADDRESS);
            migrationOperations.put(PICKETLINK_EXTENSION_ADDRESS, removeOperation);
        }
    }

    private Map<PathAddress, ModelNode> migrateSubsystems(OperationContext context, final Map<PathAddress, ModelNode> migrationOperations) throws OperationFailedException {
        final Map<PathAddress, ModelNode> result = new LinkedHashMap<>();
        MultistepUtil.recordOperationSteps(context, migrationOperations, result);
        return result;
    }

    /**
     * In domain mode, the subsystems are under /profile=XXX.
     * This method fixes the address by prepending the addresses (that start with /subsystem) with the current
     * operation parent so that is works both in standalone (parent = EMPTY_ADDRESS) and domain mode
     * (parent = /profile=XXX).
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

    private void fillWarnings(ModelNode result, List<String> warnings) {
        ModelNode rw = new ModelNode().setEmptyList();
        for (String warning : warnings) {
            rw.add(warning);
        }
        result.get(MIGRATION_WARNINGS).set(rw);
    }
}
