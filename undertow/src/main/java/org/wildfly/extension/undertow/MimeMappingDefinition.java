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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.dmr.ModelType;

import java.util.Collection;
import java.util.List;

/**
 * Global mime mapping config for file types
 *
 * @author Stuart Douglas
 */
class MimeMappingDefinition extends PersistentResourceDefinition {

    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.MIME_MAPPING);
    protected static final SimpleAttributeDefinition VALUE =
            new SimpleAttributeDefinitionBuilder(Constants.VALUE, ModelType.STRING, false)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .build();


    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(VALUE);

    MimeMappingDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getKey()))
                .setAddHandler(new ReloadRequiredAddStepHandler(ATTRIBUTES))
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}
