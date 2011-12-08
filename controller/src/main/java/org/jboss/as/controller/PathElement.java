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

package org.jboss.as.controller;

import java.util.regex.Pattern;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import static org.jboss.as.controller.ControllerMessages.MESSAGES;

/**
 * An element of a path specification for matching operations with addresses.
 * @author Brian Stansberry
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class PathElement {

    public static final String WILDCARD_VALUE = "*";

    private final String key;
    private final String value;
    private final boolean multiTarget;
    private final int hashCode;

    /**
     * A valid key contains alphanumerics and underscores, cannot start with a
     * number, and cannot start or end with {@code -}.
     */
    private static final Pattern VALID_KEY_PATTERN = Pattern.compile("\\*|[_a-zA-Z](?:[-_a-zA-Z0-9]*[_a-zA-Z0-9])?");

    private static final Pattern VALID_VALUE_PATTERN = Pattern.compile("\\*|[^*\\p{Space}\\p{Cntrl}]+");

    /**
     * Construct a new instance with a wildcard value.
     * @param key the path key to match
     * @return the new path element
     */
    public static PathElement pathElement(final String key) {
        return new PathElement(key);
    }

    /**
     * Construct a new instance.
     * @param key the path key to match
     * @param value the path value or wildcard to match
     * @return the new path element
     */
    public static PathElement pathElement(final String key, final String value) {
        return new PathElement(key, value);
    }

    /**
     * Construct a new instance with a wildcard value.
     * @param key the path key to match
     */
    PathElement(final String key) {
        this(key, WILDCARD_VALUE);
    }

    /**
     * Construct a new instance.
     * @param key the path key to match
     * @param value the path value or wildcard to match
     */
    PathElement(final String key, final String value) {
        if (key == null || !VALID_KEY_PATTERN.matcher(key).matches()) {
            throw new OperationClientIllegalArgumentException(MESSAGES.invalidPathElementKey(key));
        }
        if (value == null || !VALID_VALUE_PATTERN.matcher(value).matches()) {
            throw new OperationClientIllegalArgumentException(MESSAGES.invalidPathElementValue(value));
        }
        boolean multiTarget = false;
        if(key.equals(WILDCARD_VALUE)) {
            this.key = WILDCARD_VALUE;
            multiTarget = true;
        } else {
            this.key = key;
        }
        if (value.equals(WILDCARD_VALUE)) {
            this.value = WILDCARD_VALUE;
            multiTarget = true;
        } else if (value.charAt(0) == '[' && value.charAt(value.length() - 1) == ']') {
            this.value = value.substring(1, value.length() - 1);
            multiTarget |= value.indexOf(',') != -1;
        } else {
            this.value = value;
        }
        this.multiTarget = multiTarget;
        hashCode = key.hashCode() * 19 + value.hashCode();
    }

    /**
     * Get the path key.
     * @return the path key
     */
    public String getKey() {
        return key;
    }

    /**
     * Get the path value.
     * @return the path value
     */
    public String getValue() {
        return value;
    }

    /**
     * Determine whether the given property matches this element.
     * @param property the property to check
     * @return {@code true} if the property matches
     */
    public boolean matches(Property property) {
        return property.getName().equals(key) && (value == WILDCARD_VALUE || property.getValue().asString().equals(value));
    }

    /**
     * Determine whether the value is the wildcard value.
     * @return {@code true} if the value is the wildcard value
     */
    public boolean isWildcard() {
        return WILDCARD_VALUE == value;
    }

    public boolean isMultiTarget() {
        return multiTarget;
    }

    public String[] getSegments() {
        return value.split(",");
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * Determine whether this object is equal to another.
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(Object other) {
        return other instanceof PathElement && equals((PathElement) other);
    }

    /**
     * Determine whether this object is equal to another.
     * @param other the other object
     * @return {@code true} if they are equal, {@code false} otherwise
     */
    public boolean equals(PathElement other) {
        return this == other || other != null && other.key.equals(key) && other.value.equals(value);
    }

    @Override
    public String toString() {
        return "\"" + key + "\" => \"" + value + "\"";
    }

    /**
     * AS7-2905. An IAE that implements OperationClientException. Allows PathElement to continue to throw IAE
     * in case client code expects that failure type, but lets operation handling code detect that the
     * IAE is a client error.
     */
    private static class OperationClientIllegalArgumentException extends IllegalArgumentException implements OperationClientException {

        private OperationClientIllegalArgumentException(final String msg) {
            super(msg);
        }

        @Override
        public ModelNode getFailureDescription() {
            return new ModelNode(getLocalizedMessage());
        }
    }
}
