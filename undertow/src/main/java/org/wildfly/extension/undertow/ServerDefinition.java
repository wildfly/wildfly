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
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class ServerDefinition extends PersistentResourceDefinition {

    static final SimpleAttributeDefinition DEFAULT_HOST = new SimpleAttributeDefinitionBuilder(Constants.DEFAULT_HOST, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("default-host"))
            .build();
    static final SimpleAttributeDefinition SERVLET_CONTAINER = new SimpleAttributeDefinitionBuilder(Constants.SERVLET_CONTAINER, ModelType.STRING)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode("default"))
            .build();
    static final AttributeDefinition[] ATTRIBUTES = {DEFAULT_HOST, SERVLET_CONTAINER};
    static final List<PersistentResourceDefinition> CHILDREN = Arrays.asList(
            AjpListenerResourceDefinition.INSTANCE,
            HttpListenerResourceDefinition.INSTANCE,
            HttpsListenerResourceDefinition.INSTANCE,
            HostDefinition.INSTANCE);

    static final ServerDefinition INSTANCE = new ServerDefinition();

    private ServerDefinition() {
        super(UndertowExtension.SERVER_PATH, UndertowExtension.getResolver(Constants.SERVER), new ServerAdd(), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        //noinspection unchecked
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return CHILDREN;
    }
}
