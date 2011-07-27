/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;

import java.util.logging.Level;

import static org.jboss.dmr.ModelType.EXPRESSION;

/**
 * Checks the value to see if it's a valid {@link Level}.
 *
 * Date: 13.07.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
final class LogLevelValidator implements ParameterValidator {
    /**
     * Default class constructor
     */
    LogLevelValidator() {
    }

    @Override
    public void validateParameter(final String parameterName, final ModelNode value) throws OperationFailedException {
        if (value.isDefined() && value.getType() != EXPRESSION) {
            final String level = value.asString();
            try {
                Level.parse(level);
            } catch (IllegalArgumentException e) {
                throw new OperationFailedException(new ModelNode().set(String.format("Log level %s is invalid.", level)));
            }
        }
    }

    @Override
    public void validateResolvedParameter(final String parameterName, final ModelNode value) throws OperationFailedException {
        validateParameter(parameterName, value.resolve());
    }
}
