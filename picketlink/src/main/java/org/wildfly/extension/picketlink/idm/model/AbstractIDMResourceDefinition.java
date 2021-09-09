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

import java.util.function.Function;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.wildfly.extension.picketlink.common.model.AbstractResourceDefinition;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.validator.ModelValidationStepHandler;
import org.wildfly.extension.picketlink.idm.IDMExtension;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 18, 2012
 */
public abstract class AbstractIDMResourceDefinition extends AbstractResourceDefinition {

    private final ModelValidationStepHandler[] modelValidators;
    private final Function<PathAddress, PathAddress> partitionAddressProvider;

    protected AbstractIDMResourceDefinition(final ModelElement modelElement,
                                            final Function<PathAddress, PathAddress> partitionAddressProvider,
                                            final SimpleAttributeDefinition... attributes) {
        this(modelElement, null, partitionAddressProvider, attributes);
    }

    protected AbstractIDMResourceDefinition(final ModelElement modelElement,
                                            final ModelValidationStepHandler[] modelValidators,
                                            final Function<PathAddress, PathAddress> partitionAddressProvider,
                                            final SimpleAttributeDefinition... attributes) {
        super(modelElement, new DefaultAddStepHandler(modelValidators, partitionAddressProvider, attributes),
                new DefaultRemoveStepHandler(partitionAddressProvider),
                IDMExtension.getResourceDescriptionResolver(modelElement.getName()),
                attributes);
        this.modelValidators = modelValidators;
        this.partitionAddressProvider = partitionAddressProvider;
    }

    @Override
    protected OperationStepHandler createAttributeWriterHandler() {
        return new IDMConfigWriteAttributeHandler(modelValidators, partitionAddressProvider, getAttributes().toArray(new AttributeDefinition[0]));
    }
}
