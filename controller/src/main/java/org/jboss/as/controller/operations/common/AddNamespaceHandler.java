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


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

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

/**
 * Handler for the root resource add-namespace operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class AddNamespaceHandler implements ModelUpdateOperationHandler, DescriptionProvider {

    public static final String ADD_NAMESPACE = "add-namespace";

    public static final AddNamespaceHandler INSTANCE = new AddNamespaceHandler();

    private final ParameterValidator typeValidator = new ModelTypeValidator(ModelType.PROPERTY);

    public static ModelNode getAddNamespaceOperation(ModelNode address, ModelNode namespace) {
        ModelNode op = new ModelNode();
        op.get(OP).set(ADD_NAMESPACE);
        op.get(OP_ADDR).set(address);
        op.get(NAMESPACE).set(namespace);
        return op;
    }

    /**
     * Create the AddNamespaceHandler
     */
    private AddNamespaceHandler() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
        try {
            ModelNode param = operation.get(NAMESPACE);
            ModelNode namespaces = context.getSubModel().get(NAMESPACES);
            String failure = validate(param, namespaces);
            if (failure == null) {
                namespaces.add(param);
                ModelNode compensating = RemoveNamespaceHandler.getRemoveNamespaceOperation(operation.get(OP_ADDR), param.asProperty().getName());
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
        return CommonAttributes.getAddNamespaceOperation(locale);
    }

    private String validate(ModelNode param, ModelNode namespaces) {
        String failure = typeValidator.validateParameter(NAMESPACE, param);
        String name = param.asProperty().getName();
        if (failure == null && !namespaces.isDefined()) {
            for (ModelNode node : namespaces.asList()) {
                if (name.equals(node.asProperty().getName())) {
                    failure = "Namespace with prefix " + name + " already registered with schema URI " + node.asProperty().getValue().asString();
                }
            }
        }
        return failure;
    }

}
