/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.webservices.dmr;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CLASS;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONTEXT;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_NAME;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_TYPE;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_WSDL;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * Registers WS endpoint into webservices subsystem model.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class EndpointAdd extends AbstractAddStepHandler {

    static final EndpointAdd INSTANCE = new EndpointAdd();

    private EndpointAdd() {
        // forbidden instantiation
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        final ModelNode opAddr = operation.require(OP_ADDR);
        final PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();

        // Get operation parameters
        final String endpointName = operation.require(ENDPOINT_NAME).asString();
        final String endpointContext = operation.require(ENDPOINT_CONTEXT).asString();
        final String endpointClass = operation.require(ENDPOINT_CLASS).asString();
        final String endpointType = operation.require(ENDPOINT_TYPE).asString();
        final String endpointWSDL = operation.require(ENDPOINT_WSDL).asString();

        model.get(NAME).set(name);
        model.get(ENDPOINT_NAME).set(endpointName);
        model.get(ENDPOINT_CONTEXT).set(endpointContext);
        model.get(ENDPOINT_CLASS).set(endpointClass);
        model.get(ENDPOINT_TYPE).set(endpointType);
        model.get(ENDPOINT_WSDL).set(endpointWSDL);
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }
}
