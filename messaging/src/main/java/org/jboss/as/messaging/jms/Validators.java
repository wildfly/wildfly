/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.jms;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ListValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;

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
