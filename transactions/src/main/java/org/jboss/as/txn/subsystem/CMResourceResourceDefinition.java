/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReadResourceNameOperationStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
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
     * It set {@link CMResourceAdd} as add handler
     * and {@link org.jboss.as.controller.ReloadRequiredRemoveStepHandler} as remove handler
     */
    public CMResourceResourceDefinition() {
        super(new Parameters(PATH_CM_RESOURCE,
                TransactionExtension.getResourceDescriptionResolver(CommonAttributes.CM_RESOURCE))
                .setAddHandler(CMResourceAdd.INSTANCE)
                .setAddRestartLevel(OperationEntry.Flag.RESTART_JVM)
                .setRemoveHandler(new AbstractRemoveStepHandler() { // TODO consider adding a RestartRequiredRemoveStepHandler to WildFly Core
                    @Override
                    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
                        context.restartRequired();
                    }

                    @Override
                    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
                        context.revertRestartRequired();
                    }
                })
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_JVM)
        );
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

