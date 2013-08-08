/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.operations.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.access.constraint.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * An operation that deletes a snapshot of the current configuration
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SnapshotDeleteHandler implements OperationStepHandler {

    private static final String OPERATION_NAME = "delete-snapshot";

    private static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.NAME, ModelType.STRING)
            .setAllowNull(false)
            .build();

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, ControllerResolver.getResolver("snapshot"))
            .setParameters(NAME)
            .setRuntimeOnly()
            .withFlag(OperationEntry.Flag.MASTER_HOST_CONTROLLER_ONLY)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SNAPSHOTS)
            .build();


    private final ConfigurationPersister persister;

    public SnapshotDeleteHandler(ConfigurationPersister persister) {
        this.persister = persister;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        AuthorizationResult authorizationResult = context.authorize(operation);
        if (authorizationResult.getDecision() == AuthorizationResult.Decision.DENY) {
            throw ControllerMessages.MESSAGES.unauthorized(operation.get(OP).asString(),
                    PathAddress.pathAddress(operation.get(OP_ADDR)), authorizationResult.getExplanation());
        }

        String name = operation.require(ModelDescriptionConstants.NAME).asString();
        try {
            persister.deleteSnapshot(name);
        } catch (Exception e) {
            throw new OperationFailedException(e);
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

}
