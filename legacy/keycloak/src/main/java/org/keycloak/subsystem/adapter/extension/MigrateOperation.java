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
package org.keycloak.subsystem.adapter.extension;

import static org.jboss.as.controller.OperationContext.Stage.MODEL;
import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.operations.common.Util.createAddOperation;
import static org.jboss.as.controller.operations.common.Util.createOperation;
import static org.jboss.as.controller.operations.common.Util.createRemoveOperation;
import static org.keycloak.subsystem.adapter.logging.KeycloakLogger.ROOT_LOGGER;

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
 * Operation to migrate from the legacy keycloak subsystem to the elytron-oidc-client subsystem.
 *
 * <p/>
 * This operation must be performed when the server is in admin-only mode.
 * <p/>
 *
 * <p>
 * Internally, the operation:
 * <p/>
 *
 * <ul>
 *     <li>queries the description of the entire keycloak subsystem by invoking the :describe operation.
 *     This returns a list of :add operations for each keycloak resource.</li>
 *     <li>:add the new org.wildfly.extension.elytron-oidc-client extension if necessary</li>
 *     <li>for each keycloak resource, transform the :add operations to add the corresponding resource to the
 *     new elytron-oidc-client subsystem. In this step, changes to the resources model are taken into account</li>
 *     <li>:remove the keycloak subsystem</li>
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

    private static final String ELYTRON_OIDC_EXTENSION = "org.wildfly.extension.elytron-oidc-client";
    private static final String ELYTRON_OIDC = "elytron-oidc-client";
    private static final String KEYCLOAK_EXTENSION = "org.keycloak.keycloak-adapter-subsystem";
    private static final PathAddress KEYCLOAK_EXTENSION_ADDRESS = pathAddress(pathElement(EXTENSION, KEYCLOAK_EXTENSION));

    private static final OperationStepHandler DESCRIBE_MIGRATION_INSTANCE = new MigrateOperation(true);
    private static final OperationStepHandler MIGRATE_INSTANCE = new MigrateOperation(false);

    private static final String MIGRATE = "migrate";
    private static final String MIGRATION_WARNINGS = "migration-warnings";
    private static final String MIGRATION_ERROR = "migration-error";
    private static final String MIGRATION_OPERATIONS = "migration-operations";
    private static final String DESCRIBE_MIGRATION = "describe-migration";
    private static final String SECRET = "secret";
    private static final String REPLACEMENT = "replacement";

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

        // invoke an OSH to describe the legacy keycloak subsystem
        describeLegacyKeycloakResources(context, legacyModelAddOps);

        // invoke an OSH to add the elytron-oidc-client extension if necessary
        addElytronOidcExtension(context, migrationOperations, describe);

        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                // transform the legacy add operations and put them in migrationOperations
                transformResources(context, legacyModelAddOps, migrationOperations, warnings);

                // add the /subsystem=keycloak:remove operation
                removeKeycloak(migrationOperations, context.getProcessType() == ProcessType.STANDALONE_SERVER);

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

    private void describeLegacyKeycloakResources(OperationContext context, ModelNode legacyModelDescription) {
        ModelNode describeLegacySubsystem = createOperation(GenericSubsystemDescribeHandler.DEFINITION, context.getCurrentAddress());
        context.addStep(legacyModelDescription, describeLegacySubsystem, GenericSubsystemDescribeHandler.INSTANCE, MODEL, true);
    }

    /**
     * Attempt to add the elytron-oidc-client extension. If it's already present, nothing is done.
     */
    private void addElytronOidcExtension(OperationContext context, Map<PathAddress, ModelNode> migrationOperations, boolean describe) {
        Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false);
        if (root.getChildrenNames(EXTENSION).contains(ELYTRON_OIDC_EXTENSION)) {
            return; // extension is already added, nothing to do
        }
        PathAddress extensionAddress = pathAddress(EXTENSION, ELYTRON_OIDC_EXTENSION);
        OperationEntry addEntry = context.getRootResourceRegistration().getOperationEntry(extensionAddress, ADD);
        ModelNode addOperation = createAddOperation(extensionAddress);
        addOperation.get(MODULE).set(ELYTRON_OIDC_EXTENSION);
        if (describe) {
            migrationOperations.put(extensionAddress, addOperation);
        } else {
            context.addStep(context.getResult().get(extensionAddress.toString()), addOperation, addEntry.getOperationHandler(), MODEL);
        }
    }

    private void transformResources(OperationContext context, final ModelNode legacyModelDescription, final Map<PathAddress, ModelNode> newAddOperations,
                                    List<String> warnings) throws OperationFailedException {
        List<String> credentialNames = new ArrayList<>();
        for (ModelNode legacyAddOp : legacyModelDescription.get(RESULT).asList()) {
            final ModelNode newAddOp = legacyAddOp.clone();
            ModelNode legacyAddress = legacyAddOp.get(OP_ADDR);

            ModelNode newAddress = transformAddress(legacyAddress.clone(), context);
            if (! newAddress.isDefined()) {
                continue;
            }
            newAddOp.get(OP_ADDR).set(newAddress);
            PathAddress address = PathAddress.pathAddress(newAddress);
            if (newAddress.asList().size() == 2 && SecureServerDefinition.TAG_NAME.equals(address.getLastElement().getKey())) {
                // elytron-oidc-client doesn't support the secure-server resource yet
                warnings.add(ROOT_LOGGER.couldNotMigrateUnsupportedSecureServerResource());
                continue;
            }

            if (newAddress.asList().size() > 2) {
                // element 0 is subsystem=elytron-oidc-client
                String childType = address.getElement(2).getKey();
                if (childType.equals(CredentialDefinition.TAG_NAME)) {
                    migrateCredential(newAddOp, address.getElement(2).getValue(), credentialNames);
                } else if (childType.equals(RedirecRewritetRuleDefinition.TAG_NAME)) {
                    migrateRedirectRewriteRule(newAddOp);
                }
            }

            newAddOperations.put(address, newAddOp);
        }
    }

    private ModelNode transformAddress(ModelNode legacyAddress, OperationContext context) {
        ModelNode newAddress = new ModelNode();
        if (legacyAddress.asPropertyList().size() == 1) {
            Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false);
            if (root.getChildrenNames(SUBSYSTEM).contains(ELYTRON_OIDC)) {
                return new ModelNode(); // elytron-oidc-client subsystem is already present, no need to add it
            }
        }
        for (Property segment : legacyAddress.asPropertyList()) {
            final Property newSegment;
            switch (segment.getName()) {
                case SUBSYSTEM:
                    newSegment = new Property(SUBSYSTEM, new ModelNode(ELYTRON_OIDC));
                    break;
                default:
                    newSegment = segment;
            }
            newAddress.add(newSegment);
        }
        return newAddress;
    }

    private void removeKeycloak(Map<PathAddress, ModelNode> migrationOperations, boolean standalone) {
        PathAddress subsystemAddress =  pathAddress(KeycloakExtension.PATH_SUBSYSTEM);
        ModelNode removeOperation = createRemoveOperation(subsystemAddress);
        migrationOperations.put(subsystemAddress, removeOperation);
        if (standalone) {
            removeOperation = createRemoveOperation(KEYCLOAK_EXTENSION_ADDRESS);
            migrationOperations.put(KEYCLOAK_EXTENSION_ADDRESS, removeOperation);
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

    private void migrateCredential(ModelNode newOp, String credentialType, List<String> credentialNames) {
        // complex legacy credential types will be of the form name:attribute (e.g., jwt.client-key-password)
        // (each credential attribute type was previously a resource, need to update the op to set attributes
        // for the credential resource instead)
        String[] nameAndType = credentialType.split("\\.");
        String name = nameAndType[0];
        boolean useWriteAttributeOp = false;
        if (credentialNames.contains(name)) {
            useWriteAttributeOp = true;
        } else {
            credentialNames.add(name);
        }
        switch (name) {
            case SECRET:
                updateOp(newOp, SECRET, useWriteAttributeOp);
                break;
            default:
                String type = nameAndType[1];
                updateOp(newOp, type, useWriteAttributeOp);
                newOp.get(ADDRESS).asList().get(2).get(CredentialDefinition.TAG_NAME).set(name);
                break;
        }
    }

    private void migrateRedirectRewriteRule(ModelNode newAddOp) {
        // <redirect-rewrite-rule name="..." replacement="VALUE"/> instead of <redirect-rewrite-rule name="...">VALUE</redirect-rewrite-rule>
        newAddOp.get(REPLACEMENT).set(newAddOp.get(VALUE).asString());
        newAddOp.remove(VALUE);
    }

    private void updateOp(ModelNode newOp, String attributeName, boolean useWriteAttributeOp) {
        if (useWriteAttributeOp) {
            newOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            newOp.get(NAME).set(attributeName);
        } else {
            newOp.get(attributeName).set(newOp.get(VALUE).asString());
            newOp.remove(VALUE);
        }
    }
}
