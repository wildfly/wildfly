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

package org.jboss.as.web;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.modeler.Registry;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author Emanuel Muckenhuber
 */
class WebConnectorMetrics implements ModelQueryOperationHandler {

    static WebConnectorMetrics INSTANCE = new WebConnectorMetrics();

    static final String[] NO_LOCATION = new String[0];
    static final String[] ATTRIBUTES = new String[] {"bytesSent", "bytesReceived", "processingTime", "errorCount", "maxTime", "requestCount"};
    static final String BASE_NAME = "jboss.web:type=GlobalRequestProcessor,name=";

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {

        if (context.getRuntimeContext() != null) {
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final String name = address.getLastElement().getValue();
            final String attributeName = operation.require(NAME).asString();

            final ServiceController<?> controller = context.getRuntimeContext().getServiceRegistry()
                .getService(WebSubsystemServices.JBOSS_WEB_CONNECTOR.append(name));
            if (controller != null) {
                try {
                    final Connector connector = (Connector) controller.getValue();
                    final int port = connector.getPort();
                    final ModelNode result = new ModelNode();
                    result.set("" + getAttribute("http-" + port, attributeName));
                    resultHandler.handleResultFragment(new String[0], result);
                    resultHandler.handleResultComplete();
                } catch (Exception e) {
                    throw new OperationFailedException(new ModelNode().set("failed to get metrics" + e.getMessage()));
                }
            }
        } else {
            resultHandler.handleResultFragment(NO_LOCATION, new ModelNode().set("no metrics available"));
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult();
    }

    static final ObjectName createObjectName(final String name) throws MalformedObjectNameException {
        return new ObjectName(BASE_NAME + name);
    }

    static final Object getAttribute(final String name, final String attributeName) throws MalformedObjectNameException, AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException {
        final ObjectName connectorName = createObjectName(name);
        final Object value = getMBeanServer().getAttribute(connectorName, attributeName);
        return value;
    }

    static MBeanServer getMBeanServer() {
        return Registry.getRegistry(null, null).getMBeanServer();
    }

}
