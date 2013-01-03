/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.logging.Logging.createOperationFailure;
import static org.jboss.as.logging.LoggingMessages.MESSAGES;

import java.text.SimpleDateFormat;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class SuffixValidator extends ModelTypeValidator {

    public SuffixValidator() {
        this(false);
    }

    public SuffixValidator(final boolean nullable) {
        super(ModelType.STRING, nullable);
    }

    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined()) {
            final String suffix = value.asString();
            try {
                new SimpleDateFormat(suffix);
                if (suffix.contains("s") || suffix.contains("S")) {
                    throw createOperationFailure(MESSAGES.invalidSuffix(suffix));
                }
            } catch (IllegalArgumentException e) {
                throw createOperationFailure(MESSAGES.invalidSuffix(suffix));
            }
        }
    }
}
