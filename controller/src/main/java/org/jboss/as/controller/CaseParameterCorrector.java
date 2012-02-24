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

package org.jboss.as.controller;

import java.util.Locale;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Parameter correctors that can be used to change the case of a {@link ModelNode model node} that is of {@link
 * ModelType#STRING type string}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class CaseParameterCorrector {

    /**
     * Converts the string value of the {@code newValue} into uppercase only if the value is not already in uppercase.
     */
    public static ParameterCorrector TO_UPPER = new ParameterCorrector() {
        @Override
        public ModelNode correct(final ModelNode newValue, final ModelNode currentValue) {
            if (newValue.getType() == ModelType.UNDEFINED) {
                return newValue;
            }
            if (newValue.getType() != ModelType.STRING || currentValue.getType() != ModelType.STRING) {
                return newValue;
            }
            final String stringValue = newValue.asString();
            final String uCase = stringValue.toUpperCase(Locale.ENGLISH);
            if (!stringValue.equals(uCase)) {
                newValue.set(uCase);
            }
            return newValue;
        }
    };

    /**
     * Converts the string value of the {@code newValue} into lowercase only if the value is not already in lowercase.
     */
    public static ParameterCorrector TO_LOWER = new ParameterCorrector() {
        @Override
        public ModelNode correct(final ModelNode newValue, final ModelNode currentValue) {
            if (newValue.getType() == ModelType.UNDEFINED) {
                return newValue;
            }
            if (newValue.getType() != ModelType.STRING || currentValue.getType() != ModelType.STRING) {
                return newValue;
            }
            final String stringValue = newValue.asString();
            final String uCase = stringValue.toLowerCase(Locale.ENGLISH);
            if (!stringValue.equals(uCase)) {
                newValue.set(uCase);
            }
            return newValue;
        }
    };
}
