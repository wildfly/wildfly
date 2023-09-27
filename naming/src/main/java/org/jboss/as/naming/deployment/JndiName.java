/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.deployment;

import java.io.Serializable;

import org.jboss.as.naming.logging.NamingLogger;

/**
 * Utility object used to easily manged the construction and management of JNDI names.
 *
 * @author John E. Bailey
 */
public class JndiName implements Serializable, Comparable<JndiName> {
    private static final long serialVersionUID = 3748117883355718029L;
    private static final String ENTRY_SEPARATOR = "/";

    private final JndiName parent;
    private final String local;

    private JndiName(final JndiName parent, final String local) {
        this.parent = parent;
        this.local = local;
    }

    /**
     * Get the local JNDI entry name.  Eg.  java:comp/enc => enc
     *
     * @return The local JNDI entry name
     */
    public String getLocalName() {
        return local;
    }

    /**
     * Get the parent JNDI name.  Eg.  java:comp/enc => java:comp
     *
     * @return The parent JNDI name
     */
    public JndiName getParent() {
        return parent;
    }

    /**
     * Get the absolute JNDI name as a string.
     *
     * @return The absolute JNDI name as a string
     */
    public String getAbsoluteName() {
        final StringBuilder absolute = new StringBuilder();
        if (parent != null) {
            absolute.append(parent).append(ENTRY_SEPARATOR);
        }
        absolute.append(local);
        return absolute.toString();
    }

    /**
     * Create a new JNDI name by appending a new local entry name to this name.
     *
     * @param local The new local part to append
     * @return A new JNDI name
     */
    public JndiName append(final String local) {
        return new JndiName(this, local);
    }

    /**
     * Create a new instance of the JndiName by breaking the provided string format into a JndiName parts.
     *
     * @param name The string representation of a JNDI name.
     * @return The JndiName representation
     */
    public static JndiName of(final String name) {
        if(name == null || name.isEmpty()) throw NamingLogger.ROOT_LOGGER.invalidJndiName(name);
        final String[] parts = name.split(ENTRY_SEPARATOR);
        JndiName current = null;
        for(String part : parts) {
            current = new JndiName(current, part);
        }
        return current;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final JndiName jndiName = (JndiName) o;
        return !(local != null ? !local.equals(jndiName.local) : jndiName.local != null) && !(parent != null ? !parent.equals(jndiName.parent) : jndiName.parent != null);
    }

    @Override
    public int hashCode() {
        int result = parent != null ? parent.hashCode() : 0;
        result = 31 * result + (local != null ? local.hashCode() : 0);
        return result;
    }


    public String toString() {
        return getAbsoluteName();
    }

    @Override
    public int compareTo(final JndiName other) {
        return getAbsoluteName().compareTo(other.getAbsoluteName());
    }
}
