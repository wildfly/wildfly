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
package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDES;

import java.util.Locale;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.common.SocketBindingGroupDescription;
import org.jboss.as.controller.operations.common.AbstractSocketBindingGroupAddHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.ListValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;

/**
 * Handler for the domain socket-binding-group resource's add operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public class SocketBindingGroupAddHandler extends AbstractSocketBindingGroupAddHandler {

    private static final ParametersValidator VALIDATOR = new ParametersValidator();
    static {
        VALIDATOR.registerValidator(INCLUDES, new ListValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true), true, 0, Integer.MAX_VALUE));
    }

    public static final ModelNode getOperation(ModelNode address, ModelNode model) {
        ModelNode op = Util.getEmptyOperation(ADD, address);
        op.get(DEFAULT_INTERFACE).set(model.get(DEFAULT_INTERFACE));
        op.get(INCLUDES).set(model.get(INCLUDES));
        return op;
    }

    public static final SocketBindingGroupAddHandler INSTANCE = new SocketBindingGroupAddHandler();

    private SocketBindingGroupAddHandler() {
        super(VALIDATOR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return SocketBindingGroupDescription.getDomainSocketBindingGroupAddOperation(locale);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        super.populateModel(operation, model);
        model.get(INCLUDES).set(operation.get(INCLUDES));
    }

}
