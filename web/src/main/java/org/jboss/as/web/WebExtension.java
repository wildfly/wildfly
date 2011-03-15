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
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.logging.Logger;

/**
 * The web extension.
 *
 * @author Emanuel Muckenhuber
 */
public class WebExtension implements Extension {

    private static final Logger log = Logger.getLogger("org.jboss.as.web");

    public static final String SUBSYSTEM_NAME = "web";
    private static final PathElement connectorPath =  PathElement.pathElement(Constants.CONNECTOR);
    private static final PathElement hostPath = PathElement.pathElement(Constants.VIRTUAL_SERVER);

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {
        log.debugf("Activating Web Extension");

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(WebSubsystemDescriptionProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, WebSubsystemAdd.INSTANCE, WebSubsystemAdd.INSTANCE, false);
        registration.registerOperationHandler(DESCRIBE, WebSubsystemDescribe.INSTANCE, WebSubsystemDescribe.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        subsystem.registerXMLElementWriter(WebSubsystemParser.getInstance());
        // connector
        final ModelNodeRegistration connectors = registration.registerSubModel(connectorPath, WebSubsystemDescriptionProviders.CONNECTOR);
        connectors.registerOperationHandler(ADD, WebConnectorAdd.INSTANCE, WebConnectorAdd.INSTANCE, false);
        connectors.registerOperationHandler(REMOVE, WebConnectorRemove.INSTANCE, WebConnectorRemove.INSTANCE, false);
        for(final String attributeName : WebConnectorMetrics.ATTRIBUTES) {
            connectors.registerMetric(attributeName, WebConnectorMetrics.INSTANCE);
        }
        //hosts
        final ModelNodeRegistration hosts = registration.registerSubModel(hostPath, WebSubsystemDescriptionProviders.VIRTUAL_SERVER);
        hosts.registerOperationHandler(ADD, WebVirtualHostAdd.INSTANCE, WebVirtualHostAdd.INSTANCE, false);
        hosts.registerOperationHandler(REMOVE, WebVirtualHostRemove.INSTANCE, WebVirtualHostRemove.INSTANCE, false);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), WebSubsystemParser.getInstance());
    }

}
