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

package org.wildfly.extension.picketlink.idm.model;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.picketlink.common.model.ModelElement;

public abstract class AbstractIdentityStoreResourceDefinition extends AbstractIDMResourceDefinition {

    public static final SimpleAttributeDefinition SUPPORT_ATTRIBUTE = new SimpleAttributeDefinitionBuilder(ModelElement.IDENTITY_STORE_SUPPORT_ATTRIBUTE.getName(), ModelType.BOOLEAN, true)
        .setDefaultValue(new ModelNode(true))
        .setAllowExpression(true)
        .build();

    public static final SimpleAttributeDefinition SUPPORT_CREDENTIAL = new SimpleAttributeDefinitionBuilder(ModelElement.IDENTITY_STORE_SUPPORT_CREDENTIAL.getName(), ModelType.BOOLEAN, true)
        .setDefaultValue(new ModelNode(true))
        .setAllowExpression(true)
        .build();

    protected AbstractIdentityStoreResourceDefinition(ModelElement modelElement, OperationStepHandler addHandler, SimpleAttributeDefinition... attributes) {
        super(modelElement, addHandler, IdentityStoreRemoveStepHandler.INSTANCE, attributes);
    }
}
