/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2012, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.jboss.as.controller;

import java.util.Arrays;
import java.util.EnumSet;

import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelType;

/**
 * Builder for helping construct {@link SimpleOperationDefinition}
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class SimpleOperationDefinitionBuilder {
    private ResourceDescriptionResolver resolver;
    private ResourceDescriptionResolver attributeResolver;
    protected String name;
    protected OperationEntry.EntryType entryType = OperationEntry.EntryType.PUBLIC;
    protected EnumSet<OperationEntry.Flag> flags = EnumSet.noneOf(OperationEntry.Flag.class);
    protected AttributeDefinition[] parameters = new AttributeDefinition[0];
    protected ModelType replyType;
    protected ModelType replyValueType;
    protected DeprecationData deprecationData = null;
    protected AttributeDefinition[] replyParameters = new AttributeDefinition[0];

    public SimpleOperationDefinitionBuilder(String name, ResourceDescriptionResolver resolver) {
        this.name = name;
        this.resolver = resolver;
    }


    public SimpleOperationDefinition build() {
        if (attributeResolver == null) {
            attributeResolver = resolver;
        }
        return new SimpleOperationDefinition(name, resolver, attributeResolver, entryType, flags, replyType, replyValueType, deprecationData, replyParameters, parameters);
    }

    public SimpleOperationDefinitionBuilder setEntryType(OperationEntry.EntryType entryType) {
        this.entryType = entryType;
        return this;
    }

    public SimpleOperationDefinitionBuilder setPrivateEntry() {
        this.entryType = OperationEntry.EntryType.PRIVATE;
        return this;
    }

    public SimpleOperationDefinitionBuilder withFlags(EnumSet<OperationEntry.Flag> flags) {
        this.flags = flags;
        return this;
    }

    public SimpleOperationDefinitionBuilder withFlag(OperationEntry.Flag flag) {
        this.flags = EnumSet.of(flag);
        return this;
    }

    public SimpleOperationDefinitionBuilder setRuntimeOnly() {
        this.flags = EnumSet.of(OperationEntry.Flag.RUNTIME_ONLY, flags.toArray(new OperationEntry.Flag[flags.size()]));
        return this;
    }

    public SimpleOperationDefinitionBuilder setReadOnly() {
        this.flags = EnumSet.of(OperationEntry.Flag.READ_ONLY, flags.toArray(new OperationEntry.Flag[flags.size()]));
        return this;
    }


    public SimpleOperationDefinitionBuilder setParameters(AttributeDefinition... parameters) {
        this.parameters = parameters;
        return this;
    }

    public SimpleOperationDefinitionBuilder addParameter(AttributeDefinition parameter) {
        int i = parameters.length;
        parameters = Arrays.copyOf(parameters, i + 1);
        parameters[i] = parameter;
        return this;
    }

    public SimpleOperationDefinitionBuilder setReplyType(ModelType replyType) {
        this.replyType = replyType;
        return this;
    }

    public SimpleOperationDefinitionBuilder setReplyValueType(ModelType replyValueType) {
        this.replyValueType = replyValueType;
        return this;
    }

    public SimpleOperationDefinitionBuilder setDeprecated(ModelVersion since) {
        this.deprecationData = new DeprecationData(since);
        return this;
    }

    public SimpleOperationDefinitionBuilder setReplyParameters(AttributeDefinition... replyParameters) {
        this.replyParameters = replyParameters;
        return this;
    }

    public SimpleOperationDefinitionBuilder setAttributeResolver(ResourceDescriptionResolver resolver) {
        this.attributeResolver = resolver;
        return this;
    }
}
