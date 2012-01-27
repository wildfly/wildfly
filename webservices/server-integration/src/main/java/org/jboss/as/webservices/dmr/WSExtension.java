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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.registry.OperationEntry.EntryType.PRIVATE;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONFIG;
import static org.jboss.as.webservices.dmr.Constants.HANDLER;
import static org.jboss.as.webservices.dmr.Constants.HANDLER_CLASS;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.PORT_NAME_PATTERN;
import static org.jboss.as.webservices.dmr.Constants.POST_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PRE_HANDLER_CHAIN;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY;
import static org.jboss.as.webservices.dmr.Constants.PROTOCOL_BINDINGS;
import static org.jboss.as.webservices.dmr.Constants.SERVICE_NAME_PATTERN;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
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
    private static final PathElement endpointConfigPath = PathElement.pathElement(ENDPOINT_CONFIG);
    private static final PathElement propertyPath = PathElement.pathElement(PROPERTY);
    private static final PathElement preHandlerChainPath = PathElement.pathElement(PRE_HANDLER_CHAIN);
    private static final PathElement postHandlerChainPath = PathElement.pathElement(POST_HANDLER_CHAIN);
    private static final PathElement handlerPath = PathElement.pathElement(HANDLER);

    public static final String SUBSYSTEM_NAME = "webservices";

    @Override
    public void initialize(final ExtensionContext context) {
        final boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);
        subsystem.registerXMLElementWriter(WebservicesSubsystemParser.getInstance());
        // ws subsystem
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(WSSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, WSSubsystemAdd.INSTANCE, WSSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, WSSubsystemDescribe.INSTANCE, WSSubsystemProviders.SUBSYSTEM_DESCRIBE, false, PRIVATE);
        registration.registerOperationHandler(REMOVE, WSSubsystemRemove.INSTANCE, WSSubsystemProviders.SUBSYSTEM_REMOVE, false);
        registration.registerReadWriteAttribute(WSDL_HOST, null, new WSSubsystemAttributeChangeHandler(new StringLengthValidator(1, Integer.MAX_VALUE, true, true)), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(WSDL_PORT, null, new WSSubsystemAttributeChangeHandler(new IntRangeValidator(1, Integer.MAX_VALUE, true, true)), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(WSDL_SECURE_PORT, null, new WSSubsystemAttributeChangeHandler(new IntRangeValidator(1, Integer.MAX_VALUE, true, true)), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(MODIFY_WSDL_ADDRESS, null, new WSSubsystemAttributeChangeHandler(new ModelTypeValidator(ModelType.BOOLEAN, true)), Storage.CONFIGURATION);
        // endpoint configuration
        final ManagementResourceRegistration endpointConfig = registration.registerSubModel(endpointConfigPath, WSSubsystemProviders.ENDPOINT_CONFIG_DESCRIPTION);
        endpointConfig.registerOperationHandler(ADD, EndpointConfigAdd.INSTANCE, WSSubsystemProviders.ENDPOINT_CONFIG_ADD_DESCRIPTION, false);
        endpointConfig.registerOperationHandler(REMOVE, EndpointConfigRemove.INSTANCE, WSSubsystemProviders.ENDPOINT_CONFIG_REMOVE_DESCRIPTION, false);
        // properties
        final ManagementResourceRegistration property = endpointConfig.registerSubModel(propertyPath, WSSubsystemProviders.PROPERTY_DESCRIPTION);
        property.registerOperationHandler(ADD, PropertyAdd.INSTANCE, WSSubsystemProviders.PROPERTY_ADD_DESCRIPTION, false);
        property.registerOperationHandler(REMOVE, PropertyRemove.INSTANCE, WSSubsystemProviders.PROPERTY_REMOVE_DESCRIPTION, false);
        property.registerReadOnlyAttribute(VALUE, null, Storage.CONFIGURATION);
        // pre handler chains
        final ManagementResourceRegistration preHandlerChain = endpointConfig.registerSubModel(preHandlerChainPath, WSSubsystemProviders.PRE_HANDLER_CHAIN_DESCRIPTION);
        preHandlerChain.registerOperationHandler(ADD, HandlerChainAdd.INSTANCE, WSSubsystemProviders.PRE_HANDLER_CHAIN_ADD_DESCRIPTION, false);
        preHandlerChain.registerOperationHandler(REMOVE, HandlerChainRemove.INSTANCE, WSSubsystemProviders.PRE_HANDLER_CHAIN_REMOVE_DESCRIPTION, false);
        preHandlerChain.registerReadOnlyAttribute(PROTOCOL_BINDINGS, null, Storage.CONFIGURATION);
        preHandlerChain.registerReadOnlyAttribute(PORT_NAME_PATTERN, null, Storage.CONFIGURATION);
        preHandlerChain.registerReadOnlyAttribute(SERVICE_NAME_PATTERN, null, Storage.CONFIGURATION);
        // pre handlers
        final ManagementResourceRegistration preHandler = preHandlerChain.registerSubModel(handlerPath, WSSubsystemProviders.HANDLER_DESCRIPTION);
        preHandler.registerOperationHandler(ADD, HandlerAdd.INSTANCE, WSSubsystemProviders.HANDLER_ADD_DESCRIPTION, false);
        preHandler.registerOperationHandler(REMOVE, HandlerRemove.INSTANCE, WSSubsystemProviders.HANDLER_REMOVE_DESCRIPTION, false);
        preHandler.registerReadOnlyAttribute(HANDLER_CLASS, null, Storage.CONFIGURATION);
        // post handler chains
        final ManagementResourceRegistration postHandlerChain = endpointConfig.registerSubModel(postHandlerChainPath, WSSubsystemProviders.POST_HANDLER_CHAIN_DESCRIPTION);
        postHandlerChain.registerOperationHandler(ADD, HandlerChainAdd.INSTANCE, WSSubsystemProviders.POST_HANDLER_CHAIN_ADD_DESCRIPTION, false);
        postHandlerChain.registerOperationHandler(REMOVE, HandlerChainRemove.INSTANCE, WSSubsystemProviders.POST_HANDLER_CHAIN_REMOVE_DESCRIPTION, false);
        postHandlerChain.registerReadOnlyAttribute(PROTOCOL_BINDINGS, null, Storage.CONFIGURATION);
        postHandlerChain.registerReadOnlyAttribute(PORT_NAME_PATTERN, null, Storage.CONFIGURATION);
        postHandlerChain.registerReadOnlyAttribute(SERVICE_NAME_PATTERN, null, Storage.CONFIGURATION);
        // post handlers
        final ManagementResourceRegistration postHandler = postHandlerChain.registerSubModel(handlerPath, WSSubsystemProviders.HANDLER_DESCRIPTION);
        postHandler.registerOperationHandler(ADD, HandlerAdd.INSTANCE, WSSubsystemProviders.HANDLER_ADD_DESCRIPTION, false);
        postHandler.registerOperationHandler(REMOVE, HandlerRemove.INSTANCE, WSSubsystemProviders.HANDLER_REMOVE_DESCRIPTION, false);
        postHandler.registerReadOnlyAttribute(HANDLER_CLASS, null, Storage.CONFIGURATION);

        if (registerRuntimeOnly) {
            final ManagementResourceRegistration deployments = subsystem.registerDeploymentModel(WSSubsystemProviders.DEPLOYMENT_DESCRIPTION);
            // ws endpoint children
            final ManagementResourceRegistration endpoints = deployments.registerSubModel(endpointPath, WSSubsystemProviders.ENDPOINT_DESCRIPTION);
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
