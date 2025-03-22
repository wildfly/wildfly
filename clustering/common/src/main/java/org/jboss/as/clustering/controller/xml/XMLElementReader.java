/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.xml;

/**
 * Adds absentee handling to an {link org.jboss.staxmapper.XMLElementReader}.
 */
public interface XMLElementReader<C> extends org.jboss.staxmapper.XMLElementReader<C> {
    /**
     * Handles the case where the associated element is not present in the XML input.
     * By default, no action is taken.
     * @param context a reader context
     */
    default void handleAbsentElement(C context) {
        // Do nothing
    }
}
