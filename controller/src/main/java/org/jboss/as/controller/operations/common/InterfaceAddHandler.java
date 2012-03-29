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


import org.jboss.as.controller.AttributeDefinition;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.InterfaceDescription;
import org.jboss.as.controller.interfaces.ParsedInterfaceCriteria;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Handler for the interface resource add operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author Emanuel Muckenhuber
 */
public class InterfaceAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    private static final AttributeDefinition[] ATTRIBUTES = InterfaceDescription.ROOT_ATTRIBUTES;
    public static final String OPERATION_NAME = ADD;

    public static ModelNode getAddInterfaceOperation(ModelNode address, ModelNode criteria) {
        ModelNode op = Util.getEmptyOperation(ADD, address);
        for(final AttributeDefinition def : ATTRIBUTES) {
            if(criteria.hasDefined(def.getName())) {
                op.get(def.getName()).set(criteria.get(def.getName()));
            }
        }
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

    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {

        PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
        model.get(ModelDescriptionConstants.NAME).set(address.getLastElement().getValue());

        for(final AttributeDefinition definition : ATTRIBUTES) {
            if(specified || operation.hasDefined(definition.getName())) {
                validateAndSet(definition, operation, model);
            }
        }
    }

    protected void validateAndSet(final AttributeDefinition definition, final ModelNode operation, final ModelNode subModel) throws OperationFailedException {
        final String attributeName = definition.getName();
        final boolean has = operation.has(attributeName);
        if(! has && definition.isRequired(operation)) {
            throw new OperationFailedException(new ModelNode().set(MESSAGES.required(attributeName)));
        }
        if(has) {
            if(! definition.isAllowed(operation)) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.invalid(attributeName)));
            }
            definition.validateAndSet(operation, subModel);
        } else {
            // create the undefined node
            subModel.get(definition.getName());
        }
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        String name = getInterfaceName(operation);
        ParsedInterfaceCriteria parsed = getCriteria(context, operation);
        if (parsed.getFailureMessage() != null) {
            throw new OperationFailedException(new ModelNode().set(parsed.getFailureMessage()));
        }
        performRuntime(context, operation, model, verificationHandler, newControllers, name, parsed);
    }

    protected String getInterfaceName(ModelNode operation) {
        final ModelNode opAddr = operation.require(OP_ADDR);
        return PathAddress.pathAddress(opAddr).getLastElement().getValue();
    }

    protected ParsedInterfaceCriteria getCriteria(OperationContext context, ModelNode operation) {
        return ParsedInterfaceCriteria.parse(operation, specified, context);
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers, String name, ParsedInterfaceCriteria criteria) {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return specified ? InterfaceDescription.getSpecifiedInterfaceAddOperation(locale) : InterfaceDescription.getNamedInterfaceAddOperation(locale);
    }

}
