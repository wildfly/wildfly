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

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.registry.OperationEntry.EntryType.PRIVATE;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * The webservices extension.
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="ema@redhat.com">Jim Ma</a>
 */
public final class WSExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "webservices";

    @Override
    public void initialize(final ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);
        subsystem.registerXMLElementWriter(WebservicesSubsystemParser.getInstance());
        // ws subsystem
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(WSSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, WSSubsystemAdd.INSTANCE, WSSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, WSSubsystemDescribe.INSTANCE, WSSubsystemProviders.SUBSYSTEM_DESCRIBE, false, PRIVATE);
        registration.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, WSSubsystemProviders.SUBSYSTEM_REMOVE, false);
        // ws endpoint configuration
        final ManagementResourceRegistration epConfigs = registration.registerSubModel(PathElement.pathElement(ENDPOINT_CONFIG), WSSubsystemProviders.ENDPOINTCONFIG_DESCRIPTION);
        epConfigs.registerOperationHandler(ADD, EndpointConfigAdd.INSTANCE, WSSubsystemProviders.ENDPOINTCONFIG_ADD_DESCRIPTION, false);
        epConfigs.registerOperationHandler(REMOVE, EndpointConfigRemove.INSTANCE, WSSubsystemProviders.ENDPOINTCONFIG_REMOVE_DESCRIPTION, false);

        final ManagementResourceRegistration deployments = subsystem.registerDeploymentModel(WSSubsystemProviders.DEPLOYMENT_DESCRIPTION);
        // ws endpoint children
        final ManagementResourceRegistration endpoints = deployments.registerSubModel(PathElement.pathElement(ENDPOINT), WSSubsystemProviders.ENDPOINT_DESCRIPTION);
        for (final String attributeName : WSEndpointMetrics.ATTRIBUTES) {
            endpoints.registerMetric(attributeName, WSEndpointMetrics.INSTANCE);
        }
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.CURRENT.getUriString(), WebservicesSubsystemParser.getInstance());
    }

}
