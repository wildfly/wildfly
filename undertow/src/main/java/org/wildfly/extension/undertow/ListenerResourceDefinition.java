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
import java.util.LinkedHashSet;
import java.util.List;

import io.undertow.UndertowOptions;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.io.OptionAttributeDefinition;
import org.wildfly.extension.io.OptionList;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
abstract class ListenerResourceDefinition extends PersistentResourceDefinition {

    protected static final SimpleAttributeDefinition SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(Constants.SOCKET_BINDING, ModelType.STRING)
            .setAllowNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_BINDING_REF)
            .build();
    protected static final SimpleAttributeDefinition WORKER = new SimpleAttributeDefinitionBuilder(Constants.WORKER, ModelType.STRING)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .setDefaultValue(new ModelNode("default"))
            .build();
    protected static final SimpleAttributeDefinition BUFFER_POOL = new SimpleAttributeDefinitionBuilder(Constants.BUFFER_POOL, ModelType.STRING)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .setDefaultValue(new ModelNode("default"))
            .build();
    protected static final SimpleAttributeDefinition ENABLED = new SimpleAttributeDefinitionBuilder(Constants.ENABLED, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(true))
            .setAllowExpression(true)
            .build();

    protected static final SimpleAttributeDefinition REDIRECT_SOCKET = new SimpleAttributeDefinitionBuilder(Constants.REDIRECT_SOCKET, ModelType.STRING)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .setDefaultValue(new ModelNode("https"))
            .setAllowExpression(false)
            .build();


    static final List<OptionAttributeDefinition> OPTIONS = OptionList.builder()
            .addOption(UndertowOptions.MAX_HEADER_SIZE, "max-header-size", new ModelNode(UndertowOptions.DEFAULT_MAX_HEADER_SIZE))
            .addOption(UndertowOptions.MAX_ENTITY_SIZE, Constants.MAX_POST_SIZE, new ModelNode(10485760L))
            .addOption(UndertowOptions.BUFFER_PIPELINED_DATA, "buffer-pipelined-data", new ModelNode(true))
            .addOption(UndertowOptions.MAX_PARAMETERS, "max-parameters", new ModelNode(1000))
            .addOption(UndertowOptions.MAX_HEADERS, "max-headers", new ModelNode(200))
            .addOption(UndertowOptions.MAX_COOKIES, "max-cookies", new ModelNode(200))
            .addOption(UndertowOptions.ALLOW_ENCODED_SLASH, "allow-encoded-slash", new ModelNode(false))
            .addOption(UndertowOptions.DECODE_URL, "decode-url", new ModelNode(true))
            .addOption(UndertowOptions.URL_CHARSET, "url-charset", new ModelNode("UTF-8"))
            .addOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, "always-set-keep-alive", new ModelNode(true))
            .build();

    protected static final Collection ATTRIBUTES;
    protected static final List<AccessConstraintDefinition> CONSTRAINTS = Arrays.asList(UndertowExtension.LISTENER_CONSTRAINT);

    static {
        ATTRIBUTES = new LinkedHashSet<AttributeDefinition>(Arrays.asList(SOCKET_BINDING, WORKER, BUFFER_POOL, ENABLED));
        ATTRIBUTES.addAll(OPTIONS);
    }

    public ListenerResourceDefinition(PathElement pathElement) {
        super(pathElement, UndertowExtension.getResolver(Constants.LISTENER)
        );
    }

    public Collection<AttributeDefinition> getAttributes() {
        //noinspection unchecked
        return ATTRIBUTES;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        super.registerAddOperation(resourceRegistration, getAddHandler(), OperationEntry.Flag.RESTART_NONE);
        super.registerRemoveOperation(resourceRegistration, new ListenerRemoveHandler(getAddHandler()), OperationEntry.Flag.RESTART_NONE);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return CONSTRAINTS;
    }

    protected abstract ListenerAdd getAddHandler();
}
