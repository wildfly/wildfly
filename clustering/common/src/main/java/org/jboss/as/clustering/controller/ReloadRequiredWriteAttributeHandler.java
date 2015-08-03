/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Convenience extension of {@link org.jboss.as.controller.ReloadRequiredWriteAttributeHandler} that can be initialized with an {@link Attribute} set.
 * @author Paul Ferraro
 */
public class ReloadRequiredWriteAttributeHandler extends org.jboss.as.controller.ReloadRequiredWriteAttributeHandler implements Registration {

    private final Map<String, AttributeDefinition> attributes = new HashMap<>();

    public <E extends Enum<E> & Attribute> ReloadRequiredWriteAttributeHandler(Class<E> enumClass) {
        this(EnumSet.allOf(enumClass));
    }

    public ReloadRequiredWriteAttributeHandler(Attribute... attributes) {
        this(Arrays.asList(attributes));
    }

    public ReloadRequiredWriteAttributeHandler(Iterable<? extends Attribute> attributes) {
        for (Attribute attribute : attributes) {
            AttributeDefinition definition = attribute.getDefinition();
            this.attributes.put(definition.getName(), definition);
        }
    }

    @Override
    protected AttributeDefinition getAttributeDefinition(String name) {
        return this.attributes.get(name);
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        for (AttributeDefinition attribute : this.attributes.values()) {
            registration.registerReadWriteAttribute(attribute, null, this);
        }
    }
}
