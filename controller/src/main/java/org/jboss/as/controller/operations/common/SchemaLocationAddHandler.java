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
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Handler for the root resource add-schema-location operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SchemaLocationAddHandler implements ModelUpdateOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "add-schema-location";

    public static final SchemaLocationAddHandler INSTANCE = new SchemaLocationAddHandler();

    public static ModelNode getAddSchemaLocationOperation(ModelNode address, Property schemaLocation) {
        ModelNode op = new ModelNode();
        op.get(OP).set(OPERATION_NAME);
        op.get(OP_ADDR).set(address);
        op.get(SCHEMA_LOCATION).set(schemaLocation);
        return op;
    }

    private final ParameterValidator typeValidator = new ModelTypeValidator(ModelType.PROPERTY);

    /**
     * Create the AddSchemaLocationHandler
     */
    private SchemaLocationAddHandler() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cancellable execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {
        try {
            ModelNode param = operation.get(SCHEMA_LOCATION);
            ModelNode locations = context.getSubModel().get(SCHEMA_LOCATIONS);
            String failure = validate(param, locations);
            if (failure == null) {
                Property loc = param.asProperty();
                locations.add(loc.getName(), loc.getValue());
                ModelNode compensating = SchemaLocationRemoveHandler.getRemoveSchemaLocationOperation(operation.get(OP_ADDR), param.asProperty().getName());
                resultHandler.handleResultComplete(compensating);
            }
            else {
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
        return CommonDescriptions.getAddSchemaLocationOperation(locale);
    }

    private String validate(ModelNode param, ModelNode locations) {
        String failure = typeValidator.validateParameter(SCHEMA_LOCATION, param);
        String uri = param.asProperty().getName();
        if (failure == null && locations.isDefined()) {
            for (ModelNode node : locations.asList()) {
                if (uri.equals(node.asProperty().getName())) {
                    failure = "Schema with URI " + uri + " already registered with location " + node.asProperty().getValue().asString();
                }
            }
        }
        return failure;
    }

}
