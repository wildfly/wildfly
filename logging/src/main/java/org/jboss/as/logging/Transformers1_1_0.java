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

package org.jboss.as.logging;

import java.util.regex.Pattern;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Utility classes related to transforming the logging subsystem to the 1.1.0 model version.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
final class Transformers1_1_0 {

    /** For use with the {@link CommonAttributes#ENABLED} attribute, discards if value is undefined or 'true' */
    static final DiscardAttributeChecker DISCARD_ENABLED = new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(true));

    /** {@link AttributeConverter} that strips the console color portion of a logging format. */
    static final AttributeConverter CONSOLE_COLOR_CONVERTER = new ConsoleColorFormatPatternConverter();

    /**
     * {@link AttributeConverter} that converts a filter-spec expression to a legacy "filter" complex attribute value.
     * <p>
     * <strong>Must be combined with an attribute rename to convert the name to 'filter'</strong>
     * </p>
     */
    static final AttributeConverter FILTER_SPEC_CONVERTER = new FilterSpecAttributeConverter();

    /** {@link DiscardAttributeChecker} that discards a value of "all". */
    static final DiscardAttributeChecker LEVEL_ALL_DISCARD_CHECKER = new LevelAllDiscardAttributeChecker();

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile(".*\\$\\{.*\\}.*");

    private Transformers1_1_0() {
        // prevent external instantiation
    }

    private static boolean isExpression(ModelNode attributeValue) {
        return attributeValue.getType() == ModelType.EXPRESSION
                || (attributeValue.getType() == ModelType.STRING && EXPRESSION_PATTERN.matcher(attributeValue.asString()).matches());
    }

    private static class ConsoleColorFormatPatternConverter extends AttributeConverter.DefaultAttributeConverter {

        private static String fixFormatPattern(final String currentPattern) {
            return currentPattern.replaceAll("(%K\\{[a-zA-Z]*?})", "");
        }

        @Override
        protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            if (attributeValue.getType() == ModelType.STRING) {
                String currentPattern = attributeValue.asString();
                attributeValue.set(fixFormatPattern(currentPattern));
            } else if (attributeValue.getType() == ModelType.EXPRESSION) {
                String currentPattern = attributeValue.asString();
                attributeValue.setExpression(fixFormatPattern(currentPattern));
            }
        }
    }

    private static class FilterSpecAttributeConverter extends AttributeConverter.DefaultAttributeConverter {

        @Override
        protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            if (attributeValue.isDefined() && !isExpression(attributeValue)) {
                final String filterExpression = attributeValue.asString();
                attributeValue.set(Filters.filterSpecToFilter(filterExpression));
            }
        }
    }

    /**
     * {@link DiscardAttributeChecker} that discards a value of "all".
     *
     * @author Brian Stansberry (c) 2012 Red Hat Inc.
     */
    private static class LevelAllDiscardAttributeChecker extends DiscardAttributeChecker.DefaultDiscardAttributeChecker {

        private LevelAllDiscardAttributeChecker() {
            super(false, false);
        }

        @Override
        protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            return attributeValue.isDefined() && attributeValue.asString().equalsIgnoreCase("ALL");
        }
    }
}
