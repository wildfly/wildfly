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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationPersister.SnapshotInfo;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * An operation that lists the snapshots taken of the current configuration
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class SnapshotListHandler implements OperationStepHandler {

    private static final String OPERATION_NAME = "list-snapshots";

    private static final SimpleAttributeDefinition DIRECTORY = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.DIRECTORY, ModelType.STRING)
            .setAllowNull(false)
            .build();
    private static final AttributeDefinition NAMES = new StringListAttributeDefinition.Builder(ModelDescriptionConstants.NAMES)
            .build();

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, ControllerResolver.getResolver("snapshot"))
            .setReplyParameters(DIRECTORY, NAMES)
            .withFlag(OperationEntry.Flag.MASTER_HOST_CONTROLLER_ONLY)
            .build();

    private final ConfigurationPersister persister;

    public SnapshotListHandler(ConfigurationPersister persister) {
        this.persister = persister;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        try {
            SnapshotInfo info = persister.listSnapshots();
            ModelNode result = context.getResult();
            result.get(ModelDescriptionConstants.DIRECTORY).set(info.getSnapshotDirectory());
            result.get(ModelDescriptionConstants.NAMES).setEmptyList();
            for (String name : info.names()) {
                result.get(ModelDescriptionConstants.NAMES).add(name);
            }
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        } catch (Exception e) {
            throw new OperationFailedException(e.getMessage(), new ModelNode().set(e.getMessage()));
        }
    }

}
