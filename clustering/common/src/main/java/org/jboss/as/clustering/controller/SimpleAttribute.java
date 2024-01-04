/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.AttributeDefinition;

/**
 * Adapts an {@link AttributeDefinition} to the {@link Attribute} interface.
 * @author Paul Ferraro
 */
public class SimpleAttribute implements Attribute {

    private final AttributeDefinition definition;

    public SimpleAttribute(AttributeDefinition definition) {
        this.definition = definition;
    }

    @Override
    public AttributeDefinition getDefinition() {
        return this.definition;
    }
}
