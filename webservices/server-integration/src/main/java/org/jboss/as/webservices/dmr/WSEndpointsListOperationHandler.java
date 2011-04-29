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
package org.jboss.as.webservices.dmr;

import javax.management.ObjectName;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.management.EndpointRegistry;

/**
* @author <a href="mailto:ema@redhat.com">Jim Ma</a>
*/
public class WSEndpointsListOperationHandler implements OperationHandler {
    public static final String OPERATION_NAME = "list-endpoints";
    static final WSEndpointsListOperationHandler INSTANCE = new WSEndpointsListOperationHandler();

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler)
            throws OperationFailedException {

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceController<?> controller = context.getServiceRegistry()
                            .getService(WSServices.REGISTRY_SERVICE);
                    if (controller != null) {
                        try {
                            final EndpointRegistry registry = (EndpointRegistry) controller.getValue();
                            final ModelNode result = new ModelNode();
                            result.get("deployed-count").set(registry.getEndpoints().size());
                            for (ObjectName obj : registry.getEndpoints()) {
                                Endpoint endpoint = registry.getEndpoint(obj);
                                String endpointName = obj.toString();
                                result.get(endpointName).add("address", endpoint.getAddress());
                                result.get(endpointName).add("wsdlURL", endpoint.getAddress() + "?wsdl");
                                result.get(endpointName).add("shortName", endpoint.getShortName());
                                result.get(endpointName).add("implClass", endpoint.getTargetBeanClass().getName());

                            }
                            resultHandler.handleResultFragment(new String[0], result);
                            resultHandler.handleResultComplete();
                        } catch (Exception e) {
                            throw new OperationFailedException(new ModelNode().set("failed to get webservice endpoints list"
                                    + e.getMessage()));
                        }
                    } else {
                        resultHandler.handleResultFragment(ResultHandler.EMPTY_LOCATION,
                                new ModelNode().set("no webserivce endpoints available"));
                        resultHandler.handleResultComplete();
                    }
                }
            });
        } else {
            resultHandler.handleResultFragment(ResultHandler.EMPTY_LOCATION, new ModelNode().set("no webservice endpoints available"));
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult();
    }
}


