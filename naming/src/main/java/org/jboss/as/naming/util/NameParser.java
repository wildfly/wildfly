/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming.util;

import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NamingException;

/**
 * Name parser used by the NamingContext instances.  Relies on composite name instances.
 *
 * @author John E. Bailey
 */
public class NameParser implements javax.naming.NameParser {

    public static final NameParser INSTANCE = new NameParser();

    private NameParser() {
    }

    /**
     * Parse the string name into a {@code javax.naming.Name} instance.
     *
     * @param name The name to parse
     * @return The parsed name.
     * @throws NamingException
     */
    public Name parse(String name) throws NamingException {
        return new CompositeName(name);
    }
}
