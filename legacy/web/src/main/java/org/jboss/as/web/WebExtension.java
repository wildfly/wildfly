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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.DeprecatedResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.extension.AbstractLegacyExtension;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AliasEntry;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * The web extension.
 *
 * @author Emanuel Muckenhuber
 * @author Tomaz Cerar
 */
public class WebExtension extends AbstractLegacyExtension {
    public static final String SUBSYSTEM_NAME = "web";
    public static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    public static final PathElement VALVE_PATH = PathElement.pathElement(Constants.VALVE);
    protected static final PathElement CONNECTOR_PATH = PathElement.pathElement(Constants.CONNECTOR);
    protected static final PathElement SSL_PATH = PathElement.pathElement(Constants.CONFIGURATION, Constants.SSL);
    protected static final PathElement SSL_ALIAS = PathElement.pathElement(Constants.SSL, Constants.CONFIGURATION);
    protected static final PathElement HOST_PATH = PathElement.pathElement(Constants.VIRTUAL_SERVER);
    protected static final PathElement JSP_CONFIGURATION_PATH = PathElement.pathElement(Constants.CONFIGURATION, Constants.JSP_CONFIGURATION);
    protected static final PathElement STATIC_RESOURCES_PATH = PathElement.pathElement(Constants.CONFIGURATION, Constants.STATIC_RESOURCES);
    protected static final PathElement CONTAINER_PATH = PathElement.pathElement(Constants.CONFIGURATION, Constants.CONTAINER);
    protected static final PathElement ACCESS_LOG_PATH = PathElement.pathElement(Constants.CONFIGURATION, Constants.ACCESS_LOG);
    protected static final PathElement ACCESS_LOG_ALIAS = PathElement.pathElement(Constants.ACCESS_LOG, Constants.CONFIGURATION);
    protected static final PathElement REWRITE_PATH = PathElement.pathElement(Constants.REWRITE);
    protected static final PathElement SSO_PATH = PathElement.pathElement(Constants.CONFIGURATION, Constants.SSO);
    protected static final PathElement SSO_ALIAS = PathElement.pathElement(Constants.SSO, Constants.CONFIGURATION);
    protected static final PathElement DIRECTORY_PATH = PathElement.pathElement(Constants.SETTING, Constants.DIRECTORY);
    protected static final PathElement DIRECTORY_ALIAS = PathElement.pathElement(Constants.DIRECTORY, Constants.CONFIGURATION);
    protected static final PathElement REWRITECOND_PATH = PathElement.pathElement(Constants.CONDITION);
    protected static final PathElement PARAM = PathElement.pathElement(Constants.PARAM);
    private static final String RESOURCE_NAME = WebExtension.class.getPackage().getName() + ".LocalDescriptions";
    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(2, 2, 0);
    static final ModelVersion DEPRECATED_SINCE = ModelVersion.create(1,5,0);
    private static final String EXTENSION_NAME = "org.jboss.as.web";

    static final SensitiveTargetAccessConstraintDefinition WEB_CONNECTOR_CONSTRAINT = new SensitiveTargetAccessConstraintDefinition(
            new SensitivityClassification(SUBSYSTEM_NAME, "web-connector", false, false, false));

    static final SensitiveTargetAccessConstraintDefinition WEB_VALVE_CONSTRAINT = new SensitiveTargetAccessConstraintDefinition(
            new SensitivityClassification(SUBSYSTEM_NAME, "web-valve", false, false, false));

    public WebExtension() {
        super(EXTENSION_NAME, "web");
    }

    @SuppressWarnings("deprecation")
    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        final String prefix = SUBSYSTEM_NAME + (keyPrefix == null ? "" : "." + keyPrefix);
        return new DeprecatedResourceDescriptionResolver(SUBSYSTEM_NAME, prefix, RESOURCE_NAME, WebExtension.class.getClassLoader(), true, false);
    }

    @Override
    protected Set<ManagementResourceRegistration> initializeLegacyModel(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(WebDefinition.INSTANCE);
        subsystem.registerXMLElementWriter(WebSubsystemParser.getInstance());

        // connectors
        final ManagementResourceRegistration connectors = registration.registerSubModel(WebConnectorDefinition.INSTANCE);

        final ManagementResourceRegistration ssl = connectors.registerSubModel(WebSSLDefinition.INSTANCE);
        connectors.registerAlias(SSL_ALIAS, new StandardWebExtensionAliasEntry(ssl));

        //hosts
        final ManagementResourceRegistration hosts = registration.registerSubModel(WebVirtualHostDefinition.INSTANCE);

        // access-log.
        final ManagementResourceRegistration accesslog = hosts.registerSubModel(WebAccessLogDefinition.INSTANCE);
        hosts.registerAlias(ACCESS_LOG_ALIAS, new StandardWebExtensionAliasEntry(accesslog));

        // access-log.
        // the directory needs one level more
        final ManagementResourceRegistration accessLogDir = accesslog.registerSubModel(WebAccessLogDirectoryDefinition.INSTANCE);
        accesslog.registerAlias(DIRECTORY_ALIAS, new StandardWebExtensionAliasEntry(accessLogDir));

        // sso valve.
        final ManagementResourceRegistration sso = hosts.registerSubModel(WebSSODefinition.INSTANCE);
        hosts.registerAlias(SSO_ALIAS, new StandardWebExtensionAliasEntry(sso));

        // rewrite valve.
        final ManagementResourceRegistration rewrite = hosts.registerSubModel(WebReWriteDefinition.INSTANCE);

        // the condition needs one level more
        rewrite.registerSubModel(WebReWriteConditionDefinition.INSTANCE);

        // configuration=jsp
        registration.registerSubModel(WebJSPDefinition.INSTANCE);

        // configuration=resources
        registration.registerSubModel(WebStaticResources.INSTANCE);

        // configuration=container
        registration.registerSubModel(WebContainerDefinition.INSTANCE);


        // Global valve.
        registration.registerSubModel(WebValveDefinition.INSTANCE);

        //noinspection deprecation
        if (context.isRegisterTransformers()) {
            registerTransformers_1_3_0(subsystem);
            registerTransformers_1_4_0(subsystem);
            registerTransformers_2_x_0(subsystem, 0);
            registerTransformers_2_x_0(subsystem, 1);
        }
        return Collections.singleton(registration);
    }

    @Override
    protected void initializeLegacyParsers(ExtensionParsingContext context) {
        for (Namespace ns : Namespace.values()) {
            if (ns.getUriString() != null) {
                context.setSubsystemXmlMapping(SUBSYSTEM_NAME, ns.getUriString(), WebSubsystemParser.getInstance());
            }
        }
        context.setProfileParsingCompletionHandler(new DefaultJsfProfileCompletionHandler());
    }

    private void registerTransformers_1_3_0(SubsystemRegistration registration) {
        final ResourceTransformationDescriptionBuilder subsystemRoot = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        subsystemRoot.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, WebDefinition.DEFAULT_SESSION_TIMEOUT)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(30)), WebDefinition.DEFAULT_SESSION_TIMEOUT)
                .end();

        final ResourceTransformationDescriptionBuilder hostBuilder = subsystemRoot.addChildResource(HOST_PATH);
        final ResourceTransformationDescriptionBuilder ssoBuilder = hostBuilder.addChildResource(SSO_PATH);
        ssoBuilder.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, WebSSODefinition.HTTP_ONLY)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(true)), WebSSODefinition.HTTP_ONLY)
                .end();

        final ResourceTransformationDescriptionBuilder connectorBuilder = subsystemRoot.addChildResource(CONNECTOR_PATH);
        connectorBuilder.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, WebConnectorDefinition.PROXY_BINDING, WebConnectorDefinition.REDIRECT_BINDING)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, WebSSLDefinition.SSL_PROTOCOL, WebConnectorDefinition.PROXY_BINDING, WebConnectorDefinition.REDIRECT_BINDING)
                .end();

        connectorBuilder.addChildResource(SSL_PATH).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, WebSSLDefinition.CIPHER_SUITE)
                .end();

        TransformationDescription.Tools.register(subsystemRoot.build(), registration, ModelVersion.create(1, 3, 0));
    }

    private void registerTransformers_1_4_0(SubsystemRegistration registration) {
        final ResourceTransformationDescriptionBuilder subsystemRoot = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        subsystemRoot.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, WebDefinition.DEFAULT_SESSION_TIMEOUT)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(30)), WebDefinition.DEFAULT_SESSION_TIMEOUT)
                .end();

        final ResourceTransformationDescriptionBuilder hostBuilder = subsystemRoot.addChildResource(HOST_PATH);
        final ResourceTransformationDescriptionBuilder ssoBuilder = hostBuilder.addChildResource(SSO_PATH);
        ssoBuilder.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, WebSSODefinition.HTTP_ONLY)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(true)), WebSSODefinition.HTTP_ONLY)
                .end();

        TransformationDescription.Tools.register(subsystemRoot.build(), registration, ModelVersion.create(1, 4, 0));
    }

    //todo, could probably be removed as 2_x was never in EAP 6.x
    private void registerTransformers_2_x_0(SubsystemRegistration registration, int minor) {
        final ResourceTransformationDescriptionBuilder subsystemRoot = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        subsystemRoot.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, WebDefinition.DEFAULT_SESSION_TIMEOUT)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(30)), WebDefinition.DEFAULT_SESSION_TIMEOUT)
                .end();

        final ResourceTransformationDescriptionBuilder hostBuilder = subsystemRoot.addChildResource(HOST_PATH);
        final ResourceTransformationDescriptionBuilder ssoBuilder = hostBuilder.addChildResource(SSO_PATH);
        ssoBuilder.getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, WebSSODefinition.HTTP_ONLY)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode(true)), WebSSODefinition.HTTP_ONLY)
                .end();

        if (minor == 0) {
            final ResourceTransformationDescriptionBuilder connectorBuilder = subsystemRoot.addChildResource(CONNECTOR_PATH);
            connectorBuilder.getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.DEFINED, WebConnectorDefinition.PROXY_BINDING, WebConnectorDefinition.REDIRECT_BINDING)
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, WebSSLDefinition.SSL_PROTOCOL, WebConnectorDefinition.PROXY_BINDING, WebConnectorDefinition.REDIRECT_BINDING)
                    .end();

            connectorBuilder.addChildResource(SSL_PATH).getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.UNDEFINED, WebSSLDefinition.CIPHER_SUITE)
                    .end();
        }

        TransformationDescription.Tools.register(subsystemRoot.build(), registration, ModelVersion.create(2, minor, 0));
    }

    private static class StandardWebExtensionAliasEntry extends AliasEntry {
        public StandardWebExtensionAliasEntry(ManagementResourceRegistration target) {
            super(target);
        }

        @Override
        public PathAddress convertToTargetAddress(PathAddress addr, AliasContext aliasContext) {
            final PathAddress targetAddress = getTargetAddress();
            List<PathElement> list = new ArrayList<PathElement>();
            int i = 0;
            for (PathElement element : addr) {
                String key = element.getKey();
                try {
                    if (i < targetAddress.size() && (key.equals(Constants.SSL) || key.equals(Constants.SSO) || key.equals(Constants.ACCESS_LOG) || key.equals(Constants.DIRECTORY))) {
                        list.add(targetAddress.getElement(i));
                    } else {
                        list.add(element);
                    }
                    i++;
                } catch (Exception e) {
                    throw new RuntimeException("Bad " + addr + " " + targetAddress);
                }
            }
            return PathAddress.pathAddress(list);
        }
    }

}
