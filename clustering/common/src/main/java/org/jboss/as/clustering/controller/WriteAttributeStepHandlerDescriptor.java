/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AttributeDefinition;

/**
 * @author Paul Ferraro
 */
public interface WriteAttributeStepHandlerDescriptor extends OperationStepHandlerDescriptor {

    /**
     * Attributes of the add operation.
     * @return a collection of attributes
     */
    Collection<AttributeDefinition> getAttributes();

    /**
     * Attributes (not specified by {@link #getAttributes()}) will be ignored at runtime..
     * @return a collection of ignored attributes
     */
    default Collection<AttributeDefinition> getIgnoredAttributes() {
        return Collections.emptySet();
    }
}
