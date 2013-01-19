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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;

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

/**
 * Handler for the root resource add-namespace operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class NamespaceAddHandler extends AbstractModelUpdateHandler {

    private static final String OPERATION_NAME = "add-namespace";

    private static final SimpleAttributeDefinition NAMESPACE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.NAMESPACE, ModelType.STRING)
            .setAllowNull(false)
            .setValidator(new ModelTypeValidator(ModelType.STRING, false))
            .build();

    private static final SimpleAttributeDefinition URI = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.URI, ModelType.STRING)
            .setAllowNull(false)
            .setValidator(new ModelTypeValidator(ModelType.STRING, false))
            .build();

    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, ControllerResolver.getResolver("namespaces"))
            .setParameters(NAMESPACE, URI)
            .build();

    public static final NamespaceAddHandler INSTANCE = new NamespaceAddHandler();

    public static ModelNode getAddNamespaceOperation(ModelNode address, String prefix, String uri) {
        ModelNode op = Util.createOperation(OPERATION_NAME, PathAddress.pathAddress(address));
        op.get(NAMESPACE.getName()).set(prefix);
        op.get(URI.getName()).set(uri);
        return op;
    }


    /**
     * Create the AddNamespaceHandler
     */
    private NamespaceAddHandler() {
    }

    protected void updateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        ModelNode ns = NAMESPACE.resolveModelAttribute(ExpressionResolver.REJECTING, operation);
        ModelNode uri = URI.resolveModelAttribute(ExpressionResolver.REJECTING, operation);
        ModelNode namespaces = model.get(NAMESPACES);
        validate(ns, namespaces);
        namespaces.add(ns.asString(), uri.asString());
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }

    private void validate(ModelNode namespace, ModelNode namespaces) throws OperationFailedException {

        if (namespaces.isDefined()) {
            String namespaceString = namespace.asString();
            for (ModelNode node : namespaces.asList()) {
                if (namespaceString.equals(node.asProperty().getName())) {
                    throw new OperationFailedException(new ModelNode().set(MESSAGES.namespaceAlreadyRegistered(namespaceString, node.asProperty().getValue().asString())));
                }
            }
        }
    }

}
