/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.session;

/**
 * Exposes accesses to the attributes of a session.
 * @author Paul Ferraro
 */
public interface SessionAttributes extends ImmutableSessionAttributes {
    /**
     * Removes the specified attribute.
     * @param name a unique attribute name
     * @return the removed attribute value, or null if the attribute does not exist.
     */
    Object removeAttribute(String name);

    /**
     * Sets the specified attribute to the specified value.
     * @param name a unique attribute name
     * @param value the attribute value
     * @return the old attribute value, or null if the attribute did not previously exist.
     */
    Object setAttribute(String name, Object value);
}
