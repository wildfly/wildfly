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


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CRITERIA;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.InterfaceDescription;
import org.jboss.as.controller.interfaces.ParsedInterfaceCriteria;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Handler for the interface resource add operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class InterfaceAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = ADD;

    public static ModelNode getAddInterfaceOperation(ModelNode address, ModelNode criteria) {
        ModelNode op = Util.getEmptyOperation(ADD, address);
        op.get(CRITERIA).set(criteria);
        return op;
    }

    public static final InterfaceAddHandler NAMED_INSTANCE = new InterfaceAddHandler(false);

    public static final InterfaceAddHandler SPECIFIED_INSTANCE = new InterfaceAddHandler(true);

    private final boolean specified;

    /**
     * Create the InterfaceAddHandler
     */
    protected InterfaceAddHandler(boolean specified) {
        this.specified = specified;
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        final ModelNode opAddr = operation.get(OP_ADDR);
        PathAddress address = PathAddress.pathAddress(opAddr);
        String name = address.getLastElement().getValue();
        model.get(NAME).set(name);
        ModelNode criteriaNode = operation.get(CRITERIA);
        model.get(CRITERIA).set(criteriaNode);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        String name = getInterfaceName(operation);
        ParsedInterfaceCriteria parsed = getCriteria(operation);
        if (parsed.getFailureMessage() != null) {
            throw new OperationFailedException(new ModelNode().set(parsed.getFailureMessage()));
        }
        performRuntime(context, operation, model, verificationHandler, newControllers, name, parsed);
    }

    protected String getInterfaceName(ModelNode operation) {
        final ModelNode opAddr = operation.get(OP_ADDR);
        PathAddress address = PathAddress.pathAddress(opAddr);
        String name = address.getLastElement().getValue();
        return name;
    }

    protected ParsedInterfaceCriteria getCriteria(ModelNode operation) {
        ModelNode criteriaNode = operation.get(CRITERIA);
        return ParsedInterfaceCriteria.parse(criteriaNode.clone(), specified);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers, String name, ParsedInterfaceCriteria criteria) {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return specified ? InterfaceDescription.getSpecifiedInterfaceAddOperation(locale) : InterfaceDescription.getNamedInterfaceAddOperation(locale);
    }

}
