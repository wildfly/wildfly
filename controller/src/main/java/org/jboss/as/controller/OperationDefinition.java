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

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelType;

import java.util.EnumSet;

/**
 * Defining characteristics of operation in a {@link org.jboss.as.controller.registry.Resource}
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public abstract class OperationDefinition {
    protected final String name;
    protected final OperationEntry.EntryType entryType;
    protected final EnumSet<OperationEntry.Flag> flags;
    protected final AttributeDefinition[] parameters;
    protected final ModelType replyType;
    protected final ModelType replyValueType;

    public OperationDefinition(String name,
                               OperationEntry.EntryType entryType,
                               EnumSet<OperationEntry.Flag> flags,
                               final ModelType replyType,
                               final ModelType replyValueType,
                               AttributeDefinition... parameters) {
        this.name = name;
        this.entryType = entryType;
        this.flags = flags;
        this.parameters = parameters;
        this.replyType = replyType;
        this.replyValueType = replyValueType;
    }

    public String getName() {
        return name;
    }

    public OperationEntry.EntryType getEntryType() {
        return entryType;
    }

    public EnumSet<OperationEntry.Flag> getFlags() {
        return flags;
    }

    public AttributeDefinition[] getParameters() {
        return parameters;
    }

    public ModelType getReplyType() {
        return replyType;
    }

    public ModelType getReplyValueType() {
        return replyValueType;
    }

    public abstract DescriptionProvider getDescriptionProvider();
}
