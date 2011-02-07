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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;

/**
 * @author Emanuel Muckenhuber
 */
public class NewWebExtension implements NewExtension {

    public static final String SUBSYSTEM_NAME = "web";
    private static final PathElement connectorPath =  PathElement.pathElement(CommonAttributes.CONNECTOR);
    private static final PathElement hostPath = PathElement.pathElement(CommonAttributes.VIRTUAL_SERVER);

    /** {@inheritDoc} */
    @Override
    public void initialize(NewExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(NewWebSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, NewWebSubsystemAdd.INSTANCE, NewWebSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, NewWebSubsystemDescribe.INSTANCE, NewWebSubsystemProviders.SUBSYSTEM_DESCRIBE, false);
        subsystem.registerXMLElementWriter(NewWebSubsystemParser.getInstance());
        // connector
        final ModelNodeRegistration connectors = registration.registerSubModel(connectorPath, NewWebSubsystemProviders.CONNECTOR);
        connectors.registerOperationHandler(ADD, NewWebConnectorAdd.INSTANCE, NewWebSubsystemProviders.CONNECTOR_ADD, false);
        connectors.registerOperationHandler(REMOVE, NewWebConnectorRemove.INSTANCE, NewWebSubsystemProviders.CONNECTOR_REMOVE, false);
        for(final String attributeName : NewWebConnectorMetrics.ATTRIBUTES) {
            connectors.registerMetric(attributeName, NewWebConnectorMetrics.INSTANCE);
        }
        //hosts
        final ModelNodeRegistration hosts = registration.registerSubModel(hostPath, NewWebSubsystemProviders.HOST);
        hosts.registerOperationHandler(ADD, NewWebVirtualHostAdd.INSTANCE, NewWebSubsystemProviders.HOST_ADD, false);
        hosts.registerOperationHandler(REMOVE, NewWebVirtualHostRemove.INSTANCE, NewWebSubsystemProviders.HOST_REMOVE, false);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), NewWebSubsystemParser.getInstance());
    }

}
