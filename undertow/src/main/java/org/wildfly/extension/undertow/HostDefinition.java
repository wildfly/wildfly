/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.DynamicNameMappers;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.web.host.WebHost;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.filters.FilterRefDefinition;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class HostDefinition extends PersistentResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.HOST);
    public static final String DEFAULT_WEB_MODULE_DEFAULT = "ROOT.war";

    static final RuntimeCapability<Void> HOST_CAPABILITY = RuntimeCapability.Builder.of(Capabilities.CAPABILITY_HOST, true, Host.class)
            .addRequirements(Capabilities.CAPABILITY_UNDERTOW)
            //addDynamicRequirements(Capabilities.CAPABILITY_SERVER) -- has no function so don't use it
            .setDynamicNameMapper(DynamicNameMappers.PARENT)
            .build();


    static final StringListAttributeDefinition ALIAS = new StringListAttributeDefinition.Builder(Constants.ALIAS)
            .setRequired(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setElementValidator(new StringLengthValidator(1))
            .setAllowExpression(true)
            .setAttributeParser(AttributeParser.COMMA_DELIMITED_STRING_LIST)
            .setAttributeMarshaller(AttributeMarshaller.COMMA_STRING_LIST)
            .build();
    static final SimpleAttributeDefinition DEFAULT_WEB_MODULE = new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_WEB_MODULE, ModelType.STRING, true)
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1, true, false))
            .setDefaultValue(new ModelNode(DEFAULT_WEB_MODULE_DEFAULT))
            .build();

    static final SimpleAttributeDefinition DEFAULT_RESPONSE_CODE = new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_RESPONSE_CODE, ModelType.INT, true)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(400, 599, true, true))
            .setDefaultValue(new ModelNode(404))
            .setAllowExpression(true)
            .build();
    static final SimpleAttributeDefinition DISABLE_CONSOLE_REDIRECT = new SimpleAttributeDefinitionBuilder("disable-console-redirect", ModelType.BOOLEAN, true)
            .setRestartAllServices()
            .setDefaultValue(ModelNode.FALSE)
            .setAllowExpression(true)
            .build();
    static final SimpleAttributeDefinition QUEUE_REQUESTS_ON_START = new SimpleAttributeDefinitionBuilder("queue-requests-on-start", ModelType.BOOLEAN, true)
            .setRestartAllServices()
            .setDefaultValue(ModelNode.TRUE)
            .setAllowExpression(true)
            .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(ALIAS, DEFAULT_WEB_MODULE, DEFAULT_RESPONSE_CODE, DISABLE_CONSOLE_REDIRECT, QUEUE_REQUESTS_ON_START);

    HostDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getKey()))
                .setAddHandler(HostAdd.INSTANCE)
                .setRemoveHandler(new HostRemove())
                .addCapabilities(HOST_CAPABILITY, WebHost.CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        return List.of(
                new LocationDefinition(),
                new AccessLogDefinition(),
                new ConsoleAccessLogDefinition(),
                new FilterRefDefinition(),
                new HttpInvokerDefinition(),
                new HostSingleSignOnDefinition());
    }

}
