/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.function.UnaryOperator;

/**
 * {@link ResourceDescriptor} configurator for the common case of adding an enumeration of attributes.
 * @author Paul Ferraro
 */
public class SimpleResourceDescriptorConfigurator<E extends Enum<E> & Attribute> implements UnaryOperator<ResourceDescriptor> {
    private final Class<E> attributeClass;

    public SimpleResourceDescriptorConfigurator(Class<E> attributeClass) {
        this.attributeClass = attributeClass;
    }

    @Override
    public ResourceDescriptor apply(ResourceDescriptor descriptor) {
        return descriptor.addAttributes(this.attributeClass);
    }
}
