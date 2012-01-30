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
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.web.deployment.ServletDeploymentStats;
import org.jboss.dmr.ModelType;
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
    private static final PathElement sslPath = PathElement.pathElement(Constants.SSL, Constants.CONFIGURATION);

    private static final PathElement jspconfigurationPath = PathElement.pathElement(Constants.CONTAINER_CONFIG, Constants.JSP_CONFIGURATION);
    private static final PathElement resourcesPath = PathElement.pathElement(Constants.CONTAINER_CONFIG, Constants.STATIC_RESOURCES);
    private static final PathElement containerPath = PathElement.pathElement(Constants.CONTAINER_CONFIG, Constants.CONTAINER);

    private static final PathElement accesslogPath = PathElement.pathElement(Constants.ACCESS_LOG, Constants.CONFIGURATION);
    private static final PathElement rewritePath = PathElement.pathElement(Constants.REWRITE);
    private static final PathElement ssoPath = PathElement.pathElement(Constants.SSO, Constants.CONFIGURATION);

    private static final PathElement directoryPath = PathElement.pathElement(Constants.DIRECTORY, Constants.CONFIGURATION);
    private static final PathElement rewritecondPath = PathElement.pathElement(Constants.CONDITION);


    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {

        final boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(WebSubsystemDescriptionProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, WebSubsystemAdd.INSTANCE, WebSubsystemAdd.INSTANCE, false);
        registration.registerOperationHandler(DESCRIBE, WebSubsystemDescribe.INSTANCE, WebSubsystemDescribe.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        registration.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, WebSubsystemDescriptionProviders.SUBSYSTEM_REMOVE, false);
        subsystem.registerXMLElementWriter(WebSubsystemParser.getInstance());

        // connectors
        final ManagementResourceRegistration connectors = registration.registerSubModel(connectorPath, WebSubsystemDescriptionProviders.CONNECTOR);
        connectors.registerOperationHandler(ADD, WebConnectorAdd.INSTANCE, WebConnectorAdd.INSTANCE, false);
        connectors.registerOperationHandler(REMOVE, WebConnectorRemove.INSTANCE, WebConnectorRemove.INSTANCE, false);
        if (registerRuntimeOnly) {
            for (final String attributeName : WebConnectorMetrics.ATTRIBUTES) {
                connectors.registerMetric(attributeName, WebConnectorMetrics.INSTANCE);
            }
        }
        connectors.registerReadWriteAttribute(Constants.PROTOCOL, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        connectors.registerReadWriteAttribute(Constants.SCHEME, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        connectors.registerReadWriteAttribute(Constants.SOCKET_BINDING, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        connectors.registerReadWriteAttribute(Constants.ENABLE_LOOKUPS, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN, true), Storage.CONFIGURATION);
        connectors.registerReadWriteAttribute(Constants.PROXY_NAME, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        connectors.registerReadWriteAttribute(Constants.PROXY_PORT, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1, true), Storage.CONFIGURATION);
        connectors.registerReadWriteAttribute(Constants.MAX_POST_SIZE, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1024, true), Storage.CONFIGURATION);
        connectors.registerReadWriteAttribute(Constants.MAX_SAVE_POST_SIZE, null, new WriteAttributeHandlers.IntRangeValidatingHandler(0, true), Storage.CONFIGURATION);
        connectors.registerReadWriteAttribute(Constants.ENABLED, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN, true), Storage.CONFIGURATION);
        connectors.registerReadWriteAttribute(Constants.EXECUTOR, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        connectors.registerReadWriteAttribute(Constants.MAX_CONNECTIONS, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        connectors.registerReadWriteAttribute(Constants.VIRTUAL_SERVER, null, new WriteAttributeHandlers.ListValidatatingHandler(new StringLengthValidator(1, false), true), Storage.CONFIGURATION);
        // connectors SSL part.
        final ManagementResourceRegistration ssl = connectors.registerSubModel(sslPath, WebSubsystemDescriptionProviders.SSL);
        ssl.registerOperationHandler(ADD, WebSSLAdd.INSTANCE, WebSSLAdd.INSTANCE, true);
        ssl.registerOperationHandler(REMOVE, WebSSLRemove.INSTANCE, WebSSLRemove.INSTANCE, true);

        ssl.registerReadWriteAttribute(Constants.KEY_ALIAS, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        ssl.registerReadWriteAttribute(Constants.PASSWORD, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        ssl.registerReadWriteAttribute(Constants.CERTIFICATE_KEY_FILE, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        ssl.registerReadWriteAttribute(Constants.CIPHER_SUITE, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        ssl.registerReadWriteAttribute(Constants.PROTOCOL, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        ssl.registerReadWriteAttribute(Constants.VERIFY_CLIENT, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        ssl.registerReadWriteAttribute(Constants.VERIFY_DEPTH, null, new WriteAttributeHandlers.IntRangeValidatingHandler(1, true), Storage.CONFIGURATION);
        ssl.registerReadWriteAttribute(Constants.CERTIFICATE_FILE, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        ssl.registerReadWriteAttribute(Constants.CA_CERTIFICATE_FILE, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        ssl.registerReadWriteAttribute(Constants.CA_REVOCATION_URL, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        ssl.registerReadWriteAttribute(Constants.SESSION_CACHE_SIZE, null, new WriteAttributeHandlers.IntRangeValidatingHandler(0, true), Storage.CONFIGURATION);
        ssl.registerReadWriteAttribute(Constants.SESSION_TIMEOUT, null, new WriteAttributeHandlers.IntRangeValidatingHandler(0, true), Storage.CONFIGURATION);

        //hosts
        final ManagementResourceRegistration hosts = registration.registerSubModel(hostPath, WebSubsystemDescriptionProviders.VIRTUAL_SERVER);
        hosts.registerOperationHandler(ADD, WebVirtualHostAdd.INSTANCE, WebVirtualHostAdd.INSTANCE, false);
        hosts.registerOperationHandler(REMOVE, WebVirtualHostRemove.INSTANCE, WebVirtualHostRemove.INSTANCE, false);
        hosts.registerReadWriteAttribute(Constants.ALIAS, null, new WriteAttributeHandlers. ListValidatatingHandler(new StringLengthValidator(1, false), true), Storage.CONFIGURATION);
        // They excluded each other...
        hosts.registerReadWriteAttribute(Constants.ENABLE_WELCOME_ROOT, null, WriteEnableWelcomeRoot.INSTANCE, Storage.CONFIGURATION);
        hosts.registerReadWriteAttribute(Constants.DEFAULT_WEB_MODULE, null, WriteDefaultWebModule.INSTANCE, Storage.CONFIGURATION);

        // access-log.
        final ManagementResourceRegistration accesslog = hosts.registerSubModel(accesslogPath, WebSubsystemDescriptionProviders.ACCESS_LOG);
        accesslog.registerOperationHandler(ADD, WebAccessLogAdd.INSTANCE, WebAccessLogAdd.INSTANCE, true);
        accesslog.registerOperationHandler(REMOVE, WebAccessLogRemove.INSTANCE, WebAccessLogRemove.INSTANCE, true);

        // access-log.
        // the directory needs one level more
        final ManagementResourceRegistration directory = accesslog.registerSubModel(directoryPath, WebSubsystemDescriptionProviders.DIRECTORY);
        directory.registerReadWriteAttribute(Constants.RELATIVE_TO, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        directory.registerReadWriteAttribute(Constants.PATH, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);

        accesslog.registerReadWriteAttribute(Constants.PATTERN, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        accesslog.registerReadWriteAttribute(Constants.RESOLVE_HOSTS, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN, true), Storage.CONFIGURATION);
        accesslog.registerReadWriteAttribute(Constants.EXTENDED, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN, true), Storage.CONFIGURATION);
        accesslog.registerReadWriteAttribute(Constants.PREFIX, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        accesslog.registerReadWriteAttribute(Constants.ROTATE, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);

        // sso valve.
        final ManagementResourceRegistration sso = hosts.registerSubModel(ssoPath, WebSubsystemDescriptionProviders.SSO);
        sso.registerOperationHandler(ADD, WebSSOAdd.INSTANCE, WebSSOAdd.INSTANCE, false);
        sso.registerOperationHandler(REMOVE, WebSSORemove.INSTANCE, WebSSORemove.INSTANCE, false);

        sso.registerReadWriteAttribute(Constants.CACHE_CONTAINER, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        sso.registerReadWriteAttribute(Constants.DOMAIN, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        sso.registerReadWriteAttribute(Constants.REAUTHENTICATE, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN, true), Storage.CONFIGURATION);

        // rewrite valve.
        final ManagementResourceRegistration rewrite = hosts.registerSubModel(rewritePath, WebSubsystemDescriptionProviders.REWRITE);
        rewrite.registerOperationHandler(ADD, WebReWriteAdd.INSTANCE, WebReWriteAdd.INSTANCE, false);
        rewrite.registerOperationHandler(REMOVE, WebReWriteRemove.INSTANCE, WebReWriteRemove.INSTANCE, false);

        // the condition needs one level more
        final ManagementResourceRegistration rewritecondition = rewrite.registerSubModel(rewritecondPath, WebSubsystemDescriptionProviders.REWRITECOND);
        rewritecondition.registerOperationHandler(ADD, WebReWriteConditionAdd.INSTANCE, WebReWriteConditionAdd.INSTANCE, false);
        rewritecondition.registerOperationHandler(REMOVE, WebReWriteConditionRemove.INSTANCE, WebReWriteConditionRemove.INSTANCE, false);
        rewritecondition.registerReadWriteAttribute(Constants.TEST, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        rewritecondition.registerReadWriteAttribute(Constants.PATTERN, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        rewritecondition.registerReadWriteAttribute(Constants.FLAGS, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);

        rewrite.registerReadWriteAttribute(Constants.CONDITION, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        rewrite.registerReadWriteAttribute(Constants.PATTERN, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        rewrite.registerReadWriteAttribute(Constants.SUBSTITUTION, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        rewrite.registerReadWriteAttribute(Constants.FLAGS, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);

        // configuration=jsp
        final ManagementResourceRegistration jsp = registration.registerSubModel(jspconfigurationPath, WebSubsystemDescriptionProviders.JSP_CONFIGURATION);
        WebConfigurationHandlerUtils.initJSPAttributes(jsp); // Register write attributes
        // configuration=resources
        final ManagementResourceRegistration resources = registration.registerSubModel(resourcesPath, WebSubsystemDescriptionProviders.STATIC_RESOURCES);
        WebConfigurationHandlerUtils.initResourcesAttribtues(resources); // Register write attributes
        // configuration=container
        final ManagementResourceRegistration container = registration.registerSubModel(containerPath, WebSubsystemDescriptionProviders.CONTAINER);
        container.registerOperationHandler("add-mime", MimeMappingAdd.INSTANCE, MimeMappingAdd.INSTANCE, false);
        container.registerOperationHandler("remove-mime", MimeMappingRemove.INSTANCE, MimeMappingRemove.INSTANCE, false);
        container.registerReadWriteAttribute(Constants.WELCOME_FILE, null, new WriteAttributeHandlers. ListValidatatingHandler(new StringLengthValidator(1, false), true), Storage.CONFIGURATION);

        if (registerRuntimeOnly) {
            final ManagementResourceRegistration deployments = subsystem.registerDeploymentModel(WebSubsystemDescriptionProviders.DEPLOYMENT);
            final ManagementResourceRegistration servlets = deployments.registerSubModel(PathElement.pathElement("servlet"), WebSubsystemDescriptionProviders.SERVLET);
            ServletDeploymentStats.register(servlets);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.WEB_1_1.getUriString(), WebSubsystemParser.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.WEB_1_0.getUriString(), WebSubsystemParser.getInstance());
    }

}
