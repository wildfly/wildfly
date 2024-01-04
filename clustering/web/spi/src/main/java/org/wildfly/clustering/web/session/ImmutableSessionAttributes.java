/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.session;

import java.util.Set;

/**
 * Provides read-only access to a session's attributes.
 * @author Paul Ferraro
 */
public interface ImmutableSessionAttributes {
    /**
     * Returns the names of the attributes of this session.
     * @return a set of unique attribute names
     */
    Set<String> getAttributeNames();

    /**
     * Retrieves the value of the specified attribute.
     * @param name a unique attribute name
     * @return the attribute value, or null if the attribute does not exist.
     */
    Object getAttribute(String name);
}
