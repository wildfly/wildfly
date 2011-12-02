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


import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.URI;

import java.util.Locale;

import org.jboss.as.controller.AbstractModelUpdateHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
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
public class SchemaLocationAddHandler extends AbstractModelUpdateHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = "add-schema-location";

    public static final SchemaLocationAddHandler INSTANCE = new SchemaLocationAddHandler();

    public static ModelNode getAddSchemaLocationOperation(ModelNode address, String schemaUrl, String schemaLocation) {
        ModelNode op = new ModelNode();
        op.get(OP).set(OPERATION_NAME);
        op.get(OP_ADDR).set(address);
        op.get(URI).set(schemaUrl);
        op.get(SCHEMA_LOCATION).set(schemaLocation);
        return op;
    }

    private final ParameterValidator stringValidator = new ModelTypeValidator(ModelType.STRING);

    /**
     * Create the AddSchemaLocationHandler
     */
    private SchemaLocationAddHandler() {
    }

    protected void updateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        ModelNode uri = operation.get(URI);
        ModelNode location = operation.get(SCHEMA_LOCATION);
        ModelNode locations = model.get(SCHEMA_LOCATIONS);
        validate(uri, location, locations);
        locations.add(uri.asString(), location.asString());
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return CommonDescriptions.getAddSchemaLocationOperation(locale);
    }

    private void validate(ModelNode uri, ModelNode location, ModelNode locations) throws OperationFailedException {
        stringValidator.validateParameter(URI, uri);
        stringValidator.validateParameter(SCHEMA_LOCATION, location);
        if (locations.isDefined()) {
            String uriString = uri.asString();
            for (Property prop : locations.asPropertyList()) {
                if (uriString.equals(prop.getName())) {
                    throw new OperationFailedException(new ModelNode().set(MESSAGES.schemaAlreadyRegistered(uriString, prop.getValue().asString())));
                }
            }
        }
    }

}
