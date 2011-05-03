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
package org.jboss.as.webservices.dmr.management;

import javax.management.ObjectName;

import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.wsf.spi.deployment.Endpoint;
import org.jboss.wsf.spi.management.EndpointRegistry;

/**
 * To use this WS operation handler specify the following command in the CLI:
 *
 * <b>/subsystem=webservices:list-endpoints</b>
 *
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSEndpointsListOperationHandler extends WSAbstractOperationHandler {

    public static final String OPERATION_NAME = "list-endpoints";
    public static final WSEndpointsListOperationHandler INSTANCE = new WSEndpointsListOperationHandler();

    private static final String FALLBACK_MESSAGE = "No webservice endpoints available";
    private static final String ENDPOINTS_COUNT = "endpoints-count";
    private static final String NAME = "name";
    private static final String CLASS = "class";
    private static final String WSDL = "wsdl";

    private WSEndpointsListOperationHandler() {
        // forbidden instantiation
    }

    protected ModelNode getManagementOperationResultFragment(final ModelNode operation, final ServiceController<?> controller) {
        final EndpointRegistry registry = (EndpointRegistry) controller.getValue();
        final ModelNode result = new ModelNode();

        Endpoint endpoint;
        String endpointName;
        result.get(ENDPOINTS_COUNT).set(registry.getEndpoints().size());
        for (final ObjectName obj : registry.getEndpoints()) {
            endpoint = registry.getEndpoint(obj);
            endpointName = obj.toString();
            result.get(endpointName).add(NAME, endpoint.getShortName());
            result.get(endpointName).add(CLASS, endpoint.getTargetBeanName());
            result.get(endpointName).add(WSDL, endpoint.getAddress() + "?wsdl");
        }

        return result;
    }

    protected String getFallbackMessage() {
        return FALLBACK_MESSAGE;
    }

}
