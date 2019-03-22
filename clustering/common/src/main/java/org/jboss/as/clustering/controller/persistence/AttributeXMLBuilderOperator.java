/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller.persistence;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription.PersistentResourceXMLBuilder;

/**
 * PersistentResourceXMLBuilder operator that adds the attributes of a given Attribute enumeration
 * @author Paul Ferraro
 */
public class AttributeXMLBuilderOperator implements UnaryOperator<PersistentResourceXMLBuilder> {

    private final List<Attribute> attributes;

    public AttributeXMLBuilderOperator() {
        this.attributes = new LinkedList<>();
    }

    public <A extends Enum<A> & Attribute> AttributeXMLBuilderOperator(Class<A> attributeClass) {
        this.attributes = new ArrayList<>(EnumSet.allOf(attributeClass));
    }

    public <A extends Enum<A> & Attribute> AttributeXMLBuilderOperator addAttributes(Class<A> attributeClass) {
        return this.addAttributes(EnumSet.allOf(attributeClass));
    }

    public <A extends Attribute> AttributeXMLBuilderOperator addAttributes(Set<A> attributes) {
        this.attributes.addAll(attributes);
        return this;
    }

    @Override
    public PersistentResourceXMLBuilder apply(PersistentResourceXMLBuilder builder) {
        // Ensure element-based attributes are processed last
        List<AttributeDefinition> elementAttributes = new LinkedList<>();
        for (Attribute attribute : this.attributes) {
            AttributeDefinition definition = attribute.getDefinition();
            if (definition.getParser().isParseAsElement()) {
                elementAttributes.add(definition);
            } else {
                builder.addAttribute(definition, definition.getParser(), definition.getMarshaller());
            }
        }
        for (AttributeDefinition attribute : elementAttributes) {
            builder.addAttribute(attribute, attribute.getParser(), attribute.getMarshaller());
        }
        return builder;
    }
}
