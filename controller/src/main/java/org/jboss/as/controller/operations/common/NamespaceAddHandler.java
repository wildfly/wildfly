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
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Handler for the root resource add-namespace operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class NamespaceAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = "add-namespace";

    public static final NamespaceAddHandler INSTANCE = new NamespaceAddHandler();

    public static ModelNode getAddNamespaceOperation(ModelNode address, Property namespace) {
        ModelNode op = new ModelNode();
        op.get(OP).set(OPERATION_NAME);
        op.get(OP_ADDR).set(address);
        op.get(NAMESPACE).set(namespace);
        return op;
    }

    private final ParameterValidator typeValidator = new ModelTypeValidator(ModelType.PROPERTY);

    /**
     * Create the AddNamespaceHandler
     */
    private NamespaceAddHandler() {
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        ModelNode param = operation.get(NAMESPACE);
        ModelNode namespaces = model.get(NAMESPACES);
        validate(param, namespaces);
        Property prop = param.asProperty();
        namespaces.add(prop.getName(), prop.getValue());
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return CommonDescriptions.getAddNamespaceOperation(locale);
    }

    private void validate(ModelNode param, ModelNode namespaces) throws OperationFailedException {
        typeValidator.validateParameter(NAMESPACE, param);
        String name = param.asProperty().getName();
        if (namespaces.isDefined()) {
            for (ModelNode node : namespaces.asList()) {
                if (name.equals(node.asProperty().getName())) {
                    throw new OperationFailedException(new ModelNode().set("Namespace with prefix " + name + " already registered with schema URI " + node.asProperty().getValue().asString()));
                }
            }
        }
    }

}
