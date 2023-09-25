/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.naming;

import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * Initial context factory builder which ensures the proper naming context factory.
 *
 * @author John E. Bailey
 * @author Eduardo Martins
 */
public class InitialContextFactoryBuilder implements javax.naming.spi.InitialContextFactoryBuilder {

    private static final javax.naming.spi.InitialContextFactory DEFAULT_FACTORY = new InitialContextFactory();

    /**
     * Retrieves the default JBoss naming context factory.
     *
     * @param environment The environment
     * @return An initial context factory
     * @throws NamingException If an error occurs loading the factory class.
     */
    public javax.naming.spi.InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) throws NamingException {
        // return our default factory, the initial context it creates must be the responsible for handling a
        // custom initial context factory in env, to ensure that URL factories are processed first, as the JDK does
        return DEFAULT_FACTORY;
    }
}
