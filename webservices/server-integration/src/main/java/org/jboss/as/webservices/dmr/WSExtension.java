/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.registry.OperationEntry.EntryType.PRIVATE;
import static org.jboss.as.webservices.dmr.Constants.CLASS;
import static org.jboss.as.webservices.dmr.Constants.CLIENT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.HANDLER;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.POST_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PRE_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY;
import static org.jboss.as.webservices.dmr.Constants.PROTOCOL_BINDINGS;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * The webservices extension.
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="ema@redhat.com">Jim Ma</a>
 */
public final class WSExtension implements Extension {

    private static final PathElement endpointPath = PathElement.pathElement(ENDPOINT);
    private static final PathElement clientConfigPath = PathElement.pathElement(CLIENT_CONFIG);
    private static final PathElement endpointConfigPath = PathElement.pathElement(ENDPOINT_CONFIG);
    private static final PathElement propertyPath = PathElement.pathElement(PROPERTY);
    private static final PathElement preHandlerChainPath = PathElement.pathElement(PRE_HANDLER_CHAIN);
    private static final PathElement postHandlerChainPath = PathElement.pathElement(POST_HANDLER_CHAIN);
    private static final PathElement handlerPath = PathElement.pathElement(HANDLER);
    private static final ReloadRequiredWriteAttributeHandler reloadRequiredAttributeHandler = new ReloadRequiredWriteAttributeHandler();
    public static final String SUBSYSTEM_NAME = "webservices";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    @Override
    public void initialize(final ExtensionContext context) {
        final boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        subsystem.registerXMLElementWriter(WSSubsystemWriter.getInstance());
        // ws subsystem
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(WSSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, WSSubsystemAdd.INSTANCE, WSSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, WSSubsystemDescribe.INSTANCE, WSSubsystemProviders.SUBSYSTEM_DESCRIBE, false, PRIVATE);
        registration.registerOperationHandler(REMOVE, WSSubsystemRemove.INSTANCE, WSSubsystemProviders.SUBSYSTEM_REMOVE, false);
        registration.registerReadWriteAttribute(WSDL_HOST, null, new WSSubsystemAttributeChangeHandler(new StringLengthValidator(1, Integer.MAX_VALUE, true, true)), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(WSDL_PORT, null, new WSSubsystemAttributeChangeHandler(new IntRangeValidator(1, Integer.MAX_VALUE, true, true)), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(WSDL_SECURE_PORT, null, new WSSubsystemAttributeChangeHandler(new IntRangeValidator(1, Integer.MAX_VALUE, true, true)), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(MODIFY_WSDL_ADDRESS, null, new WSSubsystemAttributeChangeHandler(new ModelTypeValidator(ModelType.BOOLEAN, true)), Storage.CONFIGURATION);

        // endpoint configurations
        final ManagementResourceRegistration endpointConfig = registration.registerSubModel(endpointConfigPath, WSSubsystemProviders.ENDPOINT_CONFIG_DESCRIPTION);
        endpointConfig.registerOperationHandler(ADD, EndpointConfigAdd.INSTANCE, WSSubsystemProviders.ENDPOINT_CONFIG_ADD_DESCRIPTION, false);
        endpointConfig.registerOperationHandler(REMOVE, EndpointConfigRemove.INSTANCE, WSSubsystemProviders.ENDPOINT_CONFIG_REMOVE_DESCRIPTION, false);
        registerProperty(endpointConfig, false);
        registerPreHandlerChain(endpointConfig, false);
        registerPostHandlerChain(endpointConfig, false);

        // client configurations
        final ManagementResourceRegistration clientConfig = registration.registerSubModel(clientConfigPath, WSSubsystemProviders.CLIENT_CONFIG_DESCRIPTION);
        clientConfig.registerOperationHandler(ADD, ClientConfigAdd.INSTANCE, WSSubsystemProviders.CLIENT_CONFIG_ADD_DESCRIPTION, false);
        clientConfig.registerOperationHandler(REMOVE, ClientConfigRemove.INSTANCE, WSSubsystemProviders.CLIENT_CONFIG_REMOVE_DESCRIPTION, false);
        registerProperty(clientConfig, true);
        registerPreHandlerChain(clientConfig, true);
        registerPostHandlerChain(clientConfig, true);

        if (registerRuntimeOnly) {
            final ManagementResourceRegistration deployments = subsystem.registerDeploymentModel(WSSubsystemProviders.DEPLOYMENT_DESCRIPTION);
            // ws endpoints
            final ManagementResourceRegistration endpoints = deployments.registerSubModel(endpointPath, WSSubsystemProviders.ENDPOINT_DESCRIPTION);
            for (final String attributeName : WSEndpointMetrics.ATTRIBUTES) {
                endpoints.registerMetric(attributeName, WSEndpointMetrics.INSTANCE);
            }
        }
    }

    private void registerProperty(ManagementResourceRegistration config, boolean client) {
        ManagementResourceRegistration property = config.registerSubModel(propertyPath,
                client ? WSSubsystemProviders.CLIENT_PROPERTY_DESCRIPTION : WSSubsystemProviders.ENDPOINT_PROPERTY_DESCRIPTION);
        property.registerOperationHandler(ADD, PropertyAdd.INSTANCE,
                client ? WSSubsystemProviders.CLIENT_PROPERTY_ADD_DESCRIPTION : WSSubsystemProviders.ENDPOINT_PROPERTY_ADD_DESCRIPTION, false);
        property.registerOperationHandler(REMOVE, PropertyRemove.INSTANCE,
                client ? WSSubsystemProviders.CLIENT_PROPERTY_REMOVE_DESCRIPTION : WSSubsystemProviders.ENDPOINT_PROPERTY_REMOVE_DESCRIPTION, false);
        property.registerReadWriteAttribute(VALUE, null, reloadRequiredAttributeHandler, Storage.CONFIGURATION);
    }

    private void registerPreHandlerChain(ManagementResourceRegistration config, boolean client) {
        ManagementResourceRegistration handlerChain = config.registerSubModel(preHandlerChainPath,
                client ? WSSubsystemProviders.CLIENT_PRE_HANDLER_CHAIN_DESCRIPTION : WSSubsystemProviders.ENDPOINT_PRE_HANDLER_CHAIN_DESCRIPTION);
        handlerChain.registerOperationHandler(ADD, HandlerChainAdd.INSTANCE,
                client ? WSSubsystemProviders.CLIENT_PRE_HANDLER_CHAIN_ADD_DESCRIPTION : WSSubsystemProviders.ENDPOINT_PRE_HANDLER_CHAIN_ADD_DESCRIPTION, false);
        handlerChain.registerOperationHandler(REMOVE, HandlerChainRemove.INSTANCE,
                client ? WSSubsystemProviders.CLIENT_PRE_HANDLER_CHAIN_REMOVE_DESCRIPTION : WSSubsystemProviders.ENDPOINT_PRE_HANDLER_CHAIN_REMOVE_DESCRIPTION, false);
        handlerChain.registerReadWriteAttribute(PROTOCOL_BINDINGS, null, reloadRequiredAttributeHandler, Storage.CONFIGURATION);
        registerHandler(handlerChain, client);
    }

    private void registerPostHandlerChain(ManagementResourceRegistration config, boolean client) {
        ManagementResourceRegistration handlerChain = config.registerSubModel(postHandlerChainPath,
                client ? WSSubsystemProviders.CLIENT_POST_HANDLER_CHAIN_DESCRIPTION : WSSubsystemProviders.ENDPOINT_POST_HANDLER_CHAIN_DESCRIPTION);
        handlerChain.registerOperationHandler(ADD, HandlerChainAdd.INSTANCE,
                client ? WSSubsystemProviders.CLIENT_POST_HANDLER_CHAIN_ADD_DESCRIPTION : WSSubsystemProviders.ENDPOINT_POST_HANDLER_CHAIN_ADD_DESCRIPTION, false);
        handlerChain.registerOperationHandler(REMOVE, HandlerChainRemove.INSTANCE,
                client ? WSSubsystemProviders.CLIENT_POST_HANDLER_CHAIN_REMOVE_DESCRIPTION : WSSubsystemProviders.ENDPOINT_POST_HANDLER_CHAIN_REMOVE_DESCRIPTION, false);
        handlerChain.registerReadWriteAttribute(PROTOCOL_BINDINGS, null, reloadRequiredAttributeHandler, Storage.CONFIGURATION);
        registerHandler(handlerChain, client);
    }

    private void registerHandler(ManagementResourceRegistration handlerChain, boolean client) {
        ManagementResourceRegistration postHandler = handlerChain.registerSubModel(handlerPath, WSSubsystemProviders.HANDLER_DESCRIPTION);
        postHandler.registerOperationHandler(ADD, HandlerAdd.INSTANCE, WSSubsystemProviders.HANDLER_ADD_DESCRIPTION, false);
        postHandler.registerOperationHandler(REMOVE, HandlerRemove.INSTANCE, WSSubsystemProviders.HANDLER_REMOVE_DESCRIPTION, false);
        postHandler.registerReadWriteAttribute(CLASS, null, reloadRequiredAttributeHandler, Storage.CONFIGURATION);
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.WEBSERVICES_1_0.getUriString(), WSSubsystemLegacyReader.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.WEBSERVICES_1_1.getUriString(), WSSubsystemReader.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.WEBSERVICES_1_2.getUriString(), WSSubsystemReader.getInstance());
    }

}
