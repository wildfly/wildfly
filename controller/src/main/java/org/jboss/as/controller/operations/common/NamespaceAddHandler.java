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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
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

/**
 * Handler for the root resource add-namespace operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class NamespaceAddHandler extends AbstractModelUpdateHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = "add-namespace";

    public static final NamespaceAddHandler INSTANCE = new NamespaceAddHandler();

    public static ModelNode getAddNamespaceOperation(ModelNode address, String prefix, String uri) {
        ModelNode op = new ModelNode();
        op.get(OP).set(OPERATION_NAME);
        op.get(OP_ADDR).set(address);
        op.get(NAMESPACE).set(prefix);
        op.get(URI).set(uri);
        return op;
    }

    private final ParameterValidator validator = new ModelTypeValidator(ModelType.STRING);


    /**
     * Create the AddNamespaceHandler
     */
    private NamespaceAddHandler() {
    }

    protected void updateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        ModelNode ns = operation.get(NAMESPACE);
        ModelNode uri = operation.get(URI);
        ModelNode namespaces = model.get(NAMESPACES);
        validate(ns, uri, namespaces);
        namespaces.add(ns.asString(), uri.asString());
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return CommonDescriptions.getAddNamespaceOperation(locale);
    }

    private void validate(ModelNode namespace, ModelNode uri, ModelNode namespaces) throws OperationFailedException {
        validator.validateParameter(NAMESPACE, namespace);
        validator.validateParameter(URI, uri);
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
