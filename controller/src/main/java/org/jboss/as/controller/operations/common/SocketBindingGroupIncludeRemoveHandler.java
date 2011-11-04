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
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.descriptions.common.SocketBindingGroupDescription;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handler for the domain socket-binding-group resource's remove-included-group operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SocketBindingGroupIncludeRemoveHandler extends AbstractRemoveStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = "remove-included-group";

    public static final SocketBindingGroupIncludeRemoveHandler INSTANCE = new SocketBindingGroupIncludeRemoveHandler();

    public static ModelNode getOperation(ModelNode address, String group) {
        ModelNode op = new ModelNode();
        op.get(OP).set(OPERATION_NAME);
        op.get(OP_ADDR).set(address);
        op.get(INCLUDE).set(group);
        return op;
    }

    private final ParameterValidator typeValidator = new ModelTypeValidator(ModelType.STRING);

    /**
     * Create the SocketBindingGroupIncludeRemoveHandler
     */
    private SocketBindingGroupIncludeRemoveHandler() {
    }

    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        ModelNode param = operation.get(INCLUDE);
        ModelNode includes = model.get(INCLUDE);
        ModelNode toRemove = null;
        typeValidator.validateParameter(INCLUDE, param);

        ModelNode newList = new ModelNode().setEmptyList();
        String group = param.asString();
        if (includes.isDefined()) {
            for (ModelNode included : includes.asList()) {
                if (!group.equals(included.asString())) {
                    toRemove = newList.add(included);
                    break;
                }
            }
        }

        if (toRemove != null) {
            includes.set(newList);
        } else {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.groupNotFound(group)));
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return SocketBindingGroupDescription.getRemoveSocketBindingGroupIncludeOperation(locale);
    }

}
