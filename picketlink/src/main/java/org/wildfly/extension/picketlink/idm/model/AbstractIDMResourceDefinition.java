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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.wildfly.extension.picketlink.common.model.AbstractResourceDefinition;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.idm.IDMExtension;

import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 18, 2012
 */
public abstract class AbstractIDMResourceDefinition extends AbstractResourceDefinition {

    protected AbstractIDMResourceDefinition(ModelElement modelElement, OperationStepHandler addHandler, OperationStepHandler removeHandler, SimpleAttributeDefinition... attributes) {
        super(modelElement, addHandler, removeHandler, IDMExtension.getResourceDescriptionResolver(modelElement.getName()), attributes);
    }

    protected AbstractIDMResourceDefinition(ModelElement modelElement, String name, OperationStepHandler addHandler, SimpleAttributeDefinition... attributes) {
        super(modelElement, name, addHandler, IDMConfigRemoveStepHandler.INSTANCE, IDMExtension.getResourceDescriptionResolver(modelElement.getName()), attributes);
    }

    protected AbstractIDMResourceDefinition(ModelElement modelElement, OperationStepHandler addHandler, SimpleAttributeDefinition... attributes) {
        super(modelElement, addHandler, IDMConfigRemoveStepHandler.INSTANCE, IDMExtension.getResourceDescriptionResolver(modelElement.getName()), attributes);
    }

    @Override
    protected OperationStepHandler createAttributeWriterHandler() {
        List<SimpleAttributeDefinition> attributes = getAttributes();
        return new IDMConfigWriteAttributeHandler(attributes.toArray(new AttributeDefinition[attributes.size()]));
    }
}
