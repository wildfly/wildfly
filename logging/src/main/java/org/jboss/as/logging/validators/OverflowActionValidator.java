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

package org.jboss.as.logging.validators;

import static org.jboss.as.logging.LoggingMessages.MESSAGES;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.logging.util.ModelParser;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logmanager.handlers.AsyncHandler.OverflowAction;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Date: 21.09.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public final class OverflowActionValidator extends ModelTypeValidator implements AllowedValuesValidator {
    private final EnumSet<OverflowAction> allowedValues;

    public OverflowActionValidator(final boolean nullable) {
        this(nullable, false);
    }

    public OverflowActionValidator(final boolean nullable, final boolean allowExpressions) {
        super(ModelType.STRING, nullable, allowExpressions);
        allowedValues = EnumSet.allOf(OverflowAction.class);
    }

    @Override
    public void validateParameter(final String parameterName, final ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined()) {
            final String oaString = value.asString();
            try {
                final OverflowAction overflowAction = ModelParser.parseOverflowAction(value);
                if (overflowAction == null || !allowedValues.contains(overflowAction)) {
                    throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidOverflowAction(oaString)));
                }
            } catch (IllegalArgumentException e) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidOverflowAction(oaString)));
            }
        }
    }

    @Override
    public List<ModelNode> getAllowedValues() {
        List<ModelNode> result = new ArrayList<ModelNode>();
        for (OverflowAction oa : allowedValues) {
            result.add(new ModelNode().set(oa.name()));
        }
        return result;
    }
}
