/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component;

import org.jboss.as.ee.logging.EeLogger;

/**
 * A binding into JNDI.  This class contains the mechanism to construct the binding service.  In particular
 * it represents <b>only</b> the description of the binding; it does not represent injection or any other parameters
 * of a JNDI resource.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BindingConfiguration {
    private final String name;
    private final InjectionSource source;

    /**
     * Construct a new instance.
     *
     * @param name   the binding name
     * @param source The source which will be used to resolve a value to be bound in the JNDI
     * @throws IllegalArgumentException If either of the passed <code>name</code> or <code>source</code> is null
     */
    public BindingConfiguration(final String name, final InjectionSource source) {
        if (name == null) {
            throw EeLogger.ROOT_LOGGER.nullName("binding");
        }
        if (source == null) {
            throw EeLogger.ROOT_LOGGER.nullVar("source","binding", "");
        }
        this.name = name;
        this.source = source;
    }

    /**
     * The name into which this binding should be made.  The meaning of relative names depends on where this
     * binding description is used.  For component bindings, relative names are generally relative to {@code java:comp/env}.
     *
     * @return the name into which this binding should be made
     */
    public String getName() {
        return name;
    }

    /**
     * Get the source for this binding.
     *
     * @return the binding's injection source
     */
    public InjectionSource getSource() {
        return source;
    }


    public boolean equals(Object other) {
        if (!(other instanceof BindingConfiguration))
            return false;

        BindingConfiguration config = (BindingConfiguration)other;
        return name.equals(config.name) && source.equals(config.source);
    }

    public int hashCode() {
        return name.hashCode() * 31 + source.hashCode();
    }

}
