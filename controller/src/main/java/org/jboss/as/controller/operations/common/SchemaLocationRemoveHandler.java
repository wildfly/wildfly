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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;

import java.util.Locale;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonAttributes;
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
public class SchemaLocationRemoveHandler implements ModelUpdateOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "remove-schema-location";

    public static final SchemaLocationRemoveHandler INSTANCE = new SchemaLocationRemoveHandler();

    public static ModelNode getRemoveSchemaLocationOperation(ModelNode address, String schemaURI) {
        ModelNode op = new ModelNode();
        op.get(OP).set(OPERATION_NAME);
        op.get(OP_ADDR).set(address);
        op.get(SCHEMA_LOCATION).set(schemaURI);
        return op;
    }

    private final ParameterValidator typeValidator = new ModelTypeValidator(ModelType.STRING);

    /**
     * Create the RemoveSchemaLocationHandler
     */
    private SchemaLocationRemoveHandler() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
        try {
            ModelNode param = operation.get(SCHEMA_LOCATION);
            ModelNode locations = context.getSubModel().get(SCHEMA_LOCATIONS);
            ModelNode toRemove = null;
            String failure = typeValidator.validateParameter(SCHEMA_LOCATION, param);
            if (failure == null) {
                ModelNode newList = new ModelNode().setEmptyList();
                String uri = param.asProperty().getName();
                if (locations.isDefined()) {
                    for (Property location : locations.asPropertyList()) {
                        if (!uri.equals(location.getName())) {
                            toRemove = newList.add(location.getName(), location.getValue());
                            break;
                        }
                    }
                }

                if (toRemove != null) {
                    locations.set(newList);
                    ModelNode compensating = SchemaLocationAddHandler.getAddSchemaLocationOperation(operation.get(OP_ADDR), toRemove);
                    resultHandler.handleResultComplete(compensating);
                }
                else {
                    failure = "No schema location with URI " + uri + "found";
                }
            }

            if (failure != null) {
                resultHandler.handleFailed(new ModelNode().set(failure));
            }
        }
        catch (Exception e) {
            resultHandler.handleFailed(new ModelNode().set(e.getLocalizedMessage()));
        }
        return Cancellable.NULL;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return CommonAttributes.getRemoveSchemaLocationOperation(locale);
    }

}
