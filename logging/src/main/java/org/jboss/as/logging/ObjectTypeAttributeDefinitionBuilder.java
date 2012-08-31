/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import java.util.Arrays;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.logging.resolvers.ModelNodeResolver;

/**
 * Creates a builder for an {@link org.jboss.as.logging.ObjectTypeAttributeDefinition}.
 */
public class ObjectTypeAttributeDefinitionBuilder {
    private final String name;
    private final AttributeDefinition[] valueTypes;
    private String propertyName;
    private String xmlName;
    private boolean allowNull;
    private String[] alternatives;
    private String[] requires;
    private ParameterCorrector corrector;
    private AttributeAccess.Flag[] flags;
    private ModelNodeResolver<String> resolver;
    private String suffix;

    ObjectTypeAttributeDefinitionBuilder(final String name, final AttributeDefinition... valueTypes) {
        this.name = name;
        this.valueTypes = valueTypes;
    }

    /**
     * Creates a builder for {@link ObjectTypeAttributeDefinition}.
     *
     * @param name       the name of the attribute
     * @param valueTypes the value types
     *
     * @return the builder
     */
    public static ObjectTypeAttributeDefinitionBuilder of(final String name, final AttributeDefinition... valueTypes) {
        return new ObjectTypeAttributeDefinitionBuilder(name, valueTypes);
    }

    public ObjectTypeAttributeDefinition build() {
        if (xmlName == null) xmlName = name;
        return new ObjectTypeAttributeDefinition(name, xmlName, propertyName, suffix, resolver, valueTypes, allowNull, corrector, alternatives, requires, flags);
    }


    public ObjectTypeAttributeDefinitionBuilder setXmlName(final String xmlName) {
        this.xmlName = xmlName == null ? this.name : xmlName;
        return this;
    }

    public ObjectTypeAttributeDefinitionBuilder setAllowNull(final boolean allowNull) {
        this.allowNull = allowNull;
        return this;
    }

    public ObjectTypeAttributeDefinitionBuilder setCorrector(ParameterCorrector corrector) {
        this.corrector = corrector;
        return this;
    }

    public ObjectTypeAttributeDefinitionBuilder setAlternatives(final String... alternatives) {
        this.alternatives = alternatives;
        return this;
    }

    public ObjectTypeAttributeDefinitionBuilder addAlternatives(final String... alternatives) {
        if (this.alternatives == null) {
            this.alternatives = alternatives;
        } else {
            String[] newAlternatives = Arrays.copyOf(this.alternatives, this.alternatives.length + alternatives.length);
            System.arraycopy(alternatives, 0, newAlternatives, this.alternatives.length, alternatives.length);
            this.alternatives = newAlternatives;
        }
        return this;
    }

    public ObjectTypeAttributeDefinitionBuilder setRequires(final String... requires) {
        this.requires = requires;
        return this;
    }

    public ObjectTypeAttributeDefinitionBuilder setFlags(final AttributeAccess.Flag... flags) {
        this.flags = flags;
        return this;
    }

    public ObjectTypeAttributeDefinitionBuilder setPropertyName(final String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    public ObjectTypeAttributeDefinitionBuilder setResolver(final ModelNodeResolver<String> resolver) {
        this.resolver = resolver;
        return this;
    }

    public ObjectTypeAttributeDefinitionBuilder setSuffix(final String suffix) {
        this.suffix = suffix;
        return this;
    }
}
