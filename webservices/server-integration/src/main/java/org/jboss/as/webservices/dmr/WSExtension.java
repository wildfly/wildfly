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
import static org.jboss.as.webservices.dmr.Constants.FEATURE;
import static org.jboss.as.webservices.dmr.Constants.MODIFY_WSDL_ADDRESS;
import static org.jboss.as.webservices.dmr.Constants.PROPERTY;
import static org.jboss.as.webservices.dmr.Constants.WSDL_HOST;
import static org.jboss.as.webservices.dmr.Constants.WSDL_PORT;
import static org.jboss.as.webservices.dmr.Constants.WSDL_SECURE_PORT;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
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
    private static final PathElement featurePath = PathElement.pathElement(FEATURE);
    private static final PathElement propertyPath = PathElement.pathElement(PROPERTY);

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
        registration.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, WSSubsystemProviders.SUBSYSTEM_REMOVE, false);
        registration.registerReadWriteAttribute(WSDL_HOST, null, new WSSubsystemAttributeChangeHandler(new StringLengthValidator(1, Integer.MAX_VALUE, true, true)), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(WSDL_PORT, null, new WSSubsystemAttributeChangeHandler(new IntRangeValidator(1, Integer.MAX_VALUE, true, true)), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(WSDL_SECURE_PORT, null, new WSSubsystemAttributeChangeHandler(new IntRangeValidator(1, Integer.MAX_VALUE, true, true)), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(MODIFY_WSDL_ADDRESS, null, new WSSubsystemAttributeChangeHandler(new ModelTypeValidator(ModelType.BOOLEAN, true)), Storage.CONFIGURATION);
        // ws endpoint configuration
        final ManagementResourceRegistration endpointConfig = registration.registerSubModel(endpointConfigPath, WSSubsystemProviders.ENDPOINT_CONFIG_DESCRIPTION);
        endpointConfig.registerOperationHandler(ADD, EndpointConfigAdd.INSTANCE, WSSubsystemProviders.ENDPOINT_CONFIG_ADD_DESCRIPTION, false);
        endpointConfig.registerOperationHandler(REMOVE, EndpointConfigRemove.INSTANCE, WSSubsystemProviders.ENDPOINT_CONFIG_REMOVE_DESCRIPTION, false);
        // ws endpoint configuration features
        final ManagementResourceRegistration endpointConfigFeature = endpointConfig.registerSubModel(featurePath, WSSubsystemProviders.ENDPOINT_CONFIG_FEATURE_DESCRIPTION);
        endpointConfigFeature.registerOperationHandler(ADD, EndpointConfigFeatureAdd.INSTANCE, WSSubsystemProviders.ENDPOINT_CONFIG_FEATURE_ADD_DESCRIPTION, false);
        endpointConfigFeature.registerOperationHandler(REMOVE, EndpointConfigFeatureRemove.INSTANCE, WSSubsystemProviders.ENDPOINT_CONFIG_FEATURE_REMOVE_DESCRIPTION, false);
        // ws endpoint configuration properties
        final ManagementResourceRegistration endpointConfigProperty = endpointConfig.registerSubModel(propertyPath, WSSubsystemProviders.ENDPOINT_CONFIG_PROPERTY_DESCRIPTION);
        endpointConfigProperty.registerOperationHandler(ADD, EndpointConfigPropertyAdd.INSTANCE, WSSubsystemProviders.ENDPOINT_CONFIG_PROPERTY_ADD_DESCRIPTION, false);
        endpointConfigProperty.registerOperationHandler(REMOVE, EndpointConfigPropertyRemove.INSTANCE, WSSubsystemProviders.ENDPOINT_CONFIG_PROPERTY_REMOVE_DESCRIPTION, false);
        endpointConfigProperty.registerReadWriteAttribute(VALUE, null, new EndpointConfigPropertyValueChangeHandler(new StringLengthValidator(1, Integer.MAX_VALUE, true, true)), Storage.CONFIGURATION);

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
