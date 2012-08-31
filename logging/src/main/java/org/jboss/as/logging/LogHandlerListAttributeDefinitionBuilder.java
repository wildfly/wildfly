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

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.AttributeAccess;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LogHandlerListAttributeDefinitionBuilder {

    private final String name;
    private final SimpleAttributeDefinition valueType;
    private String xmlName;
    private boolean allowNull;
    private String[] alternatives;
    private String[] requires;
    private AttributeAccess.Flag[] flags;
    private String propertyName;
    private int minSize;
    private int maxSize;

    LogHandlerListAttributeDefinitionBuilder(final String name, final SimpleAttributeDefinition valueType) {
        this.name = name;
        this.valueType = valueType;
    }

    /**
     * Creates a builder for {@link LogHandlerListAttributeDefinition}.
     *
     * @param name      the name of the attribute
     * @param valueType the value type for the list entry
     *
     * @return the builder
     */
    public static LogHandlerListAttributeDefinitionBuilder of(final String name, final SimpleAttributeDefinition valueType) {
        return new LogHandlerListAttributeDefinitionBuilder(name, valueType);
    }

    public LogHandlerListAttributeDefinition build() {
        if (xmlName == null) xmlName = name;
        if (maxSize < 1) maxSize = Integer.MAX_VALUE;
        if (propertyName == null) propertyName = name;
        return new LogHandlerListAttributeDefinition(name, xmlName, propertyName, valueType, allowNull, minSize, maxSize, alternatives, requires, flags);
    }


    public LogHandlerListAttributeDefinitionBuilder setXmlName(final String xmlName) {
        this.xmlName = xmlName == null ? this.name : xmlName;
        return this;
    }

    public LogHandlerListAttributeDefinitionBuilder setAllowNull(final boolean allowNull) {
        this.allowNull = allowNull;
        return this;
    }

    public LogHandlerListAttributeDefinitionBuilder setAlternatives(final String... alternatives) {
        this.alternatives = alternatives;
        return this;
    }

    public LogHandlerListAttributeDefinitionBuilder addAlternatives(final String... alternatives) {
        if (this.alternatives == null) {
            this.alternatives = alternatives;
        } else {
            String[] newAlternatives = Arrays.copyOf(this.alternatives, this.alternatives.length + alternatives.length);
            System.arraycopy(alternatives, 0, newAlternatives, this.alternatives.length, alternatives.length);
            this.alternatives = newAlternatives;
        }
        return this;
    }

    public LogHandlerListAttributeDefinitionBuilder setRequires(final String... requires) {
        this.requires = requires;
        return this;
    }

    public LogHandlerListAttributeDefinitionBuilder setFlags(final AttributeAccess.Flag... flags) {
        this.flags = flags;
        return this;
    }

    public LogHandlerListAttributeDefinitionBuilder setMaxSize(final int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public LogHandlerListAttributeDefinitionBuilder setMinSize(final int minSize) {
        this.minSize = minSize;
        return this;
    }

    public LogHandlerListAttributeDefinitionBuilder setPropertyName(final String propertyName) {
        this.propertyName = propertyName;
        return this;
    }
}
