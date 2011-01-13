/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.remoting;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.remoting.CommonAttributes.AUTHENTICATION_PROVIDER;
import static org.jboss.as.remoting.CommonAttributes.PROPERTIES;
import static org.jboss.as.remoting.CommonAttributes.SASL;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * @author Emanuel Muckenhuber
 */
public class NewConnectorRemove implements RuntimeOperationHandler, ModelRemoveOperationHandler {

    static final OperationHandler INSTANCE = new NewConnectorRemove();

    /** {@inheritDoc} */
    @Override
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final ModelNode address = operation.get(ADDRESS);
        final String connectorName = address.get(address.asInt() - 1).asString();
        final ModelNode connector = context.getSubModel();

        final ModelNode compensating = new ModelNode();
        compensating.get(ADDRESS).set(operation.require(ADDRESS));
        compensating.get(OPERATION).set("add-connector");
        // compensating.get(REQUEST_PROPERTIES, NAME).set(connectorName);
        compensating.get(REQUEST_PROPERTIES, SASL).set(connector.get(SASL));
        compensating.get(REQUEST_PROPERTIES, AUTHENTICATION_PROVIDER).set(connector.get(AUTHENTICATION_PROVIDER));
        compensating.get(REQUEST_PROPERTIES, PROPERTIES).set(connector.get(PROPERTIES));

        // connector.clear();

        if(context instanceof NewRuntimeOperationContext) {
            final ServiceController<?> controller = ((NewRuntimeOperationContext)context).getServiceRegistry().getService(ConnectorElement.connectorName(connectorName));
            if(controller != null) {
                controller.addListener(new AbstractServiceListener<Object>() {
                    @Override
                    public void listenerAdded(final ServiceController<? extends Object> controller) {
                        controller.setMode(Mode.REMOVE);
                    }
                    @Override
                    public void serviceRemoved(final ServiceController<? extends Object> controller) {
                        //
                    }
                });
            }
        }
        resultHandler.handleResultComplete(compensating);

        return Cancellable.NULL;
    }

}
