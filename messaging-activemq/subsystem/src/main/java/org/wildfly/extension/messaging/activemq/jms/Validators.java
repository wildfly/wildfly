/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ListValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class Validators {

    public static ListValidator noDuplicateElements(ParameterValidator elementValidator) {
        return new ListValidator(elementValidator, false, 1, Integer.MAX_VALUE) {
            @Override
            public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
                super.validateParameter(parameterName, value);

                int elementsSize = value.asList().size();

                // use a set to check whether the list contains duplicate elements
                Set<ModelNode> set = new HashSet<>(value.asList());

                if (set.size() != elementsSize) {
                    throw MessagingLogger.ROOT_LOGGER.duplicateElements(parameterName, value);
                }
            }
        };
    }
}
