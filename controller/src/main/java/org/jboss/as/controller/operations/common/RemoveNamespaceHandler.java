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
import org.jboss.dmr.Property;

/**
 * Handler for the root resource remove-namespace operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class RemoveNamespaceHandler implements ModelUpdateOperationHandler, DescriptionProvider {

    public static final String REMOVE_NAMESPACE = "remove-namespace";

    public static final RemoveNamespaceHandler INSTANCE = new RemoveNamespaceHandler();

    private final ParameterValidator typeValidator = new ModelTypeValidator(ModelType.STRING);

    public static ModelNode getRemoveNamespaceOperation(ModelNode address, String prefix) {
        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE_NAMESPACE);
        op.get(OP_ADDR).set(address);
        op.get(NAMESPACE).set(prefix);
        return op;
    }

    /**
     * Create the RemoveNamespaceHandler
     */
    private RemoveNamespaceHandler() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
        try {
            ModelNode param = operation.get(NAMESPACE);
            ModelNode namespaces = context.getSubModel().get(NAMESPACES);
            ModelNode toRemove = null;
            String failure = typeValidator.validateParameter(NAMESPACE, param);
            if (failure == null) {
                ModelNode newList = new ModelNode().setEmptyList();
                if (namespaces.isDefined()) {
                    String name = param.asProperty().getName();
                    for (Property namespace : namespaces.asPropertyList()) {
                        if (!name.equals(namespace.getName())) {
                            toRemove = newList.add(namespace.getName(), namespace.getValue());
                            break;
                        }
                    }
                }

                if (toRemove != null) {
                    namespaces.set(newList);
                    ModelNode compensating = AddNamespaceHandler.getAddNamespaceOperation(operation.get(OP_ADDR), toRemove);
                    resultHandler.handleResultComplete(compensating);
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
        return CommonAttributes.getRemoveNamespaceOperation(locale);
    }

}
