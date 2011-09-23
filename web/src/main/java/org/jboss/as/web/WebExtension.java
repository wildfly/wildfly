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

import org.apache.catalina.servlets.WebdavServlet;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.Locale;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.web.deployment.ServletDeploymentStats;
import org.jboss.dmr.ModelNode;
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
    private static final PathElement confPath = PathElement.pathElement(Constants.CONFIGURATION);

    private static final PathElement jspconfigurationPath = PathElement.pathElement(Constants.CONTAINER_CONFIG, Constants.JSP_CONFIGURATION);
    private static final PathElement resourcesPath = PathElement.pathElement(Constants.CONTAINER_CONFIG, Constants.STATIC_RESOURCES);
    private static final PathElement containerPath = PathElement.pathElement(Constants.CONTAINER_CONFIG, Constants.CONTAINER);

    private static final PathElement accesslogPath = PathElement.pathElement(Constants.ACCESS_LOG, "configuration");
    private static final PathElement rewritePath = PathElement.pathElement(Constants.REWRITE);

    private static final PathElement directoryPath = PathElement.pathElement(Constants.DIRECTORY, "configuration");
    private static final PathElement rewritecondPath = PathElement.pathElement(Constants.CONDITION);

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {
        log.debugf("Activating Web Extension");

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(WebSubsystemDescriptionProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, WebSubsystemAdd.INSTANCE, WebSubsystemAdd.INSTANCE, false);
        registration.registerOperationHandler(DESCRIBE, WebSubsystemDescribe.INSTANCE, WebSubsystemDescribe.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        // Attributes
        registration.registerReadWriteAttribute(Constants.NATIVE, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN, true), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(Constants.DEFAULT_VIRTUAL_SERVER, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        subsystem.registerXMLElementWriter(WebSubsystemParser.getInstance());

        // connector
        final ManagementResourceRegistration connectors = registration.registerSubModel(connectorPath, WebSubsystemDescriptionProviders.CONNECTOR);
        connectors.registerOperationHandler(ADD, WebConnectorAdd.INSTANCE, WebConnectorAdd.INSTANCE, false);
        connectors.registerOperationHandler(REMOVE, WebConnectorRemove.INSTANCE, WebConnectorRemove.INSTANCE, false);
        for(final String attributeName : WebConnectorMetrics.ATTRIBUTES) {
            connectors.registerMetric(attributeName, WebConnectorMetrics.INSTANCE);
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
        connectors.registerReadWriteAttribute(Constants.VIRTUAL_SERVER, null, new WriteAttributeHandlers. ListValidatatingHandler(new StringLengthValidator(1, false), true), Storage.CONFIGURATION);

        //hosts
        final ManagementResourceRegistration hosts = registration.registerSubModel(hostPath, WebSubsystemDescriptionProviders.VIRTUAL_SERVER);
        hosts.registerOperationHandler(ADD, WebVirtualHostAdd.INSTANCE, WebVirtualHostAdd.INSTANCE, false);
        hosts.registerOperationHandler(REMOVE, WebVirtualHostRemove.INSTANCE, WebVirtualHostRemove.INSTANCE, false);
        hosts.registerReadWriteAttribute(Constants.ALIAS, null, new WriteAttributeHandlers. ListValidatatingHandler(new StringLengthValidator(1, false), true), Storage.CONFIGURATION);
        hosts.registerReadWriteAttribute(Constants.ENABLE_WELCOME_ROOT, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN, true), Storage.CONFIGURATION);
        hosts.registerReadWriteAttribute(Constants.DEFAULT_WEB_MODULE, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);

        final ManagementResourceRegistration deployments = subsystem.registerDeploymentModel(WebSubsystemDescriptionProviders.DEPLOYMENT);
        final ManagementResourceRegistration servlets = deployments.registerSubModel(PathElement.pathElement("servlet"), WebSubsystemDescriptionProviders.SERVLET);
        // Attributes
        registration.registerReadWriteAttribute(Constants.NATIVE, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN), Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(Constants.DEFAULT_VIRTUAL_SERVER, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1), Storage.CONFIGURATION);
        final ManagementResourceRegistration accesslog = hosts.registerSubModel(accesslogPath, WebSubsystemDescriptionProviders.ACCESS_LOG);
        accesslog.registerOperationHandler(ADD, WebAccessLogAdd.INSTANCE, WebAccessLogAdd.INSTANCE, false);
        accesslog.registerOperationHandler(REMOVE, WebAccessLogRemove.INSTANCE, WebAccessLogRemove.INSTANCE, false);

        // access-log.
        // TODO the directory needs one level more
        final ManagementResourceRegistration directory = accesslog.registerSubModel(directoryPath, WebSubsystemDescriptionProviders.DIRECTORY);
        directory.registerReadWriteAttribute(Constants.RELATIVE_TO, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        directory.registerReadWriteAttribute(Constants.PATH, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);

        accesslog.registerReadWriteAttribute(Constants.PATTERN, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        accesslog.registerReadWriteAttribute(Constants.RESOLVE_HOSTS, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN, true), Storage.CONFIGURATION);
        accesslog.registerReadWriteAttribute(Constants.EXTENDED, null, new WriteAttributeHandlers.ModelTypeValidatingHandler(ModelType.BOOLEAN, true), Storage.CONFIGURATION);
        accesslog.registerReadWriteAttribute(Constants.PREFIX, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);
        accesslog.registerReadWriteAttribute(Constants.ROTATE, null, new WriteAttributeHandlers.StringLengthValidatingHandler(1, true), Storage.CONFIGURATION);

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

        // Configuration...
        final ManagementResourceRegistration conf = registration.registerSubModel(confPath, WebSubsystemDescriptionProviders.CONFIGURATION);

        final ManagementResourceRegistration jsp = registration.registerSubModel(jspconfigurationPath, WebSubsystemDescriptionProviders.JSP_CONFIGURATION);
        WebConfigurationHandlerUtils.initJSPAttributes(jsp); // Register write attributes
        final ManagementResourceRegistration resources = registration.registerSubModel(resourcesPath, WebSubsystemDescriptionProviders.STATIC_RESOURCES);
        WebConfigurationHandlerUtils.initResourcesAttribtues(resources); // Register write attributes

        final ManagementResourceRegistration container = registration.registerSubModel(containerPath, WebSubsystemDescriptionProviders.CONTAINER);
        container.registerOperationHandler("add-mime", MimeMappingAdd.INSTANCE, MimeMappingAdd.INSTANCE, false);
        container.registerOperationHandler("remove-mime", MimeMappingRemove.INSTANCE, MimeMappingRemove.INSTANCE, false);
        container.registerReadWriteAttribute(Constants.WELCOME_FILE, null, new WriteAttributeHandlers. ListValidatatingHandler(new StringLengthValidator(1, false), true), Storage.CONFIGURATION);

        DescriptionProvider NULL = new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return new ModelNode();
            }
        };
        final ManagementResourceRegistration deployments = subsystem.registerDeploymentModel(NULL);
        final ManagementResourceRegistration servlets = deployments.registerSubModel(PathElement.pathElement("servlet"), NULL);
        ServletDeploymentStats.register(servlets);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), WebSubsystemParser.getInstance());
    }

}
