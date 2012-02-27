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


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.Locale;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.PathDescription;
import org.jboss.as.controller.services.path.AbstractPathService;
import org.jboss.dmr.ModelNode;

/**
 * Handler for the path resource remove operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class PathRemoveHandler extends AbstractRemoveStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = REMOVE;

    private final boolean services;

    public static ModelNode getRemovePathOperation(ModelNode address) {
        ModelNode op = new ModelNode();
        op.get(OP).set(OPERATION_NAME);
        op.get(OP_ADDR).set(address);
        return op;
    }

    public static final PathRemoveHandler NAMED_INSTANCE = new PathRemoveHandler(false);
    public static PathRemoveHandler SPECIFIED_INSTANCE = new PathRemoveHandler(true);
    public static PathRemoveHandler SPECIFIED_NO_SERVICES_INSTANCE = new PathRemoveHandler(false);

    /**
     * Create the PathRemoveHandler
     */
    protected PathRemoveHandler(final boolean services) {
        this.services = services;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return PathDescription.getPathRemoveOperation(locale);
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return services;
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        //This will only get called for the specified variety
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        String name = address.getLastElement().getValue();
        context.removeService(AbstractPathService.pathNameOf(name));
    }

}
