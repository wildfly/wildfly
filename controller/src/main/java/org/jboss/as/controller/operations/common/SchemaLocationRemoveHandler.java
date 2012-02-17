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


import java.util.Locale;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URI;

import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Handler for the root resource remove-schema-location operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SchemaLocationRemoveHandler implements OperationStepHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "remove-schema-location";

    public static final SchemaLocationRemoveHandler INSTANCE = new SchemaLocationRemoveHandler();

    public static ModelNode getRemoveSchemaLocationOperation(ModelNode address, String schemaURI) {
        ModelNode op = new ModelNode();
        op.get(OP).set(OPERATION_NAME);
        op.get(OP_ADDR).set(address);
        op.get(URI).set(schemaURI);
        return op;
    }

    private final ParameterValidator typeValidator = new ModelTypeValidator(ModelType.STRING);

    /**
     * Create the RemoveSchemaLocationHandler
     */
    private SchemaLocationRemoveHandler() {
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        ModelNode param = operation.get(URI);
        typeValidator.validateParameter(URI, param);

        ModelNode locations = model.get(SCHEMA_LOCATIONS);
        Property toRemove = null;
        ModelNode newList = new ModelNode().setEmptyList();
        String uri = param.asString();
        if (locations.isDefined()) {
            for (Property location : locations.asPropertyList()) {
                if (uri.equals(location.getName())) {
                    toRemove = location;
                } else {
                    newList.add(location.getName(), location.getValue());
                }
            }
        }
        if (toRemove != null) {
            locations.set(newList);
        } else {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.schemaNotFound(uri)));
        }

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return CommonDescriptions.getRemoveSchemaLocationOperation(locale);
    }

}
