/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Eduardo Martins
 */
public class DefaultManagedThreadFactoryResourceDefinition extends SimpleResourceDefinition {

    public static final String PRIORITY = "priority";

    public static final SimpleAttributeDefinition PRIORITY_AD =
            new SimpleAttributeDefinitionBuilder(PRIORITY, ModelType.INT, true)
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(Thread.MIN_PRIORITY, Thread.MAX_PRIORITY, true, true))
                    .setDefaultValue(new ModelNode(Thread.NORM_PRIORITY))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = {PRIORITY_AD};

    public static final DefaultManagedThreadFactoryResourceDefinition INSTANCE = new DefaultManagedThreadFactoryResourceDefinition();

    private DefaultManagedThreadFactoryResourceDefinition() {
        super(EESubsystemModel.DEFAULT_MANAGED_THREAD_FACTORY_PATH, EeExtension.getResourceDescriptionResolver(EESubsystemModel.DEFAULT_MANAGED_THREAD_FACTORY), DefaultManagedThreadFactoryAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }
}
