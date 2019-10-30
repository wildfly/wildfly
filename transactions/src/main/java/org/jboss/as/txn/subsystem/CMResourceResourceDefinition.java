/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReadResourceNameOperationStepHandler;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Implementation of {@link org.jboss.as.controller.ResourceDefinition} for commit-markable-resource.
 *
 * @author Stefano Maestri (c) 2011 Red Hat Inc.
 */
public class CMResourceResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH_CM_RESOURCE = PathElement.pathElement(CommonAttributes.CM_RESOURCE);

    static SimpleAttributeDefinition JNDI_NAME =  new SimpleAttributeDefinitionBuilder(CommonAttributes.CM_JNDI_NAME, ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(true)
            .setResourceOnly()
            .build();

    static SimpleAttributeDefinition CM_TABLE_BATCH_SIZE =  new SimpleAttributeDefinitionBuilder(CommonAttributes.CM_BATCH_SIZE, ModelType.INT)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(new ModelNode(CommonAttributes.CM_BATCH_SIZE_DEF_VAL))
            .setXmlName(CommonAttributes.CM_BATCH_SIZE)
            .build();

    static SimpleAttributeDefinition CM_TABLE_IMMEDIATE_CLEANUP =  new SimpleAttributeDefinitionBuilder(CommonAttributes.CM_IMMEDIATE_CLEANUP, ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(new ModelNode(CommonAttributes.CM_IMMEDIATE_CLEANUP_DEF_VAL))
            .setXmlName(CommonAttributes.CM_IMMEDIATE_CLEANUP)
            .build();

    static SimpleAttributeDefinition CM_TABLE_NAME = new SimpleAttributeDefinitionBuilder(CommonAttributes.CM_LOCATION_NAME, ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(new ModelNode(CommonAttributes.CM_LOCATION_NAME_DEF_VAL))
            .setXmlName(CommonAttributes.CM_LOCATION_NAME)
            .build();


    /**
     * Default constructure.
     * It set {@link org.jboss.as.txn.subsystem.CMResourceAdd} as add handler
     * and {@link org.jboss.as.controller.ReloadRequiredRemoveStepHandler} as remove handler
     */
    public CMResourceResourceDefinition() {
        super(PATH_CM_RESOURCE,
                TransactionExtension.getResourceDescriptionResolver(CommonAttributes.CM_RESOURCE),
                CMResourceAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        ReloadRequiredWriteAttributeHandler reloadWrtiteHandler = new ReloadRequiredWriteAttributeHandler(JNDI_NAME, CM_TABLE_NAME, CM_TABLE_BATCH_SIZE, CM_TABLE_IMMEDIATE_CLEANUP);
        resourceRegistration.registerReadWriteAttribute(CM_TABLE_NAME, null, reloadWrtiteHandler);
        resourceRegistration.registerReadWriteAttribute(CM_TABLE_BATCH_SIZE, null, reloadWrtiteHandler);
        resourceRegistration.registerReadWriteAttribute(CM_TABLE_IMMEDIATE_CLEANUP, null, reloadWrtiteHandler);

        //This comes from the address
        resourceRegistration.registerReadOnlyAttribute(JNDI_NAME, ReadResourceNameOperationStepHandler.INSTANCE);
    }
}

