/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.iiop.openjdk;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class PropertyDefinition extends PersistentResourceDefinition {
    protected static final AttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(
            Constants.PROPERTY_VALUE, ModelType.STRING, true).setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES).build();

    protected static final List<AttributeDefinition> ATTRIBUTES = Collections.unmodifiableList(Arrays.asList(VALUE));

    static final PropertyDefinition INSTANCE = new PropertyDefinition();

    private PropertyDefinition() {
        super(IIOPExtension.PATH_PROPERTY, IIOPExtension.getResourceDescriptionResolver(
                Constants.PROPERTIES, Constants.PROPERTY),
                new AbstractAddStepHandler(ATTRIBUTES), ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}
