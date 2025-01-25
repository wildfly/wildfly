/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
