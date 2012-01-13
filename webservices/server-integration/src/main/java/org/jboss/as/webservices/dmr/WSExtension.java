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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.registry.OperationEntry.EntryType.PRIVATE;
import static org.jboss.as.controller.registry.AttributeAccess.Storage.CONFIGURATION;
import static org.jboss.as.webservices.dmr.Constants.CONFIG_NAME;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.FEATURE;
import static org.jboss.as.webservices.dmr.Constants.FEATURE_NAME;
import static org.jboss.as.webservices.dmr.Constants.HANDLER;
import static org.jboss.as.webservices.dmr.Constants.HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.HANDLER_NAME;
import static org.jboss.as.webservices.dmr.Constants.HANDLER_CLASS;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.PRE_HANDLER_CHAINS;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY_NAME;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY_VALUE;
import static org.jboss.as.webservices.dmr.Constants.POST_HANDLER_CHAINS;
import static org.jboss.as.webservices.dmr.Constants.PROTOCOL_BINDING;
import static org.jboss.as.webservices.dmr.Constants.PORT_NAME_PATTERN;
import static org.jboss.as.webservices.dmr.Constants.SERVICE_NAME_PATTERN;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
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
    private static final OperationStepHandler RELOAD_REQUIRED_HANDLER = new ReloadRequiredWriteAttributeHandler();

    @Override
    public void initialize(final ExtensionContext context) {

        final boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);
        subsystem.registerXMLElementWriter(WebservicesSubsystemParser.getInstance());
        // ws subsystem
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(WSSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, WSSubsystemAdd.INSTANCE, WSSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, WSSubsystemDescribe.INSTANCE, WSSubsystemProviders.SUBSYSTEM_DESCRIBE, false, PRIVATE);
        registration.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, WSSubsystemProviders.SUBSYSTEM_REMOVE, false);
        // ws subystem attributes
        registration.registerReadWriteAttribute(WSDL_HOST, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        registration.registerReadWriteAttribute(WSDL_PORT, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        registration.registerReadWriteAttribute(WSDL_SECURE_PORT, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        registration.registerReadWriteAttribute(MODIFY_WSDL_ADDRESS, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        // ws endpoint configuration
        final ManagementResourceRegistration epConfigs = registration.registerSubModel(PathElement.pathElement(ENDPOINT_CONFIG), WSSubsystemProviders.ENDPOINTCONFIG_DESCRIPTION);
        epConfigs.registerOperationHandler(ADD, EndpointConfigAdd.INSTANCE, WSSubsystemProviders.ENDPOINTCONFIG_ADD_DESCRIPTION, false);
        epConfigs.registerOperationHandler(REMOVE, EndpointConfigRemove.INSTANCE, WSSubsystemProviders.ENDPOINTCONFIG_REMOVE_DESCRIPTION, false);
        // ws endpoint configuration attributes
        epConfigs.registerReadWriteAttribute(CONFIG_NAME, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        // ws endpoint properties
        final ManagementResourceRegistration property = epConfigs.registerSubModel(PathElement.pathElement(PROPERTY), WSSubsystemProviders.ENDPOINTCONFIG_PROPERTY_DESCRIPTION);
        property.registerReadWriteAttribute(PROPERTY_NAME, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        property.registerReadWriteAttribute(PROPERTY_VALUE, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        // ws endpoint features
        final ManagementResourceRegistration feature = epConfigs.registerSubModel(PathElement.pathElement(FEATURE), WSSubsystemProviders.ENDPOINTCONFIG_FEATURE_DESCRIPTION);
        feature.registerReadWriteAttribute(FEATURE_NAME, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        // ws endpoint pre handlers attributes
        final ManagementResourceRegistration preHandlerChain = epConfigs.registerSubModel(PathElement.pathElement(PRE_HANDLER_CHAINS, HANDLER_CHAIN), WSSubsystemProviders.ENDPOINTCONFIG_PREHANDLER_CHAIN_DESCRIPTION);
        preHandlerChain.registerReadWriteAttribute(PROTOCOL_BINDING, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        preHandlerChain.registerReadWriteAttribute(PORT_NAME_PATTERN, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        preHandlerChain.registerReadWriteAttribute(SERVICE_NAME_PATTERN, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        final ManagementResourceRegistration preHandler = preHandlerChain.registerSubModel(PathElement.pathElement(HANDLER), WSSubsystemProviders.ENDPOINTCONFIG_PREHANDLER_DESCRIPTION);
        preHandler.registerReadWriteAttribute(HANDLER_NAME, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        preHandler.registerReadWriteAttribute(HANDLER_CLASS, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        // ws endpoint post handlers attributes
        final ManagementResourceRegistration postHandlerChain = epConfigs.registerSubModel(PathElement.pathElement(POST_HANDLER_CHAINS, HANDLER_CHAIN), WSSubsystemProviders.ENDPOINTCONFIG_POSTHANDLER_CHAIN_DESCRIPTION);
        postHandlerChain.registerReadWriteAttribute(PROTOCOL_BINDING, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        postHandlerChain.registerReadWriteAttribute(PORT_NAME_PATTERN, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        postHandlerChain.registerReadWriteAttribute(SERVICE_NAME_PATTERN, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        final ManagementResourceRegistration postHandler = postHandlerChain.registerSubModel(PathElement.pathElement(HANDLER), WSSubsystemProviders.ENDPOINTCONFIG_POSTHANDLER_DESCRIPTION);
        postHandler.registerReadWriteAttribute(HANDLER_NAME, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);
        postHandler.registerReadWriteAttribute(HANDLER_CLASS, null, RELOAD_REQUIRED_HANDLER, CONFIGURATION);

        if (registerRuntimeOnly) {
            final ManagementResourceRegistration deployments = subsystem.registerDeploymentModel(WSSubsystemProviders.DEPLOYMENT_DESCRIPTION);
            // ws endpoint children
            final ManagementResourceRegistration endpoints = deployments.registerSubModel(PathElement.pathElement(ENDPOINT), WSSubsystemProviders.ENDPOINT_DESCRIPTION);
            for (final String attributeName : WSEndpointMetrics.ATTRIBUTES) {
                endpoints.registerMetric(attributeName, WSEndpointMetrics.INSTANCE);
            }
        }
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.CURRENT.getUriString(), WebservicesSubsystemParser.getInstance());
    }

}
