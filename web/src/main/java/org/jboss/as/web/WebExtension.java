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

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.logging.Logger;

/**
 * The web extension.
 *
 * @author Emanuel Muckenhuber
 */
public class WebExtension implements Extension {

    private static final Logger log = Logger.getLogger("org.jboss.as.web");

    public static final String SUBSYSTEM_NAME = "web";
    private static final PathElement connectorPath =  PathElement.pathElement(CommonAttributes.CONNECTOR);
    private static final PathElement hostPath = PathElement.pathElement(CommonAttributes.VIRTUAL_SERVER);

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {
        log.debugf("Activating Web Extension");

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(WebSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, WebSubsystemAdd.INSTANCE, WebSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, WebSubsystemDescribe.INSTANCE, WebSubsystemProviders.SUBSYSTEM_DESCRIBE, false);
        subsystem.registerXMLElementWriter(WebSubsystemParser.getInstance());
        // connector
        final ModelNodeRegistration connectors = registration.registerSubModel(connectorPath, WebSubsystemProviders.CONNECTOR);
        connectors.registerOperationHandler(ADD, WebConnectorAdd.INSTANCE, WebSubsystemProviders.CONNECTOR_ADD, false);
        connectors.registerOperationHandler(REMOVE, WebConnectorRemove.INSTANCE, WebSubsystemProviders.CONNECTOR_REMOVE, false);
        for(final String attributeName : WebConnectorMetrics.ATTRIBUTES) {
            connectors.registerMetric(attributeName, WebConnectorMetrics.INSTANCE);
        }
        //hosts
        final ModelNodeRegistration hosts = registration.registerSubModel(hostPath, WebSubsystemProviders.HOST);
        hosts.registerOperationHandler(ADD, WebVirtualHostAdd.INSTANCE, WebSubsystemProviders.HOST_ADD, false);
        hosts.registerOperationHandler(REMOVE, WebVirtualHostRemove.INSTANCE, WebSubsystemProviders.HOST_REMOVE, false);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), WebSubsystemParser.getInstance());
    }

}
