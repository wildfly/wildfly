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

import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isJavaIdentifierStart;
import static java.lang.Character.isWhitespace;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Filter utilties and constants.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class Filters {

    public static final String ACCEPT = "accept";
    public static final String ALL = "all";
    public static final String ANY = "any";
    public static final String DENY = "deny";
    public static final String LEVELS = "levels";
    public static final String LEVEL_CHANGE = "levelChange";
    public static final String LEVEL_RANGE = "levelRange";
    public static final String MATCH = "match";
    public static final String NOT = "not";
    public static final String SUBSTITUTE = "substitute";
    public static final String SUBSTITUTE_ALL = "substituteAll";

    /**
     * Converts the legacy {@link CommonAttributes#FILTER filter} to the new {@link CommonAttributes#FILTER_SPEC filter
     * spec}.
     *
     * @param value the value to convert
     *
     * @return the filter expression (filter spec)
     *
     * @throws org.jboss.as.controller.OperationFailedException
     *          if a conversion error occurs
     */
    static String filterToFilterSpec(final ModelNode value) throws OperationFailedException {
        if (!value.isDefined()) {
            return null;
        }
        if (value.hasDefined(CommonAttributes.ACCEPT.getName())) {
            return ACCEPT;
        } else if (value.hasDefined(CommonAttributes.ALL.getName())) {
            final StringBuilder result = new StringBuilder(ALL).append('(');
            boolean add = false;
            for (ModelNode filterValue : value.get(CommonAttributes.ALL.getName()).asList()) {
                if (add) {
                    result.append(",");
                } else {
                    add = true;
                }
                result.append(filterToFilterSpec(filterValue));
            }
            return result.append(")").toString();
        } else if (value.hasDefined(CommonAttributes.ANY.getName())) {
            final StringBuilder result = new StringBuilder(ANY).append('(');
            boolean add = false;
            for (ModelNode filterValue : value.get(CommonAttributes.ANY.getName()).asList()) {
                if (add) {
                    result.append(",");
                } else {
                    add = true;
                }
                result.append(filterToFilterSpec(filterValue));
            }
            return result.append(")").toString();
        } else if (value.hasDefined(CommonAttributes.CHANGE_LEVEL.getName())) {
            return String.format("%s(%s)", LEVEL_CHANGE, value.get(CommonAttributes.CHANGE_LEVEL.getName()).asString());
        } else if (value.hasDefined(CommonAttributes.DENY.getName())) {
            return DENY;
        } else if (value.hasDefined(CommonAttributes.LEVEL.getName())) {
            return String.format("%s(%s)", LEVELS, value.get(CommonAttributes.LEVEL.getName()).asString());
        } else if (value.hasDefined(CommonAttributes.LEVEL_RANGE_LEGACY.getName())) {
            final ModelNode levelRange = value.get(CommonAttributes.LEVEL_RANGE_LEGACY.getName());
            final StringBuilder result = new StringBuilder(LEVEL_RANGE);
            final boolean minInclusive = (levelRange.hasDefined(CommonAttributes.MIN_INCLUSIVE.getName()) && levelRange.get(CommonAttributes.MIN_INCLUSIVE.getName()).asBoolean());
            final boolean maxInclusive = (levelRange.hasDefined(CommonAttributes.MAX_INCLUSIVE.getName()) && levelRange.get(CommonAttributes.MAX_INCLUSIVE.getName()).asBoolean());
            if (minInclusive) {
                result.append("[");
            } else {
                result.append("(");
            }
            result.append(levelRange.get(CommonAttributes.MIN_LEVEL.getName()).asString()).append(",");
            result.append(levelRange.get(CommonAttributes.MAX_LEVEL.getName()).asString());
            if (maxInclusive) {
                result.append("]");
            } else {
                result.append(")");
            }
            return result.toString();
        } else if (value.hasDefined(CommonAttributes.MATCH.getName())) {
            return String.format("%s(%s)", MATCH, escapeString(CommonAttributes.MATCH, value));
        } else if (value.hasDefined(CommonAttributes.NOT.getName())) {
            return String.format("%s(%s)", NOT, filterToFilterSpec(value.get(CommonAttributes.NOT.getName())));
        } else if (value.hasDefined(CommonAttributes.REPLACE.getName())) {
            final ModelNode replace = value.get(CommonAttributes.REPLACE.getName());
            final boolean replaceAll = replace.get(CommonAttributes.REPLACE_ALL.getName()).asBoolean();
            final StringBuilder result = new StringBuilder();
            if (replaceAll) {
                result.append(SUBSTITUTE_ALL);
            } else {
                result.append(SUBSTITUTE);
            }
            return result.append("(")
                    .append(escapeString(CommonAttributes.PATTERN, replace))
                    .append(",").append(escapeString(CommonAttributes.REPLACEMENT, replace))
                    .append(")").toString();
        }
        final String name = value.hasDefined(CommonAttributes.FILTER.getName()) ? value.get(CommonAttributes.FILTER.getName()).asString() : value.asString();
        throw Logging.createOperationFailure(LoggingMessages.MESSAGES.invalidFilter(name));
    }

    /**
     * Converts a {@link CommonAttributes#FILTER_SPEC filter spec} to a legacy {@link CommonAttributes#FILTER filter}.
     *
     * @param value the value to convert
     *
     * @return the complex filter object
     */
    static ModelNode filterSpecToFilter(final String value) {
        final ModelNode filter = new ModelNode(CommonAttributes.FILTER.getName()).setEmptyObject();
        final Iterator<String> iterator = tokens(value).iterator();
        parseFilterExpression(iterator, filter, true);
        return filter;
    }

    private static void parseFilterExpression(final Iterator<String> iterator, final ModelNode model, final boolean outermost) {
        if (!iterator.hasNext()) {
            if (outermost) {
                model.setEmptyObject();
                return;
            }
            throw LoggingMessages.MESSAGES.unexpectedEnd();
        }
        final String token = iterator.next();
        if (ACCEPT.equals(token)) {
            set(CommonAttributes.ACCEPT, model, true);
        } else if (DENY.equals(token)) {
            set(CommonAttributes.DENY, model, true);
        } else if (NOT.equals(token)) {
            expect("(", iterator);
            parseFilterExpression(iterator, model.get(CommonAttributes.NOT.getName()), false);
            expect(")", iterator);
        } else if (ALL.equals(token)) {
            expect("(", iterator);
            do {
                final ModelNode m = model.get(CommonAttributes.ALL.getName());
                parseFilterExpression(iterator, m, false);
            } while (expect(",", ")", iterator));
        } else if (ANY.equals(token)) {
            expect("(", iterator);
            do {
                final ModelNode m = model.get(CommonAttributes.ANY.getName());
                parseFilterExpression(iterator, m, false);
            } while (expect(",", ")", iterator));
        } else if (LEVEL_CHANGE.equals(token)) {
            expect("(", iterator);
            final String levelName = expectName(iterator);
            set(CommonAttributes.CHANGE_LEVEL, model, levelName);
            expect(")", iterator);
        } else if (LEVELS.equals(token)) {
            expect("(", iterator);
            final Set<String> levels = new HashSet<String>();
            do {
                levels.add(expectName(iterator));
            } while (expect(",", ")", iterator));
            // The model only allows for one level,just use the first
            if (levels.iterator().hasNext()) set(CommonAttributes.LEVEL, model, levels.iterator().next());
        } else if (LEVEL_RANGE.equals(token)) {
            final ModelNode levelRange = model.get(CommonAttributes.LEVEL_RANGE_LEGACY.getName());
            final boolean minInclusive = expect("[", "(", iterator);
            if (minInclusive) set(CommonAttributes.MIN_INCLUSIVE, levelRange, minInclusive);
            final String minLevel = expectName(iterator);
            set(CommonAttributes.MIN_LEVEL, levelRange, minLevel);
            expect(",", iterator);
            final String maxLevel = expectName(iterator);
            set(CommonAttributes.MAX_LEVEL, levelRange, maxLevel);
            final boolean maxInclusive = expect("]", ")", iterator);
            if (maxInclusive) set(CommonAttributes.MAX_INCLUSIVE, levelRange, maxInclusive);
        } else if (MATCH.equals(token)) {
            expect("(", iterator);
            final String pattern = expectString(iterator);
            set(CommonAttributes.MATCH, model, pattern);
            expect(")", iterator);
        } else if (SUBSTITUTE.equals(token)) {
            final ModelNode substitute = model.get(CommonAttributes.REPLACE.getName());
            substitute.get(CommonAttributes.REPLACE_ALL.getName()).set(false);
            expect("(", iterator);
            final String pattern = expectString(iterator);
            set(CommonAttributes.PATTERN, substitute, pattern);
            expect(",", iterator);
            final String replacement = expectString(iterator);
            set(CommonAttributes.REPLACEMENT, substitute, replacement);
            expect(")", iterator);
        } else if (SUBSTITUTE_ALL.equals(token)) {
            final ModelNode substitute = model.get(CommonAttributes.REPLACE.getName());
            substitute.get(CommonAttributes.REPLACE_ALL.getName()).set(true);
            expect("(", iterator);
            final String pattern = expectString(iterator);
            set(CommonAttributes.PATTERN, substitute, pattern);
            expect(",", iterator);
            final String replacement = expectString(iterator);
            set(CommonAttributes.REPLACEMENT, substitute, replacement);
            expect(")", iterator);
        } else {
            final String name = expectName(iterator);
            throw LoggingMessages.MESSAGES.filterNotFound(name);
        }
    }

    private static String expectName(Iterator<String> iterator) {
        if (iterator.hasNext()) {
            final String next = iterator.next();
            if (isJavaIdentifierStart(next.codePointAt(0))) {
                return next;
            }
        }
        throw LoggingMessages.MESSAGES.expectedIdentifier();
    }

    private static String expectString(final Iterator<String> iterator) {
        if (iterator.hasNext()) {
            final String next = iterator.next();
            if (next.codePointAt(0) == '"') {
                return next.substring(1);
            }
        }
        throw LoggingMessages.MESSAGES.expectedString();
    }

    private static boolean expect(final String trueToken, final String falseToken, final Iterator<String> iterator) {
        final boolean hasNext = iterator.hasNext();
        final String next = hasNext ? iterator.next() : null;
        final boolean result;
        if (!hasNext || !((result = trueToken.equals(next)) || falseToken.equals(next))) {
            throw LoggingMessages.MESSAGES.expected(trueToken, falseToken);
        }
        return result;
    }

    private static void expect(String token, Iterator<String> iterator) {
        if (!iterator.hasNext() || !token.equals(iterator.next())) {
            throw LoggingMessages.MESSAGES.expected(token);
        }
    }

    private static void set(final AttributeDefinition attribute, final ModelNode model, final String value) {
        set(attribute.getName(), model, value);
    }

    private static void set(final AttributeDefinition attribute, final ModelNode model, final boolean value) {
        set(attribute.getName(), model, value);
    }

    private static void set(final String name, final ModelNode model, final String value) {
        model.get(name).set(value);
    }

    private static void set(final String name, final ModelNode model, final boolean value) {
        model.get(name).set(value);
    }


    private static List<String> tokens(final String source) {
        final List<String> tokens = new ArrayList<String>();
        final int length = source.length();
        int idx = 0;
        while (idx < length) {
            int ch;
            ch = source.codePointAt(idx);
            if (isWhitespace(ch)) {
                ch = source.codePointAt(idx);
                idx = source.offsetByCodePoints(idx, 1);
            } else if (isJavaIdentifierStart(ch)) {
                int start = idx;
                do {
                    idx = source.offsetByCodePoints(idx, 1);
                } while (idx < length && isJavaIdentifierPart(ch = source.codePointAt(idx)));
                tokens.add(source.substring(start, idx));
            } else if (ch == '"') {
                final StringBuilder b = new StringBuilder();
                // tag token as a string
                b.append('"');
                idx = source.offsetByCodePoints(idx, 1);
                while (idx < length && (ch = source.codePointAt(idx)) != '"') {
                    ch = source.codePointAt(idx);
                    if (ch == '\\') {
                        idx = source.offsetByCodePoints(idx, 1);
                        if (idx == length) {
                            throw LoggingMessages.MESSAGES.truncatedFilterExpression();
                        }
                        ch = source.codePointAt(idx);
                        switch (ch) {
                            case '\\':
                                b.append('\\');
                                break;
                            case '\'':
                                b.append('\'');
                                break;
                            case '"':
                                b.append('"');
                                break;
                            case 'b':
                                b.append('\b');
                                break;
                            case 'f':
                                b.append('\f');
                                break;
                            case 'n':
                                b.append('\n');
                                break;
                            case 'r':
                                b.append('\r');
                                break;
                            case 't':
                                b.append('\t');
                                break;
                            default:
                                throw LoggingMessages.MESSAGES.invalidEscapeFoundInFilterExpression();
                        }
                    } else {
                        b.appendCodePoint(ch);
                    }
                    idx = source.offsetByCodePoints(idx, 1);
                }
                idx = source.offsetByCodePoints(idx, 1);
                tokens.add(b.toString());
            } else {
                int start = idx;
                idx = source.offsetByCodePoints(idx, 1);
                tokens.add(source.substring(start, idx));
            }
        }
        return tokens;
    }

    private static String escapeString(final AttributeDefinition attribute, final ModelNode value) throws OperationFailedException {
        return String.format("\"%s\"", value.get(attribute.getName()).asString().replace("\\", "\\\\"));
    }
}
