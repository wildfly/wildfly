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

package org.jboss.as.logging.resolvers;

import static org.jboss.as.logging.LoggingMessages.MESSAGES;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.logging.CommonAttributes;
import org.jboss.as.logging.Logging;
import org.jboss.dmr.ModelNode;

/**
 * Date: 15.12.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class FilterResolver implements ModelNodeResolver<String> {

    public static final FilterResolver INSTANCE = new FilterResolver();

    private FilterResolver() {
    }

    @Override
    public String resolveValue(final OperationContext context, final ModelNode value) throws OperationFailedException {
        if (!value.isDefined()) {
            return null;
        }
        if (value.hasDefined(CommonAttributes.ACCEPT.getName())) {
            return "accept";
        } else if (value.hasDefined(CommonAttributes.ALL.getName())) {
            final StringBuilder result = new StringBuilder("all(");
            boolean add = false;
            for (ModelNode filterValue : value.get(CommonAttributes.ALL.getName()).asList()) {
                if (add) {
                    result.append(",");
                } else {
                    add = true;
                }
                result.append(resolveValue(context, filterValue));
            }
            return result.append(")").toString();
        } else if (value.hasDefined(CommonAttributes.ANY.getName())) {
            final StringBuilder result = new StringBuilder("any(");
            boolean add = false;
            for (ModelNode filterValue : value.get(CommonAttributes.ANY.getName()).asList()) {
                if (add) {
                    result.append(",");
                } else {
                    add = true;
                }
                result.append(resolveValue(context, filterValue));
            }
            return result.append(")").toString();
        } else if (value.hasDefined(CommonAttributes.CHANGE_LEVEL.getName())) {
            return String.format("levelChange(%s)", LevelResolver.INSTANCE.resolveValue(context, CommonAttributes.CHANGE_LEVEL.resolveModelAttribute(context, value)));
        } else if (value.hasDefined(CommonAttributes.DENY.getName())) {
            return "deny";
        } else if (value.hasDefined(CommonAttributes.LEVEL.getName())) {
            return String.format("levels(%s)", LevelResolver.INSTANCE.resolveValue(context, CommonAttributes.LEVEL.resolveModelAttribute(context, value)));
        } else if (value.hasDefined(CommonAttributes.LEVEL_RANGE.getName())) {
            final ModelNode levelRange = CommonAttributes.LEVEL_RANGE.resolveModelAttribute(context, value);
            final StringBuilder result = new StringBuilder("levelRange");
            final boolean minInclusive = CommonAttributes.MIN_INCLUSIVE.resolveModelAttribute(context, levelRange).asBoolean();
            final boolean maxInclusive = CommonAttributes.MAX_INCLUSIVE.resolveModelAttribute(context, levelRange).asBoolean();
            if (minInclusive) {
                result.append("[");
            } else {
                result.append("(");
            }
            result.append(LevelResolver.INSTANCE.resolveValue(context, CommonAttributes.MIN_LEVEL.resolveModelAttribute(context, levelRange))).append(",");
            result.append(LevelResolver.INSTANCE.resolveValue(context, CommonAttributes.MAX_LEVEL.resolveModelAttribute(context, levelRange)));
            if (maxInclusive) {
                result.append("]");
            } else {
                result.append(")");
            }
            return result.toString();
        } else if (value.hasDefined(CommonAttributes.MATCH.getName())) {
            return String.format("match(%s)", escapeString(context, CommonAttributes.MATCH, value));
        } else if (value.hasDefined(CommonAttributes.NOT.getName())) {
            return String.format("not(%s)", resolveValue(context, CommonAttributes.NOT.resolveModelAttribute(context, value)));
        } else if (value.hasDefined(CommonAttributes.REPLACE.getName())) {
            final ModelNode replace = CommonAttributes.REPLACE.resolveModelAttribute(context, value);
            final boolean replaceAll = CommonAttributes.REPLACE_ALL.resolveModelAttribute(context, replace).asBoolean();
            final StringBuilder result = new StringBuilder("substitute");
            if (replaceAll) {
                result.append("All");
            }
            return result.append("(").
                    append(escapeString(context, CommonAttributes.PATTERN, replace)).
                    append(",").append(escapeString(context, CommonAttributes.REPLACEMENT, replace)).
                    append(")").toString();
        }
        final String name = value.hasDefined(CommonAttributes.FILTER.getName()) ? value.get(CommonAttributes.FILTER.getName()).asString() : value.asString();
        throw Logging.createOperationFailure(MESSAGES.invalidFilter(name));
    }

    private static String escapeString(final OperationContext context, final AttributeDefinition attribute, final ModelNode value) throws OperationFailedException {
        return "\"" + attribute.resolveModelAttribute(context, value).asString() + "\"";
    }
}
