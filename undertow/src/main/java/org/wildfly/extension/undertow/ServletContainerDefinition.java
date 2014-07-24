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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.undertow.servlet.api.ServletStackTraces;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class ServletContainerDefinition extends PersistentResourceDefinition {
    static final ServletContainerDefinition INSTANCE = new ServletContainerDefinition();

    protected static final SimpleAttributeDefinition ALLOW_NON_STANDARD_WRAPPERS =
            new SimpleAttributeDefinitionBuilder(Constants.ALLOW_NON_STANDARD_WRAPPERS, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition DEFAULT_BUFFER_CACHE =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_BUFFER_CACHE, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode("default"))
                    .build();

    protected static final SimpleAttributeDefinition STACK_TRACE_ON_ERROR =
            new SimpleAttributeDefinitionBuilder(Constants.STACK_TRACE_ON_ERROR, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(ServletStackTraces.LOCAL_ONLY.toString()))
                    .setValidator(new EnumValidator<>(ServletStackTraces.class, true, true))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition DEFAULT_ENCODING =
            new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_ENCODING, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .build();

    protected static final AttributeDefinition USE_LISTENER_ENCODING =
            new SimpleAttributeDefinitionBuilder(Constants.USE_LISTENER_ENCODING, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();


    protected static final AttributeDefinition IGNORE_FLUSH =
            new SimpleAttributeDefinitionBuilder(Constants.IGNORE_FLUSH, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();


    protected static final AttributeDefinition EAGER_FILTER_INIT =
            new SimpleAttributeDefinitionBuilder("eager-filter-initialization", ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();


    protected static final AttributeDefinition DEFAULT_SESSION_TIMEOUT =
            new SimpleAttributeDefinitionBuilder("default-session-timeout", ModelType.INT, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .setMeasurementUnit(MeasurementUnit.MINUTES)
                    .setDefaultValue(new ModelNode(30)) //30 minutes
                    .build();


    private static final List<? extends PersistentResourceDefinition> CHILDREN;
    private static final Collection<AttributeDefinition> ATTRIBUTES = Arrays.asList(
            ALLOW_NON_STANDARD_WRAPPERS,
            DEFAULT_BUFFER_CACHE,
            STACK_TRACE_ON_ERROR,
            DEFAULT_ENCODING,
            USE_LISTENER_ENCODING,
            IGNORE_FLUSH,
            EAGER_FILTER_INIT,
            DEFAULT_SESSION_TIMEOUT);

    static {
        List<PersistentResourceDefinition>  children = new ArrayList<>();
        children.add(JspDefinition.INSTANCE);
        children.add(SessionCookieDefinition.INSTANCE);
        children.add(PersistentSessionsDefinition.INSTANCE);
        CHILDREN = Collections.unmodifiableList(children);
    }

    private ServletContainerDefinition() {
        super(UndertowExtension.PATH_SERVLET_CONTAINER,
                UndertowExtension.getResolver(Constants.SERVLET_CONTAINER),
                ServletContainerAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        return CHILDREN;
    }
}
