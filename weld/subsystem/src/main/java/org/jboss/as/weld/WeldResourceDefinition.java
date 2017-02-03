/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource definition for Weld subsystem
 *
 * @author Jozef Hartinger
 *
 */
class WeldResourceDefinition extends PersistentResourceDefinition {

    static final WeldResourceDefinition INSTANCE = new WeldResourceDefinition();

    static final String REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE_NAME = "require-bean-descriptor";
    static final String NON_PORTABLE_MODE_ATTRIBUTE_NAME = "non-portable-mode";
    static final String DEVELOPMENT_MODE_ATTRIBUTE_NAME = "development-mode";
    static final String THREAD_POOL_SIZE = "thread-pool-size";

    static final SimpleAttributeDefinition REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder(REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE_NAME, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition NON_PORTABLE_MODE_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder(NON_PORTABLE_MODE_ATTRIBUTE_NAME, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition DEVELOPMENT_MODE_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder(DEVELOPMENT_MODE_ATTRIBUTE_NAME, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition THREAD_POOL_SIZE_ATTRIBUTE =
            new SimpleAttributeDefinitionBuilder(THREAD_POOL_SIZE, ModelType.INT, true)
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(1))
            .setRestartAllServices()
            .build();

    private WeldResourceDefinition() {
        super(
                WeldExtension.PATH_SUBSYSTEM,
                WeldExtension.getResourceDescriptionResolver(),
                WeldSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(new AttributeDefinition[] {REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE, NON_PORTABLE_MODE_ATTRIBUTE, DEVELOPMENT_MODE_ATTRIBUTE, THREAD_POOL_SIZE_ATTRIBUTE});
    }
}
