/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.ValueExpression;
import org.jboss.security.SecurityConstants;
import org.wildfly.extension.undertow.filters.FilterDefinitions;
import org.wildfly.extension.undertow.handlers.HandlerDefinitions;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
class UndertowRootDefinition extends PersistentResourceDefinition {
    protected static final SimpleAttributeDefinition DEFAULT_VIRTUAL_HOST =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_VIRTUAL_HOST, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode("default-host"))
                    .build();
    protected static final SimpleAttributeDefinition DEFAULT_SERVLET_CONTAINER =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_SERVLET_CONTAINER, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode("default"))
                    .build();
    protected static final SimpleAttributeDefinition DEFAULT_SERVER =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_SERVER, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode("default-server"))
                    .build();
    protected static final SimpleAttributeDefinition INSTANCE_ID =
            new SimpleAttributeDefinitionBuilder(Constants.INSTANCE_ID, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode().set(new ValueExpression("${jboss.node.name}")))
                    .build();
    protected static final SimpleAttributeDefinition STATISTICS_ENABLED =
                new SimpleAttributeDefinitionBuilder(Constants.STATISTICS_ENABLED, ModelType.BOOLEAN, true)
                        .setAllowExpression(true)
                        .setDefaultValue(new ModelNode(false))
                        .build();
    protected static final SimpleAttributeDefinition DEFAULT_SECURITY_DOMAIN =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_SECURITY_DOMAIN, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(SecurityConstants.DEFAULT_APPLICATION_POLICY))
                    .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SECURITY_DOMAIN_REF)
                    .build();


    static final ApplicationSecurityDomainDefinition APPLICATION_SECURITY_DOMAIN = ApplicationSecurityDomainDefinition.INSTANCE;
    static final AttributeDefinition[] ATTRIBUTES = { DEFAULT_VIRTUAL_HOST, DEFAULT_SERVLET_CONTAINER, DEFAULT_SERVER, INSTANCE_ID, STATISTICS_ENABLED, DEFAULT_SECURITY_DOMAIN };
    static final PersistentResourceDefinition[] CHILDREN = {
            BufferCacheDefinition.INSTANCE,
            ServerDefinition.INSTANCE,
            ServletContainerDefinition.INSTANCE,
            HandlerDefinitions.INSTANCE,
            FilterDefinitions.INSTANCE,
            APPLICATION_SECURITY_DOMAIN
    };

    public static final UndertowRootDefinition INSTANCE = new UndertowRootDefinition();

    private UndertowRootDefinition() {
        super(UndertowExtension.SUBSYSTEM_PATH,
                UndertowExtension.getResolver(),
                new UndertowSubsystemAdd(APPLICATION_SECURITY_DOMAIN.getKnownSecurityDomainPredicate()),
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        return Arrays.asList(CHILDREN);
    }

    static void registerTransformers(SubsystemRegistration subsystemRegistration) {
        registerTransformers_EAP_7_0_0(subsystemRegistration);
    }

    private static void registerTransformers_EAP_7_0_0(SubsystemRegistration subsystemRegistration) {
        final ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        // Version 4.0.0 adds the new SSL_CONTEXT attribute, however it is mutually exclusive to the SECURITY_REALM attribute, both of which can
        // now be set to 'undefined' so instead of rejecting a defined SSL_CONTEXT, reject an undefined SECURITY_REALM as that covers the
        // two new combinations.
        builder.addChildResource(UndertowExtension.HTTPS_LISTENER_PATH)
            .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.UNDEFINED, Constants.SECURITY_REALM)
                .end();

        builder.addChildResource(UndertowExtension.PATH_SERVLET_CONTAINER)
                .getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.DISABLE_FILE_WATCH_SERVICE)
                .end();

        builder.addChildResource(UndertowExtension.PATH_FILTERS)
            .addChildResource(PathElement.pathElement(Constants.MOD_CLUSTER))
                .getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.SSL_CONTEXT)
                    .end();

        builder.addChildResource(UndertowExtension.PATH_HANDLERS)
            .addChildResource(PathElement.pathElement(Constants.REVERSE_PROXY))
                .addChildResource(PathElement.pathElement(Constants.HOST))
                    .getAttributeBuilder()
                        .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.SSL_CONTEXT)
                        .end();

        builder.discardChildResource(PathElement.pathElement(Constants.APPLICATION_SECURITY_DOMAIN));

        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, UndertowExtension.MODEL_VERSION_EAP7_0_0);
    }

}
