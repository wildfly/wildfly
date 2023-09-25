/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class BufferCacheDefinition extends PersistentResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.BUFFER_CACHE);

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

    static final List<AttributeDefinition> ATTRIBUTES = Collections.unmodifiableList(Arrays.asList(BUFFER_SIZE, BUFFERS_PER_REGION, MAX_REGIONS));

    BufferCacheDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getKey()))
                .setAddHandler(BufferCacheAdd.INSTANCE)
                .setRemoveHandler(new ServiceRemoveStepHandler(BufferCacheService.SERVICE_NAME, BufferCacheAdd.INSTANCE))
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }
}
