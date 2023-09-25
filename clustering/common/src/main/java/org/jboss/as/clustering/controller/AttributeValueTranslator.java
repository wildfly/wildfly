/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Translates an attribute value.
 * @author Paul Ferraro
 */
@FunctionalInterface
public interface AttributeValueTranslator {
    ModelNode translate(OperationContext context, ModelNode value) throws OperationFailedException;
}
