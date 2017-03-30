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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class BufferCacheDefinition extends PersistentResourceDefinition {
    protected static final SimpleAttributeDefinition BUFFER_SIZE = new SimpleAttributeDefinitionBuilder(Constants.BUFFER_SIZE, ModelType.INT)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true))
            .setDefaultValue(new ModelNode(1024))
            .setAllowExpression(true)
            .build();
    protected static final SimpleAttributeDefinition BUFFERS_PER_REGION = new SimpleAttributeDefinitionBuilder(Constants.BUFFERS_PER_REGION, ModelType.INT)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true))
            .setDefaultValue(new ModelNode(1024))
            .setAllowExpression(true)
            .build();
    protected static final SimpleAttributeDefinition MAX_REGIONS = new SimpleAttributeDefinitionBuilder(Constants.MAX_REGIONS, ModelType.INT)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, false, true))
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(10))
            .build();
    static final BufferCacheDefinition INSTANCE = new BufferCacheDefinition();
    private static final List<SimpleAttributeDefinition> ATTRIBUTES = Collections.unmodifiableList(Arrays.asList(BUFFER_SIZE, BUFFERS_PER_REGION, MAX_REGIONS));

    private BufferCacheDefinition() {
        super(UndertowExtension.PATH_BUFFER_CACHE,
                UndertowExtension.getResolver(Constants.BUFFER_CACHE),
                BufferCacheAdd.INSTANCE,
                new ServiceRemoveStepHandler(BufferCacheService.SERVICE_NAME, BufferCacheAdd.INSTANCE));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return (Collection) ATTRIBUTES;
    }
}
