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

package org.jboss.as.messaging;

import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hornetq.core.server.JournalType;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Validates a given string parameter is a legal {@link org.hornetq.core.server.JournalType}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JournalTypeValidator extends ModelTypeValidator implements AllowedValuesValidator {

    public static final JournalTypeValidator INSTANCE = new JournalTypeValidator();

    private JournalTypeValidator() {
        super(ModelType.STRING, true);
    }

    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.getType() != ModelType.EXPRESSION) {
            String str = value.asString();
            try {
                JournalType.valueOf(str);
            } catch (IllegalArgumentException e) {
                List<JournalType> list = Arrays.asList(JournalType.values());
                throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidParameterValue(str, parameterName, list)));
            }
        }
    }

    @Override
    public List<ModelNode> getAllowedValues() {
        final List<ModelNode> result = new ArrayList<ModelNode>();
        for (JournalType type : JournalType.values()) {
            result.add(new ModelNode().set(type.name()));
        }
        return result;
    }
}
