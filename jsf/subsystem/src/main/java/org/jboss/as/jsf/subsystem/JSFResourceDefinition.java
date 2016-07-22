/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.jsf.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Arrays;
import java.util.Collection;

/**
 * Defines attributes and operations for the JSF Subsystem
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class JSFResourceDefinition extends PersistentResourceDefinition {

    public static final String DEFAULT_SLOT_ATTR_NAME = "default-jsf-impl-slot";
    public static final String DISALLOW_DOCTYPE_DECL_ATTR_NAME = "disallow-doctype-decl";
    public static final String DEFAULT_SLOT = "main";

    public static JSFResourceDefinition INSTANCE = new JSFResourceDefinition();

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

    private JSFResourceDefinition() {
        super(JSFExtension.PATH_SUBSYSTEM,
                JSFExtension.getResourceDescriptionResolver(),
                JSFSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
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
