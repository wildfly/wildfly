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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;

import org.jboss.as.controller.AbstractModelUpdateHandler;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Handler for the root resource add-schema-location operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SchemaLocationAddHandler extends AbstractModelUpdateHandler {

    private static final String OPERATION_NAME = "add-schema-location";

    private static final SimpleAttributeDefinition SCHEMA_LOCATION = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SCHEMA_LOCATION, ModelType.STRING)
            .setAllowNull(false)
            .setValidator(new ModelTypeValidator(ModelType.STRING, false))
            .build();

    private static final SimpleAttributeDefinition URI = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.URI, ModelType.STRING)
            .setAllowNull(false)
            .setValidator(new ModelTypeValidator(ModelType.STRING, false))
            .build();

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, ControllerResolver.getResolver("schema-locations"))
            .setParameters(SCHEMA_LOCATION, URI)
            .build();

    public static final SchemaLocationAddHandler INSTANCE = new SchemaLocationAddHandler();

    public static ModelNode getAddSchemaLocationOperation(ModelNode address, String schemaUrl, String schemaLocation) {
        ModelNode op = Util.createOperation(OPERATION_NAME, PathAddress.pathAddress(address));
        op.get(URI.getName()).set(schemaUrl);
        op.get(SCHEMA_LOCATION.getName()).set(schemaLocation);
        return op;
    }

    /**
     * Create the AddSchemaLocationHandler
     */
    private SchemaLocationAddHandler() {
    }

    protected void updateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        ModelNode uri = URI.resolveModelAttribute(ExpressionResolver.REJECTING, operation);
        ModelNode location = SCHEMA_LOCATION.resolveModelAttribute(ExpressionResolver.REJECTING, operation);
        ModelNode locations = model.get(SCHEMA_LOCATIONS);
        validate(uri, locations);
        locations.add(uri.asString(), location.asString());
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }

    private void validate(ModelNode uri, ModelNode locations) throws OperationFailedException {
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
