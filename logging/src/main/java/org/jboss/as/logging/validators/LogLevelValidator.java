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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.logging.util.ModelParser;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Checks the value to see if it's a valid {@link Level}.
 * <p/>
 * Date: 13.07.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public final class LogLevelValidator extends ModelTypeValidator implements AllowedValuesValidator {
    private static final Level[] LEVELS = {
            org.jboss.logmanager.Level.ALL,
            org.jboss.logmanager.Level.CONFIG,
            org.jboss.logmanager.Level.DEBUG,
            org.jboss.logmanager.Level.ERROR,
            org.jboss.logmanager.Level.FATAL,
            org.jboss.logmanager.Level.FINE,
            org.jboss.logmanager.Level.FINER,
            org.jboss.logmanager.Level.FINEST,
            org.jboss.logmanager.Level.INFO,
            org.jboss.logmanager.Level.OFF,
            org.jboss.logmanager.Level.TRACE,
            org.jboss.logmanager.Level.WARN,
            org.jboss.logmanager.Level.WARNING
    };

    private final List<Level> allowedValues;
    private final List<ModelNode> nodeValues;

    public LogLevelValidator(final boolean nullable) {
        this(nullable, false);
    }

    public LogLevelValidator(final boolean nullable, final Level... levels) {
        this(nullable, false, levels);
    }

    public LogLevelValidator(final boolean nullable, final boolean allowExpressions) {
        this(nullable, allowExpressions, LEVELS);
    }

    public LogLevelValidator(final boolean nullable, final boolean allowExpressions, final Level... levels) {
        super(ModelType.STRING, nullable, allowExpressions);
        allowedValues = Arrays.asList(levels);
        Collections.sort(allowedValues, LevelComparator.INSTANCE);
        nodeValues = new ArrayList<ModelNode>(allowedValues.size());
        for (Level level : allowedValues) {
            nodeValues.add(new ModelNode().set(level.getName()));
        }
    }

    @Override
    public void validateParameter(final String parameterName, final ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined()) {
            final String levelString = value.asString();
            try {
                final Level level = ModelParser.parseLevel(value);
                if (!allowedValues.contains(level)) {
                    throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidLogLevel(levelString)));
                }
            } catch (IllegalArgumentException e) {
                throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidLogLevel(levelString)));
            }
        }
    }

    @Override
    public List<ModelNode> getAllowedValues() {
        return nodeValues;
    }

    private static class LevelComparator implements Comparator<Level> {

        static final int EQUAL = 0;
        static final int LESS = -1;
        static final int GREATER = 1;

        static final LevelComparator INSTANCE = new LevelComparator();

        @Override
        public int compare(final Level o1, final Level o2) {
            int result = EQUAL;
            final int left = o1.intValue();
            final int right = o2.intValue();
            if (left < right) {
                result = LESS;
            } else if (left > right) {
                result = GREATER;
            }
            return result;
        }
    }
}
