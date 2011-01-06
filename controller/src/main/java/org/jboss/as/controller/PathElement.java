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

/**
 * TODO add class javadoc for PathElement

 * @author Brian Stansberry
 *
 */
public class PathElement {

    private static final String WILDCARD_VALUE = "*";

    private final String key;
    private final String value;
    private final boolean wildcard;

    public PathElement(final String key) {
        this(key, WILDCARD_VALUE);
    }

    public PathElement(final String key, final String value) {
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }
        if (WILDCARD_VALUE.equals(key)) {
            throw new IllegalArgumentException("key cannot be '*'");
        }
        if (value == null || value.length() == 0) {
            throw new IllegalArgumentException("value cannot be null or empty");
        }
        this.key = key;
        this.value = value;
        this.wildcard = WILDCARD_VALUE.equals(value);
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public boolean isWildcard() {
        return WILDCARD_VALUE == value;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof PathElement) {
            final PathElement other = (PathElement) obj;
            return (key.equals(other.key) && (wildcard || other.wildcard || value.equals(other.value)));
        }
        return false;
    }

    @Override
    public String toString() {
        return "\"" + key + "\" => \"" + value + "\"";
    }


}
