/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jsf.subsystem;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.weld.Capabilities;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Defines attributes and operations for the Jakarta Server Faces Subsystem
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class JSFResourceDefinition extends PersistentResourceDefinition {

    public static final String DEFAULT_SLOT_ATTR_NAME = "default-jsf-impl-slot";
    public static final String DISALLOW_DOCTYPE_DECL_ATTR_NAME = "disallow-doctype-decl";
    public static final String DEFAULT_SLOT = "main";
    static final RuntimeCapability<Void> FACES_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.faces")
            .addRequirements(Capabilities.WELD_CAPABILITY_NAME)
            .build();

    protected static final SimpleAttributeDefinition DEFAULT_JSF_IMPL_SLOT =
            new SimpleAttributeDefinitionBuilder(DEFAULT_SLOT_ATTR_NAME, ModelType.STRING, true)
            .setAllowExpression(true)
            .setXmlName(DEFAULT_SLOT_ATTR_NAME)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(DEFAULT_SLOT))
            .build();

    static final SimpleAttributeDefinition DISALLOW_DOCTYPE_DECL =
            new SimpleAttributeDefinitionBuilder(DISALLOW_DOCTYPE_DECL_ATTR_NAME, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    JSFResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(JSFExtension.PATH_SUBSYSTEM, JSFExtension.SUBSYSTEM_RESOLVER)
                .setAddHandler(JSFSubsystemAdd.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .addCapabilities(FACES_CAPABILITY)
        );
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        resourceRegistration.registerOperationHandler(JSFImplListHandler.DEFINITION, new JSFImplListHandler());
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(DEFAULT_JSF_IMPL_SLOT, DISALLOW_DOCTYPE_DECL);
    }

}
