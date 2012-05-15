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

import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelType;

import java.util.EnumSet;

/**
 * Defining characteristics of operation in a {@link org.jboss.as.controller.registry.Resource}
 * SimpleOperationDefinition is simplest implementation that uses {@link DefaultOperationDescriptionProvider} for generating description of operation
 * if more complex DescriptionProvider user should extend this class or {@link OperationDefinition} and provide its own {@link DescriptionProvider}
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class SimpleOperationDefinition extends OperationDefinition {

    private ResourceDescriptionResolver resolver;

    public SimpleOperationDefinition(final String name, final ResourceDescriptionResolver resolver) {
        this(name, resolver, EnumSet.noneOf(OperationEntry.Flag.class));
    }

    public SimpleOperationDefinition(final String name, final ResourceDescriptionResolver resolver, AttributeDefinition... parameters) {
        this(name, resolver, OperationEntry.EntryType.PUBLIC, EnumSet.noneOf(OperationEntry.Flag.class), parameters);
    }

    public SimpleOperationDefinition(final String name, final ResourceDescriptionResolver resolver, final EnumSet<OperationEntry.Flag> flags) {
        this(name, resolver, OperationEntry.EntryType.PUBLIC, flags, new AttributeDefinition[0]);
    }

    public SimpleOperationDefinition(final String name, final ResourceDescriptionResolver resolver, OperationEntry.EntryType entryType, EnumSet<OperationEntry.Flag> flags) {
        this(name, resolver, entryType, flags, new AttributeDefinition[0]);
    }

    public SimpleOperationDefinition(final String name, final ResourceDescriptionResolver resolver, OperationEntry.EntryType entryType, EnumSet<OperationEntry.Flag> flags, AttributeDefinition... parameters) {
        super(name, entryType, flags, null, null, parameters);
        this.resolver = resolver;
    }

    public SimpleOperationDefinition(final String name,
                                     final ResourceDescriptionResolver resolver,
                                     OperationEntry.EntryType entryType,
                                     final EnumSet<OperationEntry.Flag> flags,
                                     final ModelType replyType,
                                     final ModelType replyValueType,
                                     AttributeDefinition... parameters) {
        super(name, entryType, flags, replyType, replyValueType, parameters);
        this.resolver = resolver;
    }

    @Override
    public DescriptionProvider getDescriptionProvider() {
        return new DefaultOperationDescriptionProvider(getName(), resolver, replyType, replyValueType, parameters);
    }

}
